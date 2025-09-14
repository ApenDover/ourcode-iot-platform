package domain

import (
	"time"

	"github.com/google/uuid"
)

type Router struct {
	ID           uuid.UUID
	SerialNumber *string
	IpAddress    *string
	LastSeenAt   *time.Time
	CreatedAt    time.Time
}
