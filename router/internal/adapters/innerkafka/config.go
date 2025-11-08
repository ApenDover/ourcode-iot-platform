package innerkafka

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/linkedin/goavro/v2"
)

type SchemaLoader struct {
	deviceEventCodec *goavro.Codec
}

func NewSchemaLoader(schemasPath string) (*SchemaLoader, error) {
	files, err := os.ReadDir(schemasPath)
	if err != nil {
		return nil, fmt.Errorf("failed to read schemas directory: %w", err)
	}

	var schemas []string
	for _, file := range files {
		if !file.IsDir() && strings.HasSuffix(file.Name(), ".avsc") {
			schemaPath := filepath.Join(schemasPath, file.Name())
			schemaData, err := os.ReadFile(schemaPath)
			if err != nil {
				return nil, fmt.Errorf("failed to read schema %s: %w", file.Name(), err)
			}
			schemas = append(schemas, string(schemaData))
		}
	}

	if len(schemas) == 0 {
		return nil, fmt.Errorf("no .avsc files found in %s", schemasPath)
	}

	combinedSchema := fmt.Sprintf("[%s]", strings.Join(schemas, ","))

	deviceEventCodec, err := goavro.NewCodec(combinedSchema)
	if err != nil {
		return nil, fmt.Errorf("failed to create device event codec: %w", err)
	}

	return &SchemaLoader{
		deviceEventCodec: deviceEventCodec,
	}, nil
}

func (s *SchemaLoader) GetDeviceEventCodec() *goavro.Codec {
	return s.deviceEventCodec
}
