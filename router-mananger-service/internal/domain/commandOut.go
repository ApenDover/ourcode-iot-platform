package domain

import (
	"time"

	"github.com/google/uuid"
)

type CommandOut struct {
	ID           uuid.UUID
	SerialNumber string
	CommandType  string
	Payload      *map[string]any
	Status       CommandStatus
	SentAt       *time.Time
	AckedAt      *time.Time
	CreatedAt    time.Time
}
