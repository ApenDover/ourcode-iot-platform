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

func (m *ManagerService) CreateCommand(serial string, commandType string, payload map[string]any) domain.CommandOut {
	router := m.routerService.Create(serial)
	command := m.commandService.CreateCommand(router.ID, commandType, payload)
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

func (m *ManagerService) CreateCommandForAll(commandType string, payload map[string]any) []domain.CommandOut {
	routers := m.routerService.GetAllRouters()
	ids := make([]uuid.UUID, len(routers))
	for i, r := range routers {
		ids[i] = r.ID
	}
	commands := m.commandService.CreateCommandsForAll(ids, commandType, payload)

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

func (m *ManagerService) GetPendingCommandsAndMarkItSent(routerSerial string) []domain.CommandOut {
	pending := m.commandService.GetPendingCommands(routerSerial)
	m.commandService.UpdateCommandsStatus(pending)
	m.routerService.UpdateSeenAt([]string{routerSerial})
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

func (m *ManagerService) AckCommand(serial string, commandId uuid.UUID) {
	m.commandService.AckCommand(serial, commandId)
	m.routerService.UpdateSeenAt([]string{serial})
}

func (m *ManagerService) MarkExpiredAsError() {
	m.commandService.MarkExpiredAsError()
}
