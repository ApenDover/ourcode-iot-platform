package main

import (
	"context"
	"errors"
	"fmt"
	"github.com/redis/go-redis/v9"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"router-manager-service/config"
	"router-manager-service/internal/conf"
	"router-manager-service/internal/conf/logutil"
	"router-manager-service/internal/conf/util"
	"syscall"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

func main() {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	log := util.GetLogger(ctx)
	cfg := config.LoadConfig()
	logCfg := logutil.LoadConfig()
	log.Info("Конфигурация загружена", slog.String("profile", logCfg.Profile), slog.String("log_level", logCfg.Level))

	tp, errTraceInit := conf.InitTracer(ctx, cfg.AlloyUrl)
	if errTraceInit != nil {
		log.Error("не удалось инициализировать TracerProvider", slog.String("error", errTraceInit.Error()))
		return
	}
	defer func() {
		shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer shutdownCancel()
		_ = tp.Shutdown(shutdownCtx)
	}()

	dbPath := databasePath(cfg)
	dbConfig, err := pgxpool.ParseConfig(dbPath)
	if err != nil {
		fmt.Errorf("не удалось распарсить конфиг: %w", err)
	}

	pool, pgxErr := createPool(ctx, dbConfig)
	pool.Config()
	if pgxErr != nil {
		log.Error("ошибка подключения к БД", slog.String("error", pgxErr.Error()))
		return
	}
	flyWayPath := util.MigrationsPath(cfg.MigrationPath)
	log.Info("ищу миграции по адресу", slog.String("миграции", flyWayPath))
	conf.RunMigrations(dbPath, flyWayPath)
	defer pool.Close()

	if errPoolPing := pool.Ping(ctx); errPoolPing != nil {
		log.Error("не удалось проверить соединение с БД", slog.String("error", errPoolPing.Error()))
		return
	}

	metricsServer := &http.Server{
		Addr:    ":9091",
		Handler: promhttp.Handler(),
	}

	go func() {
		log.Info("Запуск сервера метрик на порту: 9091")
		if errMetrics := metricsServer.ListenAndServe(); errMetrics != nil && !errors.Is(errMetrics, http.ErrServerClosed) {
			log.Error("не смог запустить экспорт метрик", slog.String("error", errMetrics.Error()))
		}
	}()

	rc := redis.NewClient(&redis.Options{
		Addr:     fmt.Sprintf("%s:%s", cfg.RedisUrl, cfg.RedisPort),
		Password: cfg.RedisPassword,
		DB:       0,
	})
	defer rc.Close()

	grpcServer := conf.InitGrpc(pool, rc)

	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)

	<-sigChan
	log.Info("Получен сигнал завершения")

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer shutdownCancel()

	if grpcServer != nil {
		grpcServer.GracefulStop()
	}

	if err := metricsServer.Shutdown(shutdownCtx); err != nil {
		log.Error("ошибка при остановке сервера метрик", slog.String("error", err.Error()))
	}

	log.Info("Приложение корректно завершено")
}

func databasePath(cfg *config.Config) string {
	return fmt.Sprintf(
		"postgres://%s:%s@%s:%s/%s?sslmode=disable",
		cfg.DBUser,
		cfg.DBPassword,
		cfg.DBHost,
		cfg.DBPort,
		cfg.DBName,
	)
}

func createPool(ctx context.Context, conf *pgxpool.Config) (*pgxpool.Pool, error) {
	c := config.LoadConfig()

	conf.MaxConns = util.StringToInt(c.DbMaxConns)
	conf.MinConns = util.StringToInt(c.DbMinConns)
	conf.MaxConnLifetime = c.DbMaxConnsLifeTime
	conf.MaxConnIdleTime = c.DbMaxConnsIdleTime
	conf.HealthCheckPeriod = c.DbHealthCheckPeriod

	conf.ConnConfig.RuntimeParams["statement_timeout"] = c.DbStatementTimeout
	conf.ConnConfig.RuntimeParams["idle_in_transaction_session_timeout"] = c.DbIdleTransSessTimeout

	pool, err := pgxpool.NewWithConfig(ctx, conf)
	if err != nil {
		return nil, fmt.Errorf("не получилось создать pool: %w", err)
	}

	return pool, nil
}
