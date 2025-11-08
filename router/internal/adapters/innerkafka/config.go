package innerkafka

import (
	"embed"
	"fmt"
	"io/fs"
	"strings"

	"github.com/linkedin/goavro/v2"
)

//go:embed avro/*.avsc
var avroSchemasFS embed.FS

type SchemaLoader struct {
	deviceEventCodec *goavro.Codec
}

func NewSchemaLoader() (*SchemaLoader, error) {
	files, err := fs.ReadDir(avroSchemasFS, "avro")
	if err != nil {
		return nil, fmt.Errorf("failed to read schemas directory from embed: %w", err)
	}

	var schemas []string
	var fileNames []string
	for _, file := range files {
		if !file.IsDir() && strings.HasSuffix(file.Name(), ".avsc") {
			schemaPath := "avro/" + file.Name()
			schemaData, err := fs.ReadFile(avroSchemasFS, schemaPath)
			if err != nil {
				return nil, fmt.Errorf("failed to read schema %s: %w", file.Name(), err)
			}
			schemas = append(schemas, string(schemaData))
			fileNames = append(fileNames, file.Name())
		}
	}

	if len(schemas) == 0 {
		return nil, fmt.Errorf("no .avsc files found in embed FS")
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
