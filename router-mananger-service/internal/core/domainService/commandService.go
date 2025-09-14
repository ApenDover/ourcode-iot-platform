package domainService

import (
	"log/slog"
	"router-mananger-service/internal/metrics"
	"router-mananger-service/internal/util"
	"strconv"
	"time"

	"github.com/google/uuid"
	"router-mananger-service/internal/domain"
	"router-mananger-service/internal/ports"
)

var log *slog.Logger

type CommandService struct {
	repo ports.CommandRepository
}

func NewCommandService(repo ports.CommandRepository) *CommandService {
	log = util.GetLogger()
	return &CommandService{repo: repo}
}

func (s *CommandService) CreateCommand(routerId uuid.UUID, commandType string, payload map[string]any) domain.Command {
	cmd := domain.Command{
		ID:          uuid.New(),
		RouterID:    routerId,
		CommandType: commandType,
		Payload:     payload,
		Status:      domain.CommandStatusPending,
		CreatedAt:   time.Now(),
	}

	err := s.repo.Save(cmd)
	if err != nil {
		metrics.CommandErrors.WithLabelValues("save").Inc()
		log.Error("не удалось сохранить команду", slog.String("routerId", routerId.String()), slog.String("error", err.Error()))
	}
	log.Debug("команда сохранена",
		slog.String("routerId", routerId.String()))
	return cmd
}

func (s *CommandService) CreateCommandsForAll(routerIds []uuid.UUID, commandType string, payload map[string]any) []domain.Command {
	if len(routerIds) == 0 {
		return []domain.Command{}
	}

	var commands []domain.Command
	batchSize := 1000
	var batch []domain.Command

	for _, rID := range routerIds {
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
				metrics.CommandErrors.WithLabelValues("save-all").Inc()
				log.Error("не удалось сохранить команды",
					slog.String("count", strconv.Itoa(len(batch))),
					slog.String("error", err.Error()))
				return commands
			}
			batch = batch[:0]
		}
	}

	if len(batch) > 0 {
		err := s.repo.SaveAll(batch)
		if err != nil {
			metrics.CommandErrors.WithLabelValues("save-all").Inc()
			log.Error("не удалось сохранить команды",
				slog.String("count", strconv.Itoa(len(batch))),
				slog.String("error", err.Error()))
			return commands
		}
	}

	log.Debug("команды сохранены", slog.String("count", strconv.Itoa(len(commands))))
	return commands
}

func (s *CommandService) GetPendingCommands(routerSerial string) []domain.Command {
	commands, err := s.repo.GetBySerialAndStatuses(routerSerial, []domain.CommandStatus{domain.CommandStatusPending})
	if err != nil {
		metrics.CommandErrors.WithLabelValues("get-pending-commands").Inc()
		log.Error("не удалось получить команды",
			slog.String("routerSerial", routerSerial),
			slog.String("error", err.Error()))
		return nil
	}
	log.Debug("нашел команды", slog.String("count", strconv.Itoa(len(commands))))
	return commands
}

func (s *CommandService) UpdateCommandsStatus(commands []domain.Command) {
	err := s.repo.SetSentStatusForPendingByCommandIds(commands)
	if err != nil {
		metrics.CommandErrors.WithLabelValues("update-commands-status").Inc()
		log.Error("не удалось обновить статус командам",
			slog.String("error", err.Error()))
	}
}

func (s *CommandService) AckCommand(routerId, commandID uuid.UUID) {
	err := s.repo.UpdateStatusToAcked(routerId, commandID)
	if err != nil {
		metrics.CommandErrors.WithLabelValues("ack-command").Inc()
		log.Error("не удалось подтвердить команду",
			slog.String("routerId", routerId.String()),
			slog.String("commandId", commandID.String()),
			slog.String("error", err.Error()))
	}
}

func (s *CommandService) MarkExpiredAsError(checkExpired time.Duration) {
	err := s.repo.MarkExpiredAsError(checkExpired)
	if err != nil {
		metrics.CommandErrors.WithLabelValues("mark-expired-as-error").Inc()
		log.Error("не удалось проверить и пометить в ERROR просроченные команды", slog.String("error", err.Error()))
		return
	}
}
