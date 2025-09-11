package service

import (
	"log/slog"
	"router-mananger-service/internal/util"
	"strconv"
	"time"

	"github.com/google/uuid"
	"router-mananger-service/internal/domain"
	"router-mananger-service/internal/ports"
)

var log = util.GetLogger()

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
		Status:      domain.CommandStatusPending,
		CreatedAt:   time.Now(),
	}

	err := s.repo.Save(cmd)
	if err != nil {
		log.Error("не удалось сохранить команду", slog.String("routerId", routerID.String()), slog.String("error", err.Error()))
	}
	log.Debug("команды сохранена",
		slog.String("routerId", routerID.String()))
	return cmd, err
}

func (s *CommandService) SendCommandAll(commandType string, payload map[string]any) ([]domain.Command, error) {
	routerIDs, err := s.repo.FindAllIDs()
	if err != nil {
		log.Error("не удалось найти роутеры",
			slog.String("error", err.Error()))
		return nil, err
	}

	if len(routerIDs) == 0 {
		return []domain.Command{}, nil
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
			Status:      domain.CommandStatusPending,
			CreatedAt:   time.Now(),
		}

		batch = append(batch, cmd)
		commands = append(commands, cmd)

		if len(batch) >= batchSize {
			err := s.repo.SaveAll(batch)
			if err != nil {
				log.Error("не удалось сохранить команды",
					slog.String("count", strconv.Itoa(len(batch))),
					slog.String("error", err.Error()))
				return commands, err
			}
			batch = batch[:0]
		}
	}

	if len(batch) > 0 {
		err := s.repo.SaveAll(batch)
		if err != nil {
			log.Error("не удалось сохранить команды",
				slog.String("count", strconv.Itoa(len(batch))),
				slog.String("error", err.Error()))
			return commands, err
		}
	}

	log.Debug("команды сохранены", slog.String("count", strconv.Itoa(len(commands))))
	return commands, nil
}

func (s *CommandService) GetCommands(routerID uuid.UUID) ([]domain.Command, error) {
	commands, err := s.repo.GetByIdAndStatuses(routerID, []domain.CommandStatus{domain.CommandStatusPending})
	if err != nil {
		log.Error("не удалось получить команды",
			slog.String("routerId", routerID.String()),
			slog.String("error", err.Error()))
		return nil, err
	}
	return commands, nil
}

func (s *CommandService) UpdateCommandsStatus(routerID uuid.UUID) error {
	err := s.repo.SetSentStatusForPendingByRouterId(routerID)
	if err != nil {
		log.Error("не удалось обновить статус командам",
			slog.String("routerId", routerID.String()),
			slog.String("error", err.Error()))
		return err
	}
	return nil
}

func (s *CommandService) AckCommand(routerID, commandID uuid.UUID) error {
	err := s.repo.UpdateStatusToAcked(routerID, commandID)
	if err != nil {
		log.Error("не удалось подтвердить команду",
			slog.String("routerId", routerID.String()),
			slog.String("commandId", commandID.String()),
			slog.String("error", err.Error()))
		return err
	}
	return nil
}
