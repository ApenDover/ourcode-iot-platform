package domain

import (
	"fmt"
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

func (c Command) String() string {
	sentAtStr := "nil"
	if c.SentAt != nil {
		sentAtStr = c.SentAt.Format(time.RFC3339)
	}

	ackedAtStr := "nil"
	if c.AckedAt != nil {
		ackedAtStr = c.AckedAt.Format(time.RFC3339)
	}

	payloadStr := "{}"
	if c.Payload != nil && len(c.Payload) > 0 {
		payloadStr = fmt.Sprintf("%d fields", len(c.Payload))
	}

	return fmt.Sprintf(
		"Command{ID:%s, RouterID:%s, Type:%s, Status:%s, Payload:%s, SentAt:%s, AckedAt:%s, CreatedAt:%s}",
		c.ID, c.RouterID, c.CommandType, c.Status, payloadStr, sentAtStr, ackedAtStr,
		c.CreatedAt.Format(time.RFC3339),
	)
}

type CommandOut struct {
	ID           uuid.UUID
	SerialNumber string
	CommandType  string
	Payload      map[string]any
	Status       CommandStatus
	SentAt       *time.Time
	AckedAt      *time.Time
	CreatedAt    time.Time
}

func (c CommandOut) String() string {
	sentAtStr := "nil"
	if c.SentAt != nil {
		sentAtStr = c.SentAt.Format(time.RFC3339)
	}

	ackedAtStr := "nil"
	if c.AckedAt != nil {
		ackedAtStr = c.AckedAt.Format(time.RFC3339)
	}

	payloadStr := "{}"
	if c.Payload != nil && len(c.Payload) > 0 {
		payloadStr = fmt.Sprintf("%d fields", len(c.Payload))
	}

	return fmt.Sprintf(
		"CommandOut{ID:%s, SerialNumber:%s, Type:%s, Status:%s, Payload:%s, SentAt:%s, AckedAt:%s, CreatedAt:%s}",
		c.ID, c.SerialNumber, c.CommandType, c.Status, payloadStr, sentAtStr, ackedAtStr,
		c.CreatedAt.Format(time.RFC3339),
	)
}

type CommandStatus string

const (
	CommandStatusPending CommandStatus = "PENDING"
	CommandStatusSent    CommandStatus = "SENT"
	CommandStatusAcked   CommandStatus = "ACKED"
	CommandStatusError   CommandStatus = "ERROR"
)
