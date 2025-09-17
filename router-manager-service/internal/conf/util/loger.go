package util

import (
	"context"
	"go.opentelemetry.io/otel/trace"
	"log/slog"
	"os"
	"router-manager-service/internal/conf/logutil"
)

const (
	envLocal = "local"
	envProd  = "prod"
)

func GetLogger(ctx context.Context) *slog.Logger {
	var log *slog.Logger
	level := logutil.LoadConfig().Level
	var logLevel slog.Level
	switch level {
	case "INFO":
		logLevel = slog.LevelDebug
	case "DEBUG":
		logLevel = slog.LevelDebug
	case "WARN":
		logLevel = slog.LevelWarn
	case "ERROR":
		logLevel = slog.LevelError
	default:
		logLevel = slog.LevelInfo
	}
	switch logutil.LoadConfig().Profile {
	case envLocal:
		log = slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{Level: logLevel}))
	case envProd:
		log = slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: logLevel}))
	default:
		log = slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{Level: logLevel}))
	}

	span := trace.SpanFromContext(ctx)
	sc := span.SpanContext()

	if sc.HasTraceID() && sc.HasSpanID() {
		log = log.With(
			slog.String("trace_id", sc.TraceID().String()),
			slog.String("span_id", sc.SpanID().String()),
		)
	}

	return log
}
