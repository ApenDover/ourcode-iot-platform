package service

import (
	"time"

	"github.com/google/uuid"
	"router-mananger-service/internal/domain"
	"router-mananger-service/internal/ports"
)

type CommandService struct {
	repo ports.CommandRepository
}

func NewCommandService(repo ports.CommandRepository) *CommandService {
	return &CommandService{repo: repo}
}

func (s *CommandService) SendCommand(routerID uuid.UUID, commandType string, payload map[string]any) (domain.Command, error) {
	cmd := domain.Command{
		ID:          uuid.New(),
		RouterID:    routerID,
		CommandType: commandType,
		Payload:     payload,
		Status:      "PENDING",
		CreatedAt:   time.Now(),
	}

	err := s.repo.Save(cmd)
	return cmd, err
}

func (s *CommandService) SendCommandAll(commandType string, payload map[string]any) ([]domain.Command, error) {
	routerIDs, err := s.repo.FindAllIDs()
	if err != nil {
		return nil, err
	}

	if len(routerIDs) == 0 {
		return nil, nil
	}

	var commands []domain.Command
	batchSize := 1000
	var batch []domain.Command

	for _, rID := range routerIDs {
		cmd := domain.Command{
			ID:          uuid.New(),
			RouterID:    rID,
			CommandType: commandType,
			Payload:     payload,
			Status:      "PENDING",
			CreatedAt:   time.Now(),
		}

		batch = append(batch, cmd)
		commands = append(commands, cmd)

		if len(batch) >= batchSize {
			if err := s.repo.SaveAll(batch); err != nil {
				return commands, err
			}
			batch = batch[:0]
		}
	}

	if len(batch) > 0 {
		if err := s.repo.SaveAll(batch); err != nil {
			return commands, err
		}
	}

	return commands, nil
}

func (s *CommandService) GetCommands(routerID uuid.UUID) ([]domain.Command, error) {
	commands, err := s.repo.GetByIdAndStatuses(routerID, []string{"PENDING"})
	if err != nil {
		return nil, err
	}
	return commands, nil
}

func (s *CommandService) UpdateCommandsStatus(routerID uuid.UUID) error {
	err := s.repo.UpdateStatusById(routerID)
	if err != nil {
		return err
	}
	return nil
}

func (s *CommandService) AckCommand(routerID, commandID uuid.UUID) error {
	err := s.repo.UpdateStatusToAcked(routerID, commandID)
	if err != nil {
		return err
	}
	return nil
}
