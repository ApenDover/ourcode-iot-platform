package ports

import (
	"context"
	"router-manager-service/internal/core/domain"
	"time"
)

type CachePort interface {
	GetRouter(ctx context.Context, serial string) (*domain.Router, error)
	GetRouters(ctx context.Context, serials []string) ([]*domain.Router, error)
	SetRouter(ctx context.Context, router domain.Router, ttl time.Duration) error
	SetRouters(ctx context.Context, routers []domain.Router, ttl time.Duration) error
}
