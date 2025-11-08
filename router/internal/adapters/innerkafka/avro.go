package innerkafka

import (
	"fmt"

	"github.com/linkedin/goavro/v2"
)

type AvroSerializer struct {
	codec *goavro.Codec
}

func NewAvroSerializer(codec *goavro.Codec) *AvroSerializer {
	return &AvroSerializer{
		codec: codec,
	}
}

func (s *AvroSerializer) SerializeDeviceEvent(event *DeviceEvent) ([]byte, error) {
	if s.codec == nil {
		return nil, fmt.Errorf("avro codec not initialized")
	}

	nativeData := map[string]interface{}{
		"eventId":   event.EventID,
		"timestamp": event.Timestamp.UnixMilli(),
		"type":      event.Type,
		"payload":   event.Payload,
		"device": map[string]interface{}{
			"deviceId":   event.Device.DeviceId,
			"deviceType": event.Device.DeviceType,
			"meta":       event.Device.Meta,
			"createdAt":  event.Device.CreatedAt.UnixMilli(),
		},
	}

	binaryData, err := s.codec.BinaryFromNative(nil, nativeData)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize device event to avro: %w", err)
	}

	return binaryData, nil
}
