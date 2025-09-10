package ports

import (
	"github.com/google/uuid"
	"router-mananger-service/internal/domain"
)

type CommandRepository interface {
	Save(cmd domain.Command) error
	SaveAll(cmd []domain.Command) error
	GetByIdAndStatuses(uuid uuid.UUID, statuses []string) ([]domain.Command, error)
	UpdateStatusById(uuid uuid.UUID) error
	UpdateStatusToAcked(taskUuid uuid.UUID, commandUuid uuid.UUID) error
	FindAllIDs() ([]uuid.UUID, error)
}
