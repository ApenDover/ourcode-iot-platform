package innerkafka

import (
	"time"
)

// DeviceEvent сообщение устройства для Kafka
type DeviceEvent struct {
	EventID   string    `json:"eventId"`
	Timestamp time.Time `json:"timestamp"`
	Type      EventType `json:"type"`
	Payload   string    `json:"payload"`
	Device    Device    `json:"device"`
}

// EventType тип события
type EventType string

const (
	EventTypeTemperature EventType = "TEMPERATURE"
	EventTypeHumidity    EventType = "HUMIDITY"
	EventTypeStatus      EventType = "STATUS"
	EventTypeError       EventType = "ERROR"
)

// Device информация об устройстве
type Device struct {
	ID           string `json:"id"`
	SerialNumber string `json:"serialNumber"`
	Model        string `json:"model"`
	Location     string `json:"location"`
}
