package main

import (
	"fmt"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"golang.org/x/net/context"
	"log/slog"
	"net/http"
	"router-manager-service/config"
	"router-manager-service/internal/adapters/rediscli"
	"router-manager-service/internal/conf"
	"router-manager-service/internal/util"
)

func main() {
	log := util.GetLogger(context.Background())
	alloyEndPoint := config.LoadConfig().AlloyUrl
	tp, err := conf.InitTracer(alloyEndPoint)
	if err != nil {
		log.Error("не удалось инициализировать TracerProvider", slog.String("error", err.Error()))
		return
	}
	defer func() {
		_ = tp.Shutdown(context.Background())
	}()

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

	redisClient := rediscli.NewRedisClient(config.LoadConfig().RedisUrl, config.LoadConfig().RedisPort, config.LoadConfig().RedisPassword)

	conf.InitGrpc(pool, dbPath, redisClient)
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
