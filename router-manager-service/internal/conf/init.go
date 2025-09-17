package conf

import (
	"context"
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
	util2 "router-manager-service/internal/conf/util"
	"strings"
	"time"
)

func InitGrpc(pool *pgxpool.Pool, redisClient *redis.Client) *grpc.Server {
	log := util2.GetLogger(context.Background())
	cfg := config.LoadConfig()

	managerService := innergrpc.NewServer(pool, redisClient)
	period := cfg.CheckExpiredInterval
	expiredTime := cfg.TimeExpired
	log.Info("Параметры проверки на просроченные статусы",
		slog.String("запускаю проверку каждые (период)", period.String()),
		slog.String("проставляю ERROR для SENT после (период)", expiredTime.String()))

	go func() {
		ticker := time.NewTicker(period)
		defer ticker.Stop()
		for range ticker.C {
			managerService.ManagerService.MarkExpiredAsError(context.Background(), expiredTime)
		}
	}()

	grpcServer, err := managerService.Start("9092")
	if err != nil {
		log.Error("Не смог запустить сервер", slog.String("error", err.Error()))
	}
	return grpcServer
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
