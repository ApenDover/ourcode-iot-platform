package innerkafka

import (
	"context"
	"crypto/rand"
	"encoding/binary"
	"fmt"
	"io/fs"
	"log/slog"
	"router-manager-service/config"
	"strings"
	"time"

	"embed"

	"github.com/linkedin/goavro/v2"
	"github.com/riferrei/srclient"
	"github.com/segmentio/kafka-go"
	"go.opentelemetry.io/otel/trace"
)

//go:embed avro/*.avsc
var avroSchemasFS embed.FS

type Producer struct {
	writer   *kafka.Writer
	srClient *srclient.SchemaRegistryClient
	topic    string
	codec    *goavro.Codec // Avro codec из Schema Registry
	schemaID int
	logger   *slog.Logger
}

func NewProducer(cfg *config.Config, logger *slog.Logger) (*Producer, error) {
	writer := &kafka.Writer{
		Addr:         kafka.TCP(cfg.BootstrapServers),
		Topic:        cfg.Topic,
		Balancer:     &kafka.LeastBytes{},
		BatchTimeout: 10 * time.Millisecond,
		RequiredAcks: kafka.RequireAll,
	}

	srClient := srclient.NewSchemaRegistryClient(cfg.SchemaRegistryURL)

	schema, codec, err := registerSchemaAndGetCodec(srClient, cfg.Topic)
	if err != nil {
		return nil, fmt.Errorf("failed to register schema: %w", err)
	}

	producer := &Producer{
		writer:   writer,
		srClient: srClient,
		topic:    cfg.Topic,
		codec:    codec,
		schemaID: schema.ID(),
		logger:   logger,
	}

	logger.Info("Schema Registry Producer initialized",
		slog.Int("schema_id", schema.ID()),
		slog.String("topic", cfg.Topic),
	)

	return producer, nil
}

func registerSchemaAndGetCodec(srClient *srclient.SchemaRegistryClient, topic string) (*srclient.Schema, *goavro.Codec, error) {
	subject := topic + "-value"

	schema, err := srClient.GetLatestSchema(subject)
	if err == nil {
		codec, err := goavro.NewCodec(schema.Schema())
		if err != nil {
			return nil, nil, fmt.Errorf("failed to create codec from existing schema: %w", err)
		}
		return schema, codec, nil
	}

	avroSchema, err := loadSchemaFromFile()
	if err != nil {
		return nil, nil, fmt.Errorf("failed to load schema from file: %w", err)
	}

	schema, err = srClient.CreateSchema(subject, avroSchema, srclient.Avro)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to create schema: %w", err)
	}

	codec, err := goavro.NewCodec(avroSchema)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to create codec from new schema: %w", err)
	}

	return schema, codec, nil
}

func loadSchemaFromFile() (string, error) {
	files, err := fs.ReadDir(avroSchemasFS, "avro")
	if err != nil {
		return "", fmt.Errorf("failed to read schemas directory: %w", err)
	}

	var schemas []string
	for _, file := range files {
		if !file.IsDir() && strings.HasSuffix(file.Name(), ".avsc") {
			schemaPath := "avro/" + file.Name()
			schemaData, err := fs.ReadFile(avroSchemasFS, schemaPath)
			if err != nil {
				return "", fmt.Errorf("failed to read schema %s: %w", file.Name(), err)
			}
			schemas = append(schemas, string(schemaData))
		}
	}

	if len(schemas) == 0 {
		return "", fmt.Errorf("no .avsc files found in avro directory")
	}

	if len(schemas) > 1 {
		return fmt.Sprintf("[%s]", strings.Join(schemas, ",")), nil
	}

	return schemas[0], nil
}

func (p *Producer) SendDeviceEvent(ctx context.Context, event *DeviceEvent) error {

	avroData, err := p.serializeToAvro(event)
	if err != nil {
		return fmt.Errorf("failed to serialize event: %w", err)
	}

	messageValue := make([]byte, 0, 5+len(avroData))
	messageValue = append(messageValue, 0) // Magic byte
	schemaIDBytes := make([]byte, 4)
	binary.BigEndian.PutUint32(schemaIDBytes, uint32(p.schemaID))
	messageValue = append(messageValue, schemaIDBytes...)
	messageValue = append(messageValue, avroData...)

	msg := kafka.Message{
		Key:     []byte(event.Device.DeviceId),
		Value:   messageValue,
		Headers: []kafka.Header{},
	}

	if err := p.writer.WriteMessages(ctx, msg); err != nil {
		return fmt.Errorf("failed to write message: %w", err)
	}

	p.logger.Debug("Device event sent to Kafka with Schema Registry",
		slog.String("eventId", event.EventID),
		slog.String("device_serial", event.Device.DeviceId),
		slog.String("event_type", event.Type),
		slog.Int("schema_id", p.schemaID),
		slog.Int("kafka_headers_count", len(msg.Headers)),
	)

	return nil
}

func (p *Producer) serializeToAvro(event *DeviceEvent) ([]byte, error) {
	if p.codec == nil {
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

	return p.codec.BinaryFromNative(nil, nativeData)
}

func (p *Producer) Close() error {
	if p.writer != nil {
		return p.writer.Close()
	}
	return nil
}

func generateSpanID() trace.SpanID {
	var id [8]byte
	_, _ = rand.Read(id[:])
	return trace.SpanID(id)
}
