package main

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"os/signal"
	"router-manager-service/cmd/app"
	"router-manager-service/config"
	"router-manager-service/internal/conf/logutil"
	"router-manager-service/internal/conf/util"
	"router-manager-service/internal/database"
	"syscall"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
)

func main() {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	cfg := config.LoadConfig()
	logCfg := logutil.LoadConfig()

	log := util.GetLogger(ctx)
	log.Info("запуск..",
		slog.String("profile", logCfg.Profile),
		slog.String("version", "1.0.0"),
	)

	deps, cleanup, err := initializeDependencies(ctx, cfg, log)
	if err != nil {
		log.Error("Не смог создать все зависимости", slog.String("error", err.Error()))
		os.Exit(1)
	}
	defer cleanup()

	application := app.New(ctx, deps)

	if err := application.Start(); err != nil {
		log.Error("ошибка запуска приложения", slog.String("error", err.Error()))
		os.Exit(1)
	}

	log.Info("приложение запущенно")

	waitForShutdownSignal(application, log)
	log.Info("приложение завершено")
}

func initializeDependencies(ctx context.Context, cfg *config.Config, log *slog.Logger) (*app.Dependencies, func(), error) {
	cleanupFuncs := make([]func(), 0)
	cleanup := func() {
		for i := len(cleanupFuncs) - 1; i >= 0; i-- {
			cleanupFuncs[i]()
		}
	}

	tracerProvider, err := app.InitTracer(ctx, cfg.AlloyUrl)
	if err != nil {
		return nil, cleanup, fmt.Errorf("ошибка создания tracer: %w", err)
	}
	cleanupFuncs = append(cleanupFuncs, func() {
		shutdownTracer(tracerProvider, log)
	})

	dbPool, err := initDatabase(ctx, cfg, log)
	if err != nil {
		return nil, cleanup, fmt.Errorf("ошибка создания подключения к БД: %w", err)
	}
	cleanupFuncs = append(cleanupFuncs, func() {
		dbPool.Close()
		log.Info("соединение с БД закрыто")
	})

	if err := runMigrations(cfg, log); err != nil {
		return nil, cleanup, fmt.Errorf("Ошибка миграций: %w", err)
	}

	redisClient := initRedis(cfg, log)
	cleanupFuncs = append(cleanupFuncs, func() {
		if err := redisClient.Close(); err != nil {
			log.Error("не получилось закрыть соединение с Redis", slog.String("error", err.Error()))
		} else {
			log.Info("соединение с Redis закрыто")
		}
	})

	deps := &app.Dependencies{
		DBPool:      dbPool,
		RedisClient: redisClient,
		Config:      cfg,
	}

	return deps, cleanup, nil
}

func initDatabase(ctx context.Context, cfg *config.Config, log *slog.Logger) (*pgxpool.Pool, error) {
	connString := fmt.Sprintf(
		"postgres://%s:%s@%s:%s/%s?sslmode=disable",
		cfg.DBUser, cfg.DBPassword, cfg.DBHost, cfg.DBPort, cfg.DBName,
	)

	dbConfig, err := pgxpool.ParseConfig(connString)
	if err != nil {
		return nil, fmt.Errorf("читаю config: %w", err)
	}

	configurePool(dbConfig, cfg)

	log.Info("подключаюсь к базе данных",
		slog.String("host", cfg.DBHost),
		slog.String("database", cfg.DBName),
	)

	pool, err := pgxpool.NewWithConfig(ctx, dbConfig)
	if err != nil {
		return nil, fmt.Errorf("создаю connection pool для БД: %w", err)
	}

	if err := pool.Ping(ctx); err != nil {
		return nil, fmt.Errorf("не получилось получит ответ на пинг БД: %w", err)
	}

	return pool, nil
}

func configurePool(conf *pgxpool.Config, cfg *config.Config) {
	conf.MaxConns = util.StringToInt(cfg.DbMaxConns)
	conf.MinConns = util.StringToInt(cfg.DbMinConns)
	conf.MaxConnLifetime = cfg.DbMaxConnsLifeTime
	conf.MaxConnIdleTime = cfg.DbMaxConnsIdleTime
	conf.HealthCheckPeriod = cfg.DbHealthCheckPeriod

	conf.ConnConfig.RuntimeParams["statement_timeout"] = cfg.DbStatementTimeout
	conf.ConnConfig.RuntimeParams["idle_in_transaction_session_timeout"] = cfg.DbIdleTransSessTimeout
}

func runMigrations(cfg *config.Config, log *slog.Logger) error {
	connString := fmt.Sprintf(
		"postgres://%s:%s@%s:%s/%s?sslmode=disable",
		cfg.DBUser, cfg.DBPassword, cfg.DBHost, cfg.DBPort, cfg.DBName,
	)

	log.Info("запускаю миграции базы данных")

	migrator, err := database.NewMigrator(connString)
	if err != nil {
		return fmt.Errorf("создаю migrator: %w", err)
	}
	defer migrator.Close()

	if err := migrator.Up(); err != nil {
		return fmt.Errorf("применю миграции: %w", err)
	}

	return nil
}

func initRedis(cfg *config.Config, log *slog.Logger) *redis.Client {
	log.Info("создание соединения Redis",
		slog.String("host", cfg.RedisUrl),
		slog.String("port", cfg.RedisPort),
	)

	client := redis.NewClient(&redis.Options{
		Addr:         fmt.Sprintf("%s:%s", cfg.RedisUrl, cfg.RedisPort),
		Password:     cfg.RedisPassword,
		DB:           0,
		DialTimeout:  5 * time.Second,
		ReadTimeout:  3 * time.Second,
		WriteTimeout: 3 * time.Second,
		PoolSize:     10,
		MinIdleConns: 2,
	})

	if err := client.Ping(context.Background()).Err(); err != nil {
		log.Error("тест соединения Redis провален", slog.String("error", err.Error()))
	} else {
		log.Info("Redis успешно подключен")
	}

	return client
}

func shutdownTracer(tp interface{}, log *slog.Logger) {
	if sh, ok := tp.(interface{ Shutdown(context.Context) error }); ok {
		ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()

		if err := sh.Shutdown(ctx); err != nil {
			log.Error("Не смог завершить tracer", slog.String("error", err.Error()))
		} else {
			log.Info("Tracer успешно завершен")
		}
	}
}

func waitForShutdownSignal(application *app.App, log *slog.Logger) {
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM, syscall.SIGQUIT)

	sig := <-sigChan
	log.Info("Услышал сигнал завершения процесса", slog.String("signal", sig.String()))

	application.Stop()
}
