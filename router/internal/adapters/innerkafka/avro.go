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
		"eventId": event.EventID,
		"timestamp": map[string]interface{}{
			"long.timestamp-millis": event.Timestamp.UnixMilli(),
		},
		"type": map[string]interface{}{
			"string": string(event.Type),
		},
		"payload": event.Payload,
		"device": map[string]interface{}{
			"id":           event.Device.ID,
			"serialNumber": event.Device.SerialNumber,
			"model":        event.Device.Model,
			"location":     event.Device.Location,
		},
	}

	binaryData, err := s.codec.BinaryFromNative(nil, nativeData)
	if err != nil {
		return nil, fmt.Errorf("failed to serialize device event to avro: %w", err)
	}

	return binaryData, nil
}
