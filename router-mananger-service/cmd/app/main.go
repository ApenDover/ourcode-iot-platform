package main

import (
	"fmt"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/net/context"
	"log/slog"
	"router-mananger-service/config"
	"router-mananger-service/internal/conf"
	"router-mananger-service/internal/util"
)

func main() {
	log := util.SetupLogger(config.LoadConfig().Profile)
	dbPath := databasePath()
	pool, err := pgxpool.New(context.Background(), dbPath)
	if err != nil {
		log.Error("Не смог подключиться к БД", slog.String("error", err.Error()))
	}
	conf.InitGrpc(pool, dbPath)
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
