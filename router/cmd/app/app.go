package app

import (
	"context"
	"fmt"
	"log/slog"
	genproto "router-manager-service/internal/ports/genproto"
	"sync"
	"time"

	"router-manager-service/config"
	"router-manager-service/internal/adapters/innergrpc"
	"router-manager-service/internal/conf/util"
)

type Dependencies struct {
	Config *config.Config
}

type App struct {
	ctx          context.Context
	cancel       context.CancelFunc
	wg           sync.WaitGroup
	deps         *Dependencies
	gClient      *innergrpc.Client
	shutdownOnce sync.Once
}

func New(ctx context.Context, deps *Dependencies) (*App, error) {
	ctx, cancel := context.WithCancel(ctx)

	grpcClient, err := innergrpc.New(ctx, deps.Config.GRPCServerAddress+":"+deps.Config.GRPCPort)
	if err != nil {
		cancel()
		return nil, fmt.Errorf("failed to create gRPC client: %w", err)
	}

	return &App{
		ctx:     ctx,
		cancel:  cancel,
		deps:    deps,
		gClient: grpcClient,
	}, nil
}

func (a *App) Start() error {
	a.startBackgroundTasks()

	log := util.GetLogger(a.ctx)
	log.Info("App started with gRPC client",
		slog.String("server_address", a.deps.Config.GRPCServerAddress),
		slog.String("router_serial", a.deps.Config.RouterSerial),
	)

	return nil
}

func (a *App) startBackgroundTasks() {
	a.wg.Add(1)
	go func() {
		defer a.wg.Done()
		a.scheduledRequestWorker()
	}()
}

func (a *App) scheduledRequestWorker() {
	ticker := time.NewTicker(5 * time.Second)
	defer ticker.Stop()

	log := util.GetLogger(a.ctx)
	log.Info("Запущен worker для отправки запросов по расписанию")

	a.sendGRPCRequests()

	for {
		select {
		case <-a.ctx.Done():
			log.Info("Worker остановлен")
			return
		case <-ticker.C:
			a.sendGRPCRequests()
		}
	}
}

func (a *App) sendGRPCRequests() {
	log := util.GetLogger(a.ctx)

	routerSerial := a.deps.Config.RouterSerial
	resp, err := a.gClient.PollCommands(a.ctx, routerSerial)
	if err != nil {
		log.Error("Poll commands failed",
			slog.String("router_serial", routerSerial),
			slog.String("error", err.Error()),
		)
		return
	}

	processedCount := 0
	for _, cmd := range resp.Commands {
		log.Info("Received command",
			slog.String("command_id", cmd.Id),
			slog.String("command_type", cmd.CommandType),
			slog.String("router_serial", cmd.RouterSerial),
		)

		if err := a.processCommand(cmd); err != nil {
			log.Error("Failed to process command",
				slog.String("command_id", cmd.Id),
				slog.String("error", err.Error()),
			)
			continue
		}

		_, err = a.gClient.AckCommand(a.ctx, routerSerial, cmd.Id)
		if err != nil {
			log.Error("Failed to ack command",
				slog.String("command_id", cmd.Id),
				slog.String("error", err.Error()),
			)
			continue
		}

		processedCount++
	}

	if processedCount > 0 {
		log.Info("Successfully processed commands",
			slog.Int("processed_count", processedCount),
			slog.Int("total_commands", len(resp.Commands)),
		)
	}
}

func (a *App) processCommand(cmd *genproto.Command) error {
	log := util.GetLogger(a.ctx)
	log.Debug("Processing command",
		slog.String("command_id", cmd.Id),
		slog.String("command_type", cmd.CommandType),
	)
	return nil
}

func (a *App) Stop() {
	a.shutdownOnce.Do(a.gracefulShutdown)
}

func (a *App) gracefulShutdown() {
	log := util.GetLogger(a.ctx)
	log.Info("Начинаю graceful shutdown")

	// Закрываем gRPC клиент
	if a.gClient != nil {
		if err := a.gClient.Close(); err != nil {
			log.Error("Failed to close gRPC client", slog.String("error", err.Error()))
		} else {
			log.Info("gRPC client closed")
		}
	}

	a.cancel()
	a.waitForShutdown(log)

	log.Info("Graceful shutdown завершен")
}

func (a *App) waitForShutdown(log *slog.Logger) {
	shutdownChan := make(chan struct{})

	go func() {
		a.wg.Wait()
		close(shutdownChan)
	}()

	select {
	case <-shutdownChan:
		log.Info("Все горутины завершены")
	case <-time.After(30 * time.Second):
		log.Warn("Таймаут graceful shutdown истек")
	}
}
