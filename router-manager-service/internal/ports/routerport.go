package ports

import (
	"context"
	"router-manager-service/internal/core/domain"
)

type RouterPort interface {
	EnsureRoutersExist(ctx context.Context, serials []string) error
	GetBySerial(ctx context.Context, serial string) (*domain.Router, error)
	GetAllRoutersBySerials(ctx context.Context, serials []string) ([]*domain.Router, error)
	UpdateSeenAt(ctx context.Context, serials []string) error
	GetAllRouters(ctx context.Context) ([]*domain.Router, error)
	Save(ctx context.Context, router domain.Router) error
}
