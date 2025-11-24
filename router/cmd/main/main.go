package main

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"os/signal"
	"router-manager-service/cmd/app"
	"router-manager-service/config"
	"router-manager-service/internal/conf/logutil"
	"router-manager-service/internal/conf/util"
	"syscall"
	"time"
)

func main() {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	cfg := config.LoadConfig()
	logCfg := logutil.LoadConfig()

	log := util.GetLogger(ctx)
	log.Info("запуск клиентского приложения..",
		slog.String("profile", logCfg.Profile),
		slog.String("version", "1.0.0"),
		slog.String("grpc_server", cfg.GRPCServerAddress),
		slog.String("router_serial", cfg.RouterSerial),
	)

	deps, cleanup, err := initializeDependencies(ctx, cfg, log)
	if err != nil {
		log.Error("Не смог создать все зависимости", slog.String("error", err.Error()))
		os.Exit(1)
	}
	defer cleanup()

	application, err := app.New(ctx, deps)
	if err != nil {
		log.Error("ошибка создания приложения", slog.String("error", err.Error()))
		os.Exit(1)
	}

	if err := application.Start(); err != nil {
		log.Error("ошибка запуска приложения", slog.String("error", err.Error()))
		os.Exit(1)
	}

	log.Info("клиентское приложение запущено")

	waitForShutdownSignal(application, log)
	log.Info("приложение завершено")
}

func initializeDependencies(ctx context.Context, cfg *config.Config, log *slog.Logger) (*app.Dependencies, func(), error) {
	cleanupFuncs := make([]func(), 0)
	cleanup := func() {
		for i := len(cleanupFuncs) - 1; i >= 0; i-- {
			cleanupFuncs[i]()
		}
	}

	tracerProvider, err := app.InitTracer(ctx, cfg.AlloyUrl)
	if err != nil {
		return nil, cleanup, fmt.Errorf("ошибка создания tracer: %w", err)
	}
	cleanupFuncs = append(cleanupFuncs, func() {
		shutdownTracer(tracerProvider, log)
	})

	deps := &app.Dependencies{
		Config: cfg,
	}

	return deps, cleanup, nil
}

func shutdownTracer(tp interface{}, log *slog.Logger) {
	if sh, ok := tp.(interface{ Shutdown(context.Context) error }); ok {
		ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()

		if err := sh.Shutdown(ctx); err != nil {
			log.Error("Не смог завершить tracer", slog.String("error", err.Error()))
		} else {
			log.Info("Tracer успешно завершен")
		}
	}
}

func waitForShutdownSignal(application *app.App, log *slog.Logger) {
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM, syscall.SIGQUIT)

	sig := <-sigChan
	log.Info("Получен сигнал завершения", slog.String("signal", sig.String()))

	application.Stop()
}
