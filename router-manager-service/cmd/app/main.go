package main

import (
	"context"
	"errors"
	"fmt"
	"github.com/redis/go-redis/v9"
	"google.golang.org/grpc"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"router-manager-service/config"
	"router-manager-service/internal/conf"
	"router-manager-service/internal/conf/logutil"
	"router-manager-service/internal/conf/util"
	"router-manager-service/internal/db"
	"sync"
	"syscall"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

type App struct {
	ctx           context.Context
	cancel        context.CancelFunc
	wg            sync.WaitGroup
	grpcServer    *grpc.Server
	metricsServer *http.Server
}

func main() {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	app := &App{
		ctx:    ctx,
		cancel: cancel,
	}

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
		log.Error("не удалось распарсить конфиг", slog.String("error", err.Error()))
		return
	}

	pool, pgxErr := createPool(ctx, dbConfig)
	if pgxErr != nil {
		log.Error("ошибка подключения к БД", slog.String("error", pgxErr.Error()))
		return
	}
	defer pool.Close()

	migrator, err := db.NewMigrator(dbPath)
	if err != nil {
		log.Error("не удалось создать мигратор", slog.String("error", err.Error()))
		return
	}
	defer migrator.Close()

	if err := migrator.Up(); err != nil {
		log.Error("ошибка выполнения миграций", slog.String("error", err.Error()))
		return
	} else {
		log.Info("миграции применены к базе", slog.String("databaseUrl", dbPath))
	}

	if errPoolPing := pool.Ping(ctx); errPoolPing != nil {
		log.Error("не удалось проверить соединение с БД", slog.String("error", errPoolPing.Error()))
		return
	}

	app.metricsServer = &http.Server{
		Addr:    ":9091",
		Handler: promhttp.Handler(),
	}

	app.wg.Add(1)
	go func() {
		defer app.wg.Done()
		log.Info("Запуск сервера метрик на порту: 9091")
		if errMetrics := app.metricsServer.ListenAndServe(); errMetrics != nil && !errors.Is(errMetrics, http.ErrServerClosed) {
			log.Error("не смог запустить экспорт метрик", slog.String("error", errMetrics.Error()))
		}
	}()

	rc := redis.NewClient(&redis.Options{
		Addr:     fmt.Sprintf("%s:%s", cfg.RedisUrl, cfg.RedisPort),
		Password: cfg.RedisPassword,
		DB:       0,
	})
	defer rc.Close()

	app.grpcServer = conf.InitGrpc(ctx, pool, rc, &app.wg)

	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)

	<-sigChan
	log.Info("Получен сигнал завершения")

	app.shutdown()
	log.Info("Приложение корректно завершено")
}

func (a *App) shutdown() {
	log := util.GetLogger(a.ctx)

	log.Info("Начало graceful shutdown...")

	a.cancel()
	log.Info("Контекст приложения отменен")

	if a.grpcServer != nil {
		log.Info("Остановка gRPC сервера...")
		grpcShutdownCtx, grpcCancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer grpcCancel()

		done := make(chan struct{})
		go func() {
			a.grpcServer.GracefulStop()
			close(done)
		}()

		select {
		case <-done:
			log.Info("gRPC сервер остановлен")
		case <-grpcShutdownCtx.Done():
			log.Warn("Таймаут остановки gRPC сервера, принудительная остановка")
			a.grpcServer.Stop()
		}
	}

	if a.metricsServer != nil {
		log.Info("Остановка сервера метрик...")
		metricsShutdownCtx, metricsCancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer metricsCancel()

		if err := a.metricsServer.Shutdown(metricsShutdownCtx); err != nil {
			log.Error("Ошибка при остановке сервера метрик", slog.String("error", err.Error()))
		} else {
			log.Info("Сервер метрик остановлен")
		}
	}

	log.Info("Ожидание завершения фоновых горутин...")
	done := make(chan struct{})
	go func() {
		a.wg.Wait()
		close(done)
	}()

	select {
	case <-done:
		log.Info("Все горутины завершены")
	case <-time.After(15 * time.Second):
		log.Warn("Таймаут graceful shutdown - принудительное завершение")
	}

	log.Info("Graceful shutdown завершен")
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
