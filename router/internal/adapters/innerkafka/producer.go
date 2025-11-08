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

func (p *Producer) SendDeviceEvent(event *DeviceEvent) error {
	avroData, err := p.serializer.SerializeDeviceEvent(event)
	if err != nil {
		return fmt.Errorf("failed to serialize event: %w", err)
	}

	msg := kafka.Message{
		Key:   []byte(event.Device.DeviceId),
		Value: avroData,
	}

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := p.writer.WriteMessages(ctx, msg); err != nil {
		return fmt.Errorf("failed to write message: %w", err)
	}

	p.logger.Debug("Device event sent to Kafka",
		slog.String("event_id", event.EventID),
		slog.String("device_serial", event.Device.DeviceId),
		slog.String("event_type", event.Type),
	)

	return nil
}

func (p *Producer) Close() error {
	if p.writer != nil {
		return p.writer.Close()
	}
	return nil
}
