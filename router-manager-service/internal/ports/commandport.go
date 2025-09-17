package ports

import (
	"context"
	"github.com/google/uuid"
	"router-manager-service/internal/core/domain"
)

type CommandPort interface {
	GetAll(ctx context.Context) ([]domain.Command, error)
	GetAllByRouterId(ctx context.Context, routerId uuid.UUID) ([]domain.Command, error)
}
