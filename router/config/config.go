package config

import (
	"context"
	"os"
	"router-manager-service/internal/conf/util"
	"time"
)

type Config struct {
	Profile              string
	PollInterval         time.Duration
	CheckExpiredInterval time.Duration
	AlloyUrl             string
	MetricsPort          string
	GRPCPort             string
	GRPCServerAddress    string
	RouterSerial         string
	BootstrapServers     string
	Topic                string
	SchemaRegistryURL    string
}

func LoadConfig() *Config {
	return &Config{
		Profile:           getEnvOrDefault("PROFILE", "local"),
		PollInterval:      getTimeOrDefault("ROUTER_POLL_INTERVAL", "10s"),
		AlloyUrl:          getEnvOrDefault("ROUTER_TRACING_OTLP_ENDPOINT_GRPC", "http://alloy:4317"),
		MetricsPort:       getEnvOrDefault("APP_ROUTER_MANAGER_METRICS_PORT", "9091"),
		GRPCPort:          getEnvOrDefault("APP_ROUTER_MANAGER_GRPC_PORT", "9092"),
		GRPCServerAddress: getEnvOrDefault("APP_ROUTER_MANAGER_HOST", "router-manager-service"),
		RouterSerial:      "01K6GJ564FPTXDWX8R1F91VZK0",
		BootstrapServers:  getEnvOrDefault("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"),
		Topic:             getEnvOrDefault("SPRING_KAFKA_TEMPLATE_EVENTS_TOPIC", "events"),
		SchemaRegistryURL: getEnvOrDefault("SPRING_KAFKA_PRODUCER_PROPERTIES_SCHEMA_REGISTRY_URL", "http://schema-registry:8081"),
	}
}

func getEnvOrDefault(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

func getTimeOrDefault(key, defaultValue string) time.Duration {
	log := util.GetLogger(context.Background())
	if value := os.Getenv(key); value != "" {
		duration, err := time.ParseDuration(os.Getenv(key))
		if err != nil {
			log.Error("неверный формат PollInterval в ENV APP_ROUTER_MANAGER_SENT_EXPIRED")
		}
		return duration
	}
	duration, err := time.ParseDuration(defaultValue)
	if err != nil {
		log.Error("неверный формат PollInterval в defaultValue, ENV APP_ROUTER_MANAGER_SENT_EXPIRED отсутствует")
	}
	return duration
}
