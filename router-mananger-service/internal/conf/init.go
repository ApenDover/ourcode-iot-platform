package conf

import (
	"context"
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
	"log/slog"
	"router-mananger-service/config"
	"router-mananger-service/internal/adapters/db"
	"router-mananger-service/internal/adapters/innergrpc"
	"router-mananger-service/internal/adapters/routes"
	"router-mananger-service/internal/core/domainService"
	"router-mananger-service/internal/core/service"
	"router-mananger-service/internal/util"
	"time"
)

func InitGrpc() {
	log := util.GetLogger()

	pool, err := pgxpool.New(context.Background(), databasePath())
	if err != nil {
		log.Error("Не смог подключиться к БД", slog.String("error", err.Error()))
	}

	RunMigrations(databasePath(), util.MigrationsPath())

	managerService := innergrpc.NewServer(pool)
	period := config.LoadConfig().CheckExpiredInterval
	expiredTime := config.LoadConfig().TimeExpired
	log.Info("Параметры проверки на просроченные статусы",
		slog.String("период запуска", period.String()),
		slog.String("просрочка после", expiredTime.String()))

	go func() {
		ticker := time.NewTicker(period)
		defer ticker.Stop()
		for range ticker.C {
			managerService.ManagerService.MarkExpiredAsError(expiredTime)
		}
	}()

	err = managerService.Start("9090")
	if err != nil {
		log.Error("Не смог запустить сервер", slog.String("error", err.Error()))
	}

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

func InitHttp() error {
	log := util.GetLogger()

	RunMigrations(databasePath(), util.MigrationsPath())

	pool, err := pgxpool.New(context.Background(), databasePath())
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
