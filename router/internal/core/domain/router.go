package domain

import (
	"github.com/google/uuid"
	"time"
)

type Router struct {
	ID           uuid.UUID
	SerialNumber string
	IpAddress    *string
	LastSeenAt   *time.Time
	CreatedAt    time.Time
}
