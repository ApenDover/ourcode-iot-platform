package domain

import (
	"time"

	"github.com/google/uuid"
)

type Command struct {
	ID          uuid.UUID
	RouterID    uuid.UUID
	CommandType string
	Payload     map[string]any
	Status      string
	SentAt      *time.Time
	AckedAt     *time.Time
	CreatedAt   time.Time
}
