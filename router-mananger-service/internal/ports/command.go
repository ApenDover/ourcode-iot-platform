package ports

import (
	"github.com/google/uuid"
	"router-mananger-service/internal/domain"
)

type CommandRepository interface {
	Save(cmd domain.Command) error
	SaveAll(cmd []domain.Command) error
	GetByIdAndStatuses(uuid uuid.UUID, statuses []domain.CommandStatus) ([]domain.Command, error)
	GetAllByRouterId(routerId uuid.UUID) ([]domain.Command, error)
	SetSentStatusForPendingByRouterId(uuid uuid.UUID) error
	SetSentStatusForPendingByCommandIds(cmd []domain.Command) error
	UpdateStatusToAcked(taskUuid uuid.UUID, commandUuid uuid.UUID) error
}
