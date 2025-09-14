package ports

import (
	"github.com/google/uuid"
	"router-manager-service/internal/domain"
)

type RouterRepository interface {
	Save(cmd domain.Router) error
	SaveAll(cmd []domain.Router) error
	GetBySerial(serial string) (domain.Router, error)
	GetAllIds() ([]uuid.UUID, error)
	GetAllRouters() ([]domain.Router, error)
	GetAllRoutersBySerials(serials []string) ([]domain.Router, error)
	UpdateSeenAt(serials []string) error
}
