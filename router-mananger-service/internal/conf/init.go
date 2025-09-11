package conf

import (
	"context"
	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"
	"log/slog"
	"router-mananger-service/internal/adapters/db"
	"router-mananger-service/internal/adapters/routes"
	"router-mananger-service/internal/core/service"
	"router-mananger-service/internal/util"
)

func Init() error {
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

	repo := db.NewPostgresCommandRepository(pool)
	cmdService := service.NewCommandService(repo)

	r := gin.Default()
	routes.RegisterRoutes(r, cmdService)
	err = r.Run(":8080")
	if err != nil {
		log.Error("приложение не запустилось", slog.String("error", err.Error()))
		return err
	}
	return nil
}
