package util

import (
	"log/slog"
	"os"
	"router-mananger-service/config"
	"sync"
)

const (
	envLocal = "local"
	envProd  = "prod"
)

var (
	logger *slog.Logger
	once   sync.Once
)

func GetLogger() *slog.Logger {
	once.Do(func() {
		logger = setupLogger(config.LoadConfig().Profile)
	})
	return logger
}

func setupLogger(env string) *slog.Logger {
	var log *slog.Logger
	switch env {
	case envLocal:
		log = slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelDebug}))
	case envProd:
		log = slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))
	default:
		log = slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))
	}
	log.Info("Профиль выбран", slog.String("profile", env))
	return log
}
