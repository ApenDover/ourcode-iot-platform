package conf

import (
	"context"
	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracegrpc"
	"go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	"log/slog"
	"router-manager-service/config"
	"router-manager-service/internal/adapters/db"
	"router-manager-service/internal/adapters/innergrpc"
	"router-manager-service/internal/adapters/rediscli"
	"router-manager-service/internal/adapters/routes"
	"router-manager-service/internal/core/domainService"
	"router-manager-service/internal/core/redisDomainService"
	"router-manager-service/internal/core/service"
	"router-manager-service/internal/util"
	"strings"
	"time"
)

func InitGrpc(pool *pgxpool.Pool, databasePath string, redisClient *rediscli.Client) {
	log := util.GetLogger(context.Background())
	flyWayPath := util.MigrationsPath(config.LoadConfig().MigrationPath)
	log.Info("ищу миграции по адресу", slog.String("миграции", flyWayPath))
	RunMigrations(databasePath, flyWayPath)

	managerService := innergrpc.NewServer(pool, redisClient)
	period := config.LoadConfig().CheckExpiredInterval
	expiredTime := config.LoadConfig().TimeExpired
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

	err := managerService.Start("9092")
	if err != nil {
		log.Error("Не смог запустить сервер", slog.String("error", err.Error()))
	}

}

func InitHttp(databasePath string, redisClient *rediscli.Client) error {
	log := util.GetLogger(context.Background())

	RunMigrations(databasePath, util.MigrationsPath(config.LoadConfig().MigrationPath))

	pool, err := pgxpool.New(context.Background(), databasePath)
	if err != nil {
		log.Error("Не смог подключиться к БД", slog.String("error", err.Error()))
		return err
	}
	defer pool.Close()

	repoCommand := db.NewPostgresCommandRepository(pool)
	commandService := domainService.NewCommandService(repoCommand)

	repoRouter := db.NewPostgresRouterRepository(pool)
	routerService := domainService.NewRouterService(repoRouter)
	redisCommand := redisDomainService.NewRedisCommandRepository(redisClient)
	redisRouter := redisDomainService.NewRedisRouterRepository(redisClient)
	ms := service.NewManagerService(commandService, routerService, redisCommand, redisRouter)

	r := gin.Default()
	routes.RegisterRoutes(r, ms)
	err = r.Run(":8080")
	if err != nil {
		log.Error("приложение не запустилось", slog.String("error", err.Error()))
		return err
	}
	return nil
}

func InitTracer(endpoint string) (*sdktrace.TracerProvider, error) {
	ctx := context.Background()
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
