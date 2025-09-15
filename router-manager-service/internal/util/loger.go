package util

import (
	"context"
	"go.opentelemetry.io/otel/trace"
	"log/slog"
	"os"
)

const (
	envLocal = "local"
	envProd  = "prod"
)

var (
	logger *slog.Logger
)

func GetLogger(ctx context.Context) *slog.Logger {
	span := trace.SpanFromContext(ctx)
	sc := span.SpanContext()

	if sc.HasTraceID() && sc.HasSpanID() {
		logger = logger.With(
			slog.String("trace_id", sc.TraceID().String()),
			slog.String("span_id", sc.SpanID().String()),
		)
	}
	return logger
}

func SetupLogger(env string) *slog.Logger {
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
	logger = log
	return logger
}
