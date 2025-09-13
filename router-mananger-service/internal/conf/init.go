package conf

import (
	"context"
	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
	"google.golang.org/grpc"
	"log/slog"
	"net"
	"router-mananger-service/internal/adapters/db"
	"router-mananger-service/internal/adapters/innergrpc"
	"router-mananger-service/internal/adapters/routes"
	"router-mananger-service/internal/core/domainService"
	"router-mananger-service/internal/core/service"
	routermanager "router-mananger-service/internal/ports/genproto"
	"router-mananger-service/internal/util"
)

func InitHttp() error {
	log := util.GetLogger()

	databasePath := util.DatabasePath()
	err := RunMigrations(databasePath, util.MigrationsPath())

	if err != nil {
		log.Error("Не смог применить flyway миграции", slog.String("error", err.Error()))
		return err
	}

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

	ms := service.NewManagerService(commandService, routerService)

	r := gin.Default()
	routes.RegisterRoutes(r, ms)
	err = r.Run(":8080")
	if err != nil {
		log.Error("приложение не запустилось", slog.String("error", err.Error()))
		return err
	}
	return nil
}

func InitGrpc() error {
	log := util.GetLogger()

	// Подключаемся к БД и применяем миграции
	databasePath := util.DatabasePath()
	err := RunMigrations(databasePath, util.MigrationsPath())
	if err != nil {
		log.Error("Не смог применить миграции", slog.String("error", err.Error()))
		return err
	}

	// Создаем пул подключений к БД
	pool, err := pgxpool.New(context.Background(), databasePath)
	if err != nil {
		log.Error("Не смог подключиться к БД", slog.String("error", err.Error()))
		return err
	}
	defer pool.Close()

	// Создаем репозиторий и сервис (бизнес-логика)
	repo := db.NewPostgresCommandRepository(pool)
	_ = domainService.NewCommandService(repo)

	// Создаем ЭКЗЕМПЛЯР нашего gRPC сервера (адаптера)
	// Здесь вызываем конструктор из нашего пакета grpc!
	ourGrpcServer := innergrpc.NewServer(pool) // Это наш собственный конструктор

	// Создаем listener
	lis, err := net.Listen("tcp", ":9090")
	if err != nil {
		log.Error("Не смог создать listener", slog.String("error", err.Error()))
		return err
	}

	// Создаем ТРАНСПОРТНЫЙ gRPC сервер (из google.golang.org/grpc)
	grpcTransportServer := grpc.NewServer()

	// Регистрируем НАШ сервер в gRPC транспорте
	routermanager.RegisterRouterManagerServiceServer(grpcTransportServer, ourGrpcServer)

	// Запускаем gRPC сервер
	log.Info("Запуск gRPC сервера на порту :9090")
	err = grpcTransportServer.Serve(lis)
	if err != nil {
		log.Error("gRPC сервер не запустился", slog.String("error", err.Error()))
		return err
	}
	return nil
}
