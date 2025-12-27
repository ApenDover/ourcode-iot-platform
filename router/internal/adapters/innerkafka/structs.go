package innerkafka

import (
	"time"
)

type DeviceEvent struct {
	EventID   string
	Timestamp time.Time
	Type      string
	Payload   string
	Device    Device
}

type Device struct {
	DeviceId   string
	DeviceType string
	Meta       string
	CreatedAt  time.Time
	Update     bool
}
