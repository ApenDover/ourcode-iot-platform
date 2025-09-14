package conf

import (
	"context"
	"fmt"
	"github.com/jackc/pgx/v5/pgxpool"
	"log/slog"
	"router-mananger-service/config"
	"router-mananger-service/internal/adapters/innergrpc"
	"router-mananger-service/internal/util"
)

func InitGrpc() {
	log := util.GetLogger()

	// Подключение к БД
	pool, err := pgxpool.New(context.Background(), databasePath())
	if err != nil {
		log.Error("Не смог подключиться к БД", slog.String("error", err.Error()))
	}

	RunMigrations(databasePath(), util.MigrationsPath())

	// Репозитории и сервисы
	managerService := innergrpc.NewServer(pool)
	err = managerService.Start("9090")
	if err != nil {
		log.Error("Не смог запустить сервер", slog.String("error", err.Error()))
	}

	//// Создаём наш gRPC сервер
	//grpcServer := grpc.NewServer()
	//
	//routermanager.RegisterRouterManagerServiceServer(grpcServer, managerService)
	//
	//// Listener
	//lis, err := net.Listen("tcp", ":9090")
	//if err != nil {
	//	log.Error("Не смог создать listener", slog.String("error", err.Error()))
	//	return err
	//}
	//
	//log.Info("Запуск gRPC сервера на порту :9090")
	//return grpcServer.Serve(lis)
}

func databasePath() string {
	c := config.LoadConfig()
	return fmt.Sprintf(
		"postgres://%s:%s@%s:%s/%s?sslmode=disable",
		c.DBUser,
		c.DBPassword,
		c.DBHost,
		c.DBPort,
		c.DBName,
	)
}

//func InitHttp() error {
//	log := util.GetLogger()
//
//	err := RunMigrations(databasePath(), util.MigrationsPath())
//
//	if err != nil {
//		log.Error("Не смог применить flyway миграции", slog.String("error", err.Error()))
//		return err
//	}
//
//	pool, err := pgxpool.New(context.Background(), databasePath())
//	if err != nil {
//		log.Error("Не смог подключиться к БД", slog.String("error", err.Error()))
//		return err
//	}
//	defer pool.Close()
//
//	repoCommand := db.NewPostgresCommandRepository(pool)
//	commandService := domainService.NewCommandService(repoCommand)
//
//	repoRouter := db.NewPostgresRouterRepository(pool)
//	routerService := domainService.NewRouterService(repoRouter)
//
//	ms := service.NewManagerService(commandService, routerService)
//
//	r := gin.Default()
//	routes.RegisterRoutes(r, ms)
//	err = r.Run(":8080")
//	if err != nil {
//		log.Error("приложение не запустилось", slog.String("error", err.Error()))
//		return err
//	}
//	return nil
//}
