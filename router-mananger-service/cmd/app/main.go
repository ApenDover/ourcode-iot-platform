package main

import (
	"fmt"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"golang.org/x/net/context"
	"log/slog"
	"net/http"
	"router-mananger-service/config"
	"router-mananger-service/internal/conf"
	"router-mananger-service/internal/util"
)

func main() {
	log := util.SetupLogger(config.LoadConfig().Profile)
	dbPath := databasePath()
	pool, err := pgxpool.New(context.Background(), dbPath)
	if err != nil {
		log.Error("ошибка подключения к БД", slog.String("error", err.Error()))
	}
	go func() {
		http.Handle("/metrics", promhttp.Handler())
		err := http.ListenAndServe(":9091", nil)
		if err != nil {
			log.Error("не смог запустить экспорт метрик", slog.String("error", err.Error()))
			return
		}
	}()

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
