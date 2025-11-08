package innerkafka

import (
	"fmt"
	"log/slog"

	"router-manager-service/config"

	"github.com/confluentinc/confluent-kafka-go/kafka"
	_ "github.com/linkedin/goavro/v2"
)

type Producer struct {
	producer   *kafka.Producer
	topic      string
	serializer *AvroSerializer
	logger     *slog.Logger
}

func NewProducer(cfg *config.Config, serializer *AvroSerializer, logger *slog.Logger) (*Producer, error) {
	kafkaConfig := &kafka.ConfigMap{
		"bootstrap.servers": cfg.BootstrapServers,
		"client.id":         "router-manager-client",
		"acks":              "all",
	}

	producer, err := kafka.NewProducer(kafkaConfig)
	if err != nil {
		return nil, fmt.Errorf("failed to create kafka producer: %w", err)
	}

	go func() {
		for e := range producer.Events() {
			switch ev := e.(type) {
			case *kafka.Message:
				if ev.TopicPartition.Error != nil {
					logger.Error("Delivery failed",
						slog.String("error", ev.TopicPartition.Error.Error()))
				} else {
					logger.Debug("Message delivered",
						slog.String("topic", *ev.TopicPartition.Topic),
						slog.Int64("partition", int64(ev.TopicPartition.Partition)),
						slog.Int64("offset", int64(ev.TopicPartition.Offset)))
				}
			}
		}
	}()

	return &Producer{
		producer:   producer,
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
	kafkaMsg := &kafka.Message{
		TopicPartition: kafka.TopicPartition{
			Topic:     &p.topic,
			Partition: kafka.PartitionAny,
		},
		Value: avroData,
		Key:   []byte(event.Device.SerialNumber),
	}

	// Отправляем сообщение
	deliveryChan := make(chan kafka.Event)
	defer close(deliveryChan)

	if err := p.producer.Produce(kafkaMsg, deliveryChan); err != nil {
		return fmt.Errorf("failed to produce message: %w", err)
	}

	// Ждем подтверждения
	e := <-deliveryChan
	m := e.(*kafka.Message)

	if m.TopicPartition.Error != nil {
		return fmt.Errorf("delivery failed: %w", m.TopicPartition.Error)
	}

	p.logger.Debug("Device event sent to Kafka",
		slog.String("event_id", event.EventID),
		slog.String("device_serial", event.Device.SerialNumber),
		slog.String("event_type", string(event.Type)),
	)

	return nil
}

func (p *Producer) Close() {
	if p.producer != nil {
		p.producer.Flush(5000)
		p.producer.Close()
	}
}
