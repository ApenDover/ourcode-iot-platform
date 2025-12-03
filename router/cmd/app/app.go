package app

import (
	"context"
	"fmt"
	"log/slog"
	"net/http"
	genproto "router-manager-service/internal/ports/genproto"
	"sync"
	"time"

	"router-manager-service/config"
	"router-manager-service/internal/adapters/innergrpc"
	"router-manager-service/internal/adapters/innerkafka"
	"router-manager-service/internal/conf/util"

	"github.com/google/uuid"
)

type Dependencies struct {
	Config *config.Config
}

type App struct {
	ctx           context.Context
	cancel        context.CancelFunc
	wg            sync.WaitGroup
	deps          *Dependencies
	gClient       *innergrpc.Client
	kafkaProducer *innerkafka.Producer
	shutdownOnce  sync.Once
}

func New(ctx context.Context, deps *Dependencies) (*App, error) {
	ctx, cancel := context.WithCancel(ctx)

	grpcClient, err := innergrpc.New(ctx, deps.Config.GRPCServerAddress+":"+deps.Config.GRPCPort)
	if err != nil {
		cancel()
		return nil, fmt.Errorf("failed to create gRPC client: %w", err)
	}

	app := &App{
		ctx:     ctx,
		cancel:  cancel,
		deps:    deps,
		gClient: grpcClient,
	}

	if err := app.initKafka(); err != nil {
		cancel()
		return nil, fmt.Errorf("failed to init Kafka: %w", err)
	}

	return app, nil
}

func (a *App) initKafka() error {
	log := util.GetLogger(a.ctx)

	if a.deps.Config.BootstrapServers == "" || a.deps.Config.Topic == "" {
		log.Info("Kafka config not provided, skipping Kafka initialization")
		return nil
	}

	if a.deps.Config.SchemaRegistryURL != "" {
		resp, err := http.Get(a.deps.Config.SchemaRegistryURL + "/config")
		if err != nil {
			log.Warn("Cannot connect to Schema Registry", slog.String("error", err.Error()))
		} else {
			defer resp.Body.Close()
			log.Info("Successfully connected to Schema Registry")
		}
	}

	kafkaProducer, err := innerkafka.NewProducer(a.deps.Config, log)
	if err != nil {
		return fmt.Errorf("failed to create kafka producer: %w", err)
	}

	a.kafkaProducer = kafkaProducer
	log.Info("Kafka producer with Schema Registry initialized",
		slog.String("bootstrap_servers", a.deps.Config.BootstrapServers),
		slog.String("topic", a.deps.Config.Topic),
		slog.String("schema_registry", a.deps.Config.SchemaRegistryURL),
	)

	return nil
}

func (a *App) Start() error {
	a.startBackgroundTasks()

	log := util.GetLogger(a.ctx)

	fields := []any{
		slog.String("server_address", a.deps.Config.GRPCServerAddress),
		slog.String("router_serial", a.deps.Config.RouterSerial),
	}

	if a.kafkaProducer != nil {
		fields = append(fields, slog.Bool("kafka_enabled", true))
	}

	log.Info("App started with gRPC client", fields...)

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
	ticker := time.NewTicker(a.deps.Config.PollInterval)
	defer ticker.Stop()

	log := util.GetLogger(a.ctx)
	log.Info("Запущен worker для отправки запросов по расписанию",
		slog.String("poll_interval", a.deps.Config.PollInterval.String()),
	)

	a.sendGRPCRequests()
	a.sendDeviceEvents()

	for {
		select {
		case <-a.ctx.Done():
			log.Info("Worker остановлен")
			return
		case <-ticker.C:
			a.sendGRPCRequests()
			a.sendDeviceEvents()
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

func (a *App) sendDeviceEvents() {
	if a.kafkaProducer == nil {
		return
	}

	log := util.GetLogger(a.ctx)

	deviceEvent := &innerkafka.DeviceEvent{
		EventID:   uuid.New().String(),
		Timestamp: time.Now(),
		Type:      "STATUS",
		Payload:   fmt.Sprintf(`{"STATUS": "online"}`),
		Device: innerkafka.Device{
			DeviceId:   "01K6GJ564FPTXDWX8R1F91VZK0",
			DeviceType: "ROUTER",
			Meta:       "Office A",
			CreatedAt:  time.Now(),
		},
	}

	spanContext, err := a.kafkaProducer.SendDeviceEvent(context.Background(), deviceEvent)
	if err != nil {
		log.Error("Failed to send device event to Kafka",
			slog.String("error", err.Error()),
			slog.String("eventId", deviceEvent.EventID),
			slog.String("traceId", spanContext.TraceID().String()),
			slog.String("spanId", spanContext.SpanID().String()),
		)
		return
	}

	log.Info("Device event sent to Kafka",
		slog.String("eventId", deviceEvent.EventID),
		slog.String("deviceId", deviceEvent.Device.DeviceId),
		slog.String("traceId", spanContext.TraceID().String()),
	)
}

func (a *App) processCommand(cmd *genproto.Command) error {
	log := util.GetLogger(a.ctx)
	log.Debug("Processing command",
		slog.String("command_id", cmd.Id),
		slog.String("command_type", cmd.CommandType),
	)

	// логика обработки команды

	return nil
}

func (a *App) Stop() {
	a.shutdownOnce.Do(a.gracefulShutdown)
}

func (a *App) gracefulShutdown() {
	log := util.GetLogger(a.ctx)
	log.Info("Начинаю graceful shutdown")

	if a.gClient != nil {
		if err := a.gClient.Close(); err != nil {
			log.Error("Failed to close gRPC client", slog.String("error", err.Error()))
		} else {
			log.Info("gRPC client closed")
		}
	}

	if a.kafkaProducer != nil {
		a.kafkaProducer.Close()
		log.Info("Kafka producer closed")
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
