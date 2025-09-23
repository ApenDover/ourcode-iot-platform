package app

import (
	"context"
	"errors"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracegrpc"
	"go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	"google.golang.org/grpc"
	"log/slog"
	"router-manager-service/config"
	"router-manager-service/internal/adapters/innergrpc"
	"router-manager-service/internal/conf/util"
	"strings"
	"sync"
	"time"
)

func InitGrpc(ctx context.Context, pool *pgxpool.Pool, redisClient *redis.Client, wg *sync.WaitGroup) *grpc.Server {
	log := util.GetLogger(ctx)
	cfg := config.LoadConfig()

	managerService := innergrpc.NewServer(pool, redisClient)
	period := cfg.CheckExpiredInterval
	expiredTime := cfg.TimeExpired

	wg.Add(1)
	go func() {
		defer wg.Done()
		runExpiredCommandsChecker(ctx, managerService, period, expiredTime, log)
	}()

	grpcServer, err := managerService.Start(cfg.GRPCPort)
	if err != nil {
		log.Error("Не смог запустить сервер", slog.String("error", err.Error()))
		return nil
	}

	log.Info("gRPC сервер запущен на порту " + cfg.GRPCPort)
	return grpcServer
}

func runExpiredCommandsChecker(ctx context.Context, managerService *innergrpc.Server, period time.Duration, expiredTime time.Duration, log *slog.Logger) {
	log.Info("Запуск Scheduller",
		slog.String("period", period.String()),
		slog.String("ERROR to SENT after", expiredTime.String()))
	ticker := time.NewTicker(period)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			log.Info("Остановка фоновой проверки просроченных команд")
			return
		case <-ticker.C:
			markCtx, cancel := context.WithTimeout(ctx, period/2)

			startTime := time.Now()
			err := managerService.ManagerService.MarkExpiredAsError(markCtx, expiredTime)
			duration := time.Since(startTime)

			cancel()

			if err != nil {
				if errors.Is(err, context.Canceled) || errors.Is(err, context.DeadlineExceeded) {
					log.Debug("Проверка просроченных команд прервана", slog.String("reason", err.Error()))
				} else {
					log.Error("Ошибка при пометке просроченных команд", slog.String("error", err.Error()))
				}
			} else {
				log.Debug("Проверка просроченных команд завершена", slog.Duration("duration", duration))
			}
		}
	}
}

func InitTracer(ctx context.Context, endpoint string) (*sdktrace.TracerProvider, error) {
	endpoint = strings.TrimPrefix(endpoint, "http://")
	endpoint = strings.TrimPrefix(endpoint, "https://")
	exporter, err := otlptracegrpc.New(ctx, otlptracegrpc.WithEndpoint(endpoint), otlptracegrpc.WithInsecure())
	if err != nil {
		return nil, err
	}

	tp := sdktrace.NewTracerProvider(
		sdktrace.WithBatcher(exporter),
		sdktrace.WithResource(resource.Default()),
	)
	otel.SetTracerProvider(tp)
	return tp, nil
}
