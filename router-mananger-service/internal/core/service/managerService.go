package service

import (
	"github.com/google/uuid"
	"router-mananger-service/internal/core/domainService"
	"router-mananger-service/internal/domain"
)

type ManagerService struct {
	commandService *domainService.CommandService
	routerService  *domainService.RouterService
}

func NewManagerService(commandService *domainService.CommandService, routerService *domainService.RouterService) *ManagerService {
	return &ManagerService{
		commandService: commandService,
		routerService:  routerService,
	}
}

func (m *ManagerService) CreateCommand(routerID uuid.UUID, commandType string, payload map[string]any) domain.Command {
	m.routerService.Create(routerID)
	return m.commandService.CreateCommand(routerID, commandType, payload)
}

func (m *ManagerService) CreateCommandForAll(commandType string, payload map[string]any) []domain.Command {
	ids := m.routerService.GetAllRouterIds()
	return m.commandService.CreateCommandsForAll(ids, commandType, payload)
}

func (m *ManagerService) GetActualCommands(routerId uuid.UUID) []domain.Command {
	pending := m.commandService.GetPendingCommands(routerId)
	m.commandService.UpdateCommandsStatus(pending)
	return pending
}
