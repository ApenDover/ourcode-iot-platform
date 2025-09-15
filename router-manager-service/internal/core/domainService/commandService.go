package domainService

import (
	"context"
	"log/slog"
	"router-manager-service/internal/metrics"
	"router-manager-service/internal/util"
	"strconv"
	"time"

	"github.com/google/uuid"
	"router-manager-service/internal/domain"
	"router-manager-service/internal/ports"
)

type CommandService struct {
	repo ports.CommandRepository
}

func NewCommandService(repo ports.CommandRepository) *CommandService {
	return &CommandService{repo: repo}
}

func (s *CommandService) CreateCommand(ctx context.Context, routerId uuid.UUID, commandType string, payload map[string]any) domain.Command {
	log := util.GetLogger(ctx)
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

func (s *CommandService) CreateCommandsForAll(ctx context.Context, routerIds []uuid.UUID, commandType string, payload map[string]any) []domain.Command {
	log := util.GetLogger(ctx)
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

func (s *CommandService) GetPendingCommands(ctx context.Context, routerSerial string) []domain.Command {
	log := util.GetLogger(ctx)
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

func (s *CommandService) UpdateCommandsStatus(ctx context.Context, commands []domain.Command) {
	log := util.GetLogger(ctx)
	err := s.repo.SetSentStatusForPendingByCommandIds(commands)
	if err != nil {
		metrics.CommandErrors.WithLabelValues("update-commands-status").Inc()
		log.Error("не удалось обновить статус командам",
			slog.String("error", err.Error()))
	}
}

func (s *CommandService) AckCommand(ctx context.Context, routerId, commandID uuid.UUID) {
	log := util.GetLogger(ctx)
	err := s.repo.UpdateStatusToAcked(routerId, commandID)
	if err != nil {
		metrics.CommandErrors.WithLabelValues("ack-command").Inc()
		log.Error("не удалось подтвердить команду",
			slog.String("routerId", routerId.String()),
			slog.String("commandId", commandID.String()),
			slog.String("error", err.Error()))
	}
}

func (s *CommandService) MarkExpiredAsError(ctx context.Context, checkExpired time.Duration) {
	log := util.GetLogger(ctx)
	util.GetLogger(ctx).Info("Проверка просроченных ответов..")
	err := s.repo.MarkExpiredAsError(checkExpired)
	if err != nil {
		metrics.CommandErrors.WithLabelValues("mark-expired-as-error").Inc()
		log.Error("не удалось проверить и пометить в ERROR просроченные команды", slog.String("error", err.Error()))
		return
	}
}
