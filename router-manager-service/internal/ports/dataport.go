package ports

import (
	"context"
	"github.com/google/uuid"
	"router-manager-service/internal/core/domain"
	"time"
)

type DataPort interface {
	CreateCommands(ctx context.Context, routerSerial []string, command domain.Command) error
	PollCommands(ctx context.Context, routerSerial string) ([]domain.Command, error)
	AckCommand(ctx context.Context, routerSerial string, commandId uuid.UUID) error
	MarkExpiredAsError(ctx context.Context, timeout time.Duration) error
}
