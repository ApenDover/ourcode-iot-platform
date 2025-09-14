package ports

import (
	"github.com/google/uuid"
	"router-manager-service/internal/domain"
	"time"
)

type CommandRepository interface {
	Save(cmd domain.Command) error
	SaveAll(cmd []domain.Command) error
	GetBySerialAndStatuses(serial string, statuses []domain.CommandStatus) ([]domain.Command, error)
	GetAllByRouterSerial(serial string) ([]domain.Command, error)
	SetSentStatusForPendingByRouterSerial(serial string) error
	SetSentStatusForPendingByCommandIds(cmd []domain.Command) error
	UpdateStatusToAcked(serial, commandUuid uuid.UUID) error
	MarkExpiredAsError(timeout time.Duration) error
}
