package kafka

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/linkedin/goavro/v2"
)

type SchemaLoader struct {
	deviceEventCodec *goavro.Codec
}

func NewSchemaLoader(schemasPath string) (*SchemaLoader, error) {
	eventSchemaPath := filepath.Join(schemasPath, "DeviceEvent.avsc")
	eventSchemaData, err := os.ReadFile(eventSchemaPath)
	if err != nil {
		return nil, fmt.Errorf("failed to read device event schema: %w", err)
	}

	eventCodec, err := goavro.NewCodec(string(eventSchemaData))
	if err != nil {
		return nil, fmt.Errorf("failed to create device event codec: %w", err)
	}

	return &SchemaLoader{
		deviceEventCodec: eventCodec,
	}, nil
}

func (s *SchemaLoader) GetDeviceEventCodec() *goavro.Codec {
	return s.deviceEventCodec
}
