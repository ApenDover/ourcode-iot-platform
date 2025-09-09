package main

import (
	"context"
	"log"
	"router-mananger-service/internal/core/routes"

	"github.com/gin-gonic/gin"
	"github.com/jackc/pgx/v5/pgxpool"

	"router-mananger-service/internal/adapters/db"
	"router-mananger-service/internal/core/service"
)

const postgres_url = "postgres://user:password@localhost:5439/router_db?sslmode=disable"

func main() {
	err := db.RunMigrations(postgres_url)
	if err != nil {
		log.Fatalf("unable to connect to database: %v", err)
		return
	}
	// подключение к базе
	pool, err := pgxpool.New(context.Background(), postgres_url)
	if err != nil {
		log.Fatalf("unable to connect to database: %v", err)
	}
	defer pool.Close()

	repo := db.NewPostgresCommandRepository(pool)
	cmdService := service.NewCommandService(repo)

	// поднимаем Gin
	r := gin.Default()
	routes.RegisterRoutes(r, cmdService)
	r.Run(":8080")
}
