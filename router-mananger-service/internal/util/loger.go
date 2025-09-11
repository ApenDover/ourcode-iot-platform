package util

import (
	"log/slog"
	"os"
	"sync"
)

const (
	envLocal = "local"
	endProd  = "prod"
)

var (
	logger *slog.Logger
	once   sync.Once
)

func SetupLogger(env string) *slog.Logger {
	var log *slog.Logger
	switch env {
	case envLocal:
		log = slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelDebug}))
	case endProd:
		log = slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))
	default:
		log = slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo}))
	}
	return log
}

func GetLogger() *slog.Logger {
	once.Do(func() {
		logger = SetupLogger(endProd)
	})
	return logger
}
