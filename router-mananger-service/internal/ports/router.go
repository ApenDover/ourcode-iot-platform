package ports

import (
	"github.com/google/uuid"
	"router-mananger-service/internal/domain"
)

type RouterRepository interface {
	Save(cmd domain.Router) error
	SaveAll(cmd []domain.Router) error
	GetById(uuid uuid.UUID) (domain.Router, error)
	GetAllIds() ([]uuid.UUID, error)
	UpdateSeenAt(uuid []uuid.UUID) error
}
