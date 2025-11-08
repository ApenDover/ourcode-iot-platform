package innerkafka

import (
	"context"
	"encoding/binary"
	"fmt"
	"log/slog"
	"router-manager-service/config"
	"time"

	"github.com/linkedin/goavro/v2"
	"github.com/riferrei/srclient"
	"github.com/segmentio/kafka-go"
)

type Producer struct {
	writer   *kafka.Writer
	srClient *srclient.SchemaRegistryClient
	topic    string
	codec    *goavro.Codec // Avro codec из Schema Registry
	schemaID int
	logger   *slog.Logger
}

// Новый конструктор БЕЗ AvroSerializer
func NewProducer(cfg *config.Config, logger *slog.Logger) (*Producer, error) {
	writer := &kafka.Writer{
		Addr:         kafka.TCP(cfg.BootstrapServers),
		Topic:        cfg.Topic,
		Balancer:     &kafka.LeastBytes{},
		BatchTimeout: 10 * time.Millisecond,
		RequiredAcks: kafka.RequireAll,
	}

	// Инициализация Schema Registry клиента
	srClient := srclient.CreateSchemaRegistryClient(cfg.SchemaRegistryURL)

	// Регистрируем схему и получаем codec
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

	// Пытаемся получить существующую схему
	schema, err := srClient.GetLatestSchema(subject)
	if err == nil {
		// Создаем codec из существующей схемы
		codec, err := goavro.NewCodec(schema.Schema())
		if err != nil {
			return nil, nil, fmt.Errorf("failed to create codec from existing schema: %w", err)
		}
		return schema, codec, nil
	}

	// Создаем новую схему
	avroSchema := `{
		"type": "record",
		"name": "DeviceEvent",
		"namespace": "com.nashkod.avro",
		"fields": [
			{"name": "eventId", "type": "string"},
			{"name": "timestamp", "type": "long"},
			{"name": "type", "type": "string"},
			{"name": "payload", "type": "string"},
			{"name": "device", "type": {
				"type": "record",
				"name": "Device", 
				"fields": [
					{"name": "deviceId", "type": "string"},
					{"name": "deviceType", "type": "string"},
					{"name": "meta", "type": "string"},
					{"name": "createdAt", "type": "long"}
				]
			}}
		]
	}`

	schema, err = srClient.CreateSchema(subject, avroSchema, srclient.Avro)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to create schema: %w", err)
	}

	// Создаем codec из новой схемы
	codec, err := goavro.NewCodec(schema.Schema())
	if err != nil {
		return nil, nil, fmt.Errorf("failed to create codec from new schema: %w", err)
	}

	return schema, codec, nil
}

func (p *Producer) SendDeviceEvent(event *DeviceEvent) error {
	// Сериализуем данные в Avro binary
	avroData, err := p.serializeToAvro(event)
	if err != nil {
		return fmt.Errorf("failed to serialize event: %w", err)
	}

	// Формируем сообщение в формате Schema Registry
	messageValue := make([]byte, 0, 5+len(avroData))
	messageValue = append(messageValue, 0) // Magic byte
	// Schema ID (big-endian)
	schemaIDBytes := make([]byte, 4)
	binary.BigEndian.PutUint32(schemaIDBytes, uint32(p.schemaID))
	messageValue = append(messageValue, schemaIDBytes...)
	messageValue = append(messageValue, avroData...)

	// Отправляем в Kafka
	msg := kafka.Message{
		Key:   []byte(event.Device.DeviceId),
		Value: messageValue,
	}

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := p.writer.WriteMessages(ctx, msg); err != nil {
		return fmt.Errorf("failed to write message: %w", err)
	}

	p.logger.Debug("Device event sent to Kafka with Schema Registry",
		slog.String("event_id", event.EventID),
		slog.String("device_serial", event.Device.DeviceId),
		slog.String("event_type", event.Type),
		slog.Int("schema_id", p.schemaID),
	)

	return nil
}

func (p *Producer) serializeToAvro(event *DeviceEvent) ([]byte, error) {
	if p.codec == nil {
		return nil, fmt.Errorf("avro codec not initialized")
	}

	// Подготавливаем данные для сериализации
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
