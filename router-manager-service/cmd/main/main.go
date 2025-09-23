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

	log := util.GetLogger(ctx)
	cfg := config.LoadConfig()
	logCfg := logutil.LoadConfig()
	log.Info("Конфигурация загружена",
		slog.String("profile", logCfg.Profile),
		slog.String("log_level", logCfg.Level),
	)

	// Инициализация трассировки
	tp, err := initTracer(ctx, cfg)
	if err != nil {
		log.Error("Ошибка инициализации трассировки", slog.String("error", err.Error()))
		return
	}
	defer shutdownTracer(tp)

	// Инициализация БД
	pool, err := initDatabase(ctx, cfg)
	if err != nil {
		log.Error("Ошибка инициализации БД", slog.String("error", err.Error()))
		return
	}
	defer pool.Close()

	// Миграции БД
	if err := runMigrations(cfg); err != nil {
		log.Error("Ошибка миграций", slog.String("error", err.Error()))
		return
	}

	// Инициализация Redis
	redisClient := initRedis(cfg)
	defer redisClient.Close()

	// Создание зависимостей приложения
	deps := &app.Dependencies{
		DBPool:      pool,
		RedisClient: redisClient,
		Config:      cfg,
	}

	// Создание и запуск приложения
	application, err := app.New(ctx, deps)
	if err != nil {
		log.Error("Ошибка создания приложения", slog.String("error", err.Error()))
		return
	}

	if err := application.Start(); err != nil {
		log.Error("Ошибка запуска приложения", slog.String("error", err.Error()))
		return
	}

	log.Info("Приложение успешно запущено")

	// Ожидание сигналов завершения
	waitForShutdownSignal(application, log)
}

// Вспомогательные функции
func initTracer(ctx context.Context, cfg *config.Config) (interface{}, error) {
	// Ваша существующая логика инициализации трассировки
	return app.InitTracer(ctx, cfg.AlloyUrl)
}

func shutdownTracer(tp interface{}) {
	// Ваша существующая логика остановки трассировки
	if sh, ok := tp.(interface{ Shutdown(context.Context) error }); ok {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		_ = sh.Shutdown(ctx)
	}
}

func initDatabase(ctx context.Context, cfg *config.Config) (*pgxpool.Pool, error) {
	dbPath := fmt.Sprintf(
		"postgres://%s:%s@%s:%s/%s?sslmode=disable",
		cfg.DBUser, cfg.DBPassword, cfg.DBHost, cfg.DBPort, cfg.DBName,
	)

	dbConfig, err := pgxpool.ParseConfig(dbPath)
	if err != nil {
		return nil, fmt.Errorf("parse config: %w", err)
	}

	// Конфигурация пула (ваша существующая логика)
	configurePool(dbConfig, cfg)

	return pgxpool.NewWithConfig(ctx, dbConfig)
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

func runMigrations(cfg *config.Config) error {
	dbPath := fmt.Sprintf(
		"postgres://%s:%s@%s:%s/%s?sslmode=disable",
		cfg.DBUser, cfg.DBPassword, cfg.DBHost, cfg.DBPort, cfg.DBName,
	)

	migrator, err := database.NewMigrator(dbPath)
	if err != nil {
		return err
	}
	defer migrator.Close()

	return migrator.Up()
}

func initRedis(cfg *config.Config) *redis.Client {
	return redis.NewClient(&redis.Options{
		Addr:     fmt.Sprintf("%s:%s", cfg.RedisUrl, cfg.RedisPort),
		Password: cfg.RedisPassword,
		DB:       0,
	})
}

func waitForShutdownSignal(application *app.App, log *slog.Logger) {
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)

	sig := <-sigChan
	log.Info("Получен сигнал завершения", slog.String("signal", sig.String()))

	application.Stop()
	log.Info("Приложение корректно завершено")
}
