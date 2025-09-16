package service

import (
	"context"
	"github.com/google/uuid"
	"github.com/prometheus/client_golang/prometheus"
	"router-manager-service/internal/core/domainService"
	"router-manager-service/internal/core/redisDomainService"
	"router-manager-service/internal/domain"
	"router-manager-service/internal/metrics"
	"time"
)

type ManagerService struct {
	commandService *domainService.CommandService
	routerService  *domainService.RouterService
	redisCommands  *redisDomainService.RedisCommandRepository
	redisRouters   *redisDomainService.RedisRouterRepository
}

func NewManagerService(commandService *domainService.CommandService, routerService *domainService.RouterService,
	redisCommands *redisDomainService.RedisCommandRepository, redisRouters *redisDomainService.RedisRouterRepository) *ManagerService {
	return &ManagerService{
		commandService: commandService,
		routerService:  routerService,
		redisCommands:  redisCommands,
		redisRouters:   redisRouters,
	}
}

func (m *ManagerService) CreateCommand(ctx context.Context, serial string, commandType string, payload map[string]any) domain.CommandOut {
	timer := prometheus.NewTimer(metrics.MethodDuration.WithLabelValues("CreateCommand"))
	defer timer.ObserveDuration()
	metrics.CommandsSent.WithLabelValues("CreateCommand").Inc()

	router := m.redisRouters.GetBySerial(ctx, serial)
	if router == nil {
		newRouter := m.routerService.Create(ctx, serial)
		m.redisRouters.Save(ctx, newRouter)
		router = &newRouter
	}

	command := m.commandService.CreateCommand(ctx, router.ID, commandType, payload)
	if command != nil {
		m.redisCommands.Save(ctx, *command)
	}
	return domain.CommandOut{
		ID:           command.ID,
		SerialNumber: serial,
		CommandType:  command.CommandType,
		Payload:      &command.Payload,
		Status:       command.Status,
		SentAt:       command.SentAt,
		AckedAt:      command.AckedAt,
		CreatedAt:    command.CreatedAt,
	}
}

func (m *ManagerService) CreateCommandForAll(ctx context.Context, commandType string, payload map[string]any) []domain.CommandOut {
	timer := prometheus.NewTimer(metrics.MethodDuration.WithLabelValues("CreateCommandForAll"))
	defer timer.ObserveDuration()
	metrics.CommandsSent.WithLabelValues("CreateCommandForAll").Inc()

	routers := m.routerService.GetAllRouters(ctx)

	ids := make([]uuid.UUID, len(routers))
	for i, r := range routers {
		ids[i] = r.ID
	}

	commands := m.commandService.CreateCommandsForAll(ctx, ids, commandType, payload)
	if len(commands) > 0 {
		m.redisCommands.SaveAll(ctx, commands)
	}

	routerMap := make(map[uuid.UUID]string, len(routers))
	for _, r := range routers {
		routerMap[r.ID] = r.SerialNumber
	}

	result := make([]domain.CommandOut, len(commands))
	for i, cmd := range commands {
		serial := routerMap[cmd.RouterID]
		result[i] = domain.CommandOut{
			ID:           cmd.ID,
			SerialNumber: serial,
			CommandType:  cmd.CommandType,
			Payload:      &cmd.Payload,
			Status:       cmd.Status,
			SentAt:       cmd.SentAt,
			AckedAt:      cmd.AckedAt,
			CreatedAt:    cmd.CreatedAt,
		}
	}

	return result
}

func (m *ManagerService) GetPendingCommandsAndMarkItSent(ctx context.Context, routerSerial string) []domain.CommandOut {
	timer := prometheus.NewTimer(metrics.MethodDuration.WithLabelValues("PollCommand"))
	defer timer.ObserveDuration()
	metrics.CommandsPolled.Inc()

	pending := m.commandService.GetPendingCommands(ctx, routerSerial)

	m.commandService.UpdateCommandsStatus(ctx, pending)
	m.routerService.UpdateSeenAt(ctx, []string{routerSerial})
	result := make([]domain.CommandOut, len(pending))
	for i, cmd := range pending {
		result[i] = domain.CommandOut{
			ID:           cmd.ID,
			SerialNumber: routerSerial,
			CommandType:  cmd.CommandType,
			Payload:      &cmd.Payload,
			Status:       cmd.Status,
			SentAt:       cmd.SentAt,
			AckedAt:      cmd.AckedAt,
			CreatedAt:    cmd.CreatedAt,
		}
	}
	return result
}

func (m *ManagerService) AckCommand(ctx context.Context, serial string, commandId uuid.UUID) {
	timer := prometheus.NewTimer(metrics.MethodDuration.WithLabelValues("AckCommand"))
	defer timer.ObserveDuration()
	metrics.CommandsAcked.Inc()

	router := m.routerService.GetBySerial(ctx, serial)
	m.commandService.AckCommand(ctx, router.ID, commandId)
	m.routerService.UpdateSeenAt(ctx, []string{serial})
}

func (m *ManagerService) MarkExpiredAsError(ctx context.Context, checkExpired time.Duration) {
	m.commandService.MarkExpiredAsError(ctx, checkExpired)
}
