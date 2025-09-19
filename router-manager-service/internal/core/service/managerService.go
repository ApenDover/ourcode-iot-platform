package service

import (
	"context"
	"log/slog"
	"router-manager-service/internal/conf/util"
	domain2 "router-manager-service/internal/core/domain"
	"time"

	"github.com/google/uuid"
	"github.com/prometheus/client_golang/prometheus"
	"router-manager-service/internal/metrics"
	"router-manager-service/internal/ports"
)

type ManagerService struct {
	dataPort         ports.DataPort
	routerRepository ports.RouterPort
	redisRouters     ports.CachePort
}

func NewManagerService(dataPort ports.DataPort, routerPort ports.RouterPort, cachePort ports.CachePort) *ManagerService {
	return &ManagerService{
		dataPort:         dataPort,
		routerRepository: routerPort,
		redisRouters:     cachePort,
	}
}

func (m *ManagerService) CreateCommand(ctx context.Context, serial string, commandType string, payload map[string]any) (domain2.CommandOut, error) {
	timer := prometheus.NewTimer(metrics.MethodDuration.WithLabelValues("CreateCommand"))
	defer timer.ObserveDuration()
	metrics.CommandsSent.WithLabelValues("CreateCommand").Inc()

	router, errRedis := m.redisRouters.GetRouter(ctx, serial)
	if router == nil || errRedis != nil {
		router = m.saveRouter(ctx, serial)
	}

	cmd := domain2.Command{
		ID:          uuid.New(),
		RouterID:    router.ID,
		CommandType: commandType,
		Payload:     payload,
		Status:      domain2.CommandStatusPending,
		CreatedAt:   time.Now(),
	}

	if errCreateCommand := m.dataPort.CreateCommands(ctx, []string{serial}, cmd); errCreateCommand != nil {
		return domain2.CommandOut{}, errCreateCommand
	}

	return domain2.CommandOut{
		ID:           cmd.ID,
		SerialNumber: serial,
		CommandType:  cmd.CommandType,
		Payload:      &cmd.Payload,
		Status:       cmd.Status,
		CreatedAt:    cmd.CreatedAt,
	}, nil
}

func (m *ManagerService) CreateCommandForAll(ctx context.Context, commandType string, payload map[string]any) ([]domain2.CommandOut, error) {
	timer := prometheus.NewTimer(metrics.MethodDuration.WithLabelValues("CreateCommandForAll"))
	defer timer.ObserveDuration()

	routers, errGetAll := m.routerRepository.GetAllRouters(ctx)
	if errGetAll != nil {
		return nil, errGetAll
	}

	results := make([]domain2.CommandOut, 0, len(routers))
	for _, r := range routers {
		cmd := domain2.Command{
			ID:          uuid.New(),
			RouterID:    r.ID,
			CommandType: commandType,
			Payload:     payload,
			Status:      domain2.CommandStatusPending,
			CreatedAt:   time.Now(),
		}
		if err := m.dataPort.CreateCommands(ctx, []string{r.SerialNumber}, cmd); err != nil {
			metrics.CommandErrors.WithLabelValues("CreateCommands").Inc()
			return nil, err
		}

		results = append(results, domain2.CommandOut{
			ID:           cmd.ID,
			SerialNumber: r.SerialNumber,
			CommandType:  cmd.CommandType,
			Payload:      &cmd.Payload,
			Status:       cmd.Status,
			CreatedAt:    cmd.CreatedAt,
		})
	}

	metrics.CommandsSent.WithLabelValues("CreateCommandForAll").Inc()
	return results, nil
}

func (m *ManagerService) GetPendingCommandsAndMarkItSent(ctx context.Context, serial string) ([]domain2.CommandOut, error) {
	timer := prometheus.NewTimer(metrics.MethodDuration.WithLabelValues("PollCommand"))
	defer timer.ObserveDuration()

	pending, err := m.dataPort.PollCommands(ctx, serial)
	if err != nil {
		metrics.CommandErrors.WithLabelValues("PollCommands").Inc()
		return nil, err
	}

	results := make([]domain2.CommandOut, len(pending))
	now := time.Now()
	for i, cmd := range pending {
		results[i] = domain2.CommandOut{
			ID:           cmd.ID,
			SerialNumber: serial,
			CommandType:  cmd.CommandType,
			Payload:      &cmd.Payload,
			Status:       domain2.CommandStatusSent,
			CreatedAt:    cmd.CreatedAt,
			SentAt:       &now,
		}
	}
	metrics.CommandsPolled.Inc()
	return results, nil
}

func (m *ManagerService) AckCommand(ctx context.Context, serial string, commandId uuid.UUID) error {
	timer := prometheus.NewTimer(metrics.MethodDuration.WithLabelValues("AckCommand"))
	defer timer.ObserveDuration()

	if err := m.dataPort.AckCommand(ctx, serial, commandId); err != nil {
		metrics.CommandErrors.WithLabelValues("AckCommand").Inc()
		return err
	}
	metrics.CommandsAcked.Inc()
	return nil
}

func (m *ManagerService) MarkExpiredAsError(ctx context.Context, timeout time.Duration) error {
	return m.dataPort.MarkExpiredAsError(ctx, timeout)
}

func (m *ManagerService) saveRouter(ctx context.Context, serial string) *domain2.Router {
	timer := prometheus.NewTimer(metrics.MethodDuration.WithLabelValues("SaveRouter"))
	defer timer.ObserveDuration()
	log := util.GetLogger(ctx)
	newRouter := domain2.Router{
		ID:           uuid.New(),
		SerialNumber: serial,
		CreatedAt:    time.Now(),
	}
	err := m.routerRepository.Save(ctx, newRouter)
	if err != nil {
		metrics.RouterErrors.WithLabelValues("save-database").Inc()
		log.Error("Ошибка сохранения роутера в базу данных", slog.String("error", err.Error()), slog.String("router_serial", serial))
		return nil
	}
	errRedis := m.redisRouters.SetRouter(ctx, newRouter, 0)
	if errRedis != nil {
		metrics.RouterErrors.WithLabelValues("save-redis").Inc()
		log.Error("Ошибка сохранения роутера в редис", slog.String("error", errRedis.Error()), slog.String("router_serial", serial))
	}
	return &newRouter
}
