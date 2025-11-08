package innerkafka

import (
	"context"
	"fmt"
	"log/slog"
	"router-manager-service/config"
	"time"

	_ "github.com/linkedin/goavro/v2"
	"github.com/segmentio/kafka-go"
)

// Producer Kafka продюсер
type Producer struct {
	writer     *kafka.Writer
	topic      string
	serializer *AvroSerializer
	logger     *slog.Logger
}

func NewProducer(cfg *config.Config, serializer *AvroSerializer, logger *slog.Logger) (*Producer, error) {
	writer := &kafka.Writer{
		Addr:         kafka.TCP(cfg.BootstrapServers),
		Topic:        cfg.Topic,
		Balancer:     &kafka.LeastBytes{},
		BatchTimeout: 10 * time.Millisecond,
		RequiredAcks: kafka.RequireAll,
	}

	return &Producer{
		writer:     writer,
		topic:      cfg.Topic,
		serializer: serializer,
		logger:     logger,
	}, nil
}

// SendDeviceEvent отправляет событие устройства в Kafka
func (p *Producer) SendDeviceEvent(event *DeviceEvent) error {
	// Сериализуем в Avro
	avroData, err := p.serializer.SerializeDeviceEvent(event)
	if err != nil {
		return fmt.Errorf("failed to serialize event: %w", err)
	}

	// Создаем Kafka сообщение
	msg := kafka.Message{
		Key:   []byte(event.Device.SerialNumber),
		Value: avroData,
	}

	// Отправляем сообщение
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := p.writer.WriteMessages(ctx, msg); err != nil {
		return fmt.Errorf("failed to write message: %w", err)
	}

	p.logger.Debug("Device event sent to Kafka",
		slog.String("event_id", event.EventID),
		slog.String("device_serial", event.Device.SerialNumber),
		slog.String("event_type", string(event.Type)),
	)

	return nil
}

func (p *Producer) Close() error {
	if p.writer != nil {
		return p.writer.Close()
	}
	return nil
}
