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
	Status      CommandStatus
	SentAt      *time.Time
	AckedAt     *time.Time
	CreatedAt   time.Time
}

type CommandStatus string

const (
	CommandStatusPending CommandStatus = "PENDING"
	CommandStatusSent    CommandStatus = "SENT"
	CommandStatusAcked   CommandStatus = "ACKED"
	CommandStatusError   CommandStatus = "ERROR"
)
