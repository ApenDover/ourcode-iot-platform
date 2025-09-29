package config

import (
	"context"
	"os"
	"router-manager-service/internal/conf/util"
	"time"
)

type Config struct {
	DBHost                 string
	DBPort                 string
	DBUser                 string
	DBPassword             string
	DBName                 string
	Profile                string
	TimeExpired            time.Duration
	CheckExpiredInterval   time.Duration
	MigrationPath          string
	AlloyUrl               string
	RedisUrl               string
	RedisPort              string
	RedisPassword          string
	DbMaxConns             string
	DbMinConns             string
	DbMaxConnsLifeTime     time.Duration
	DbMaxConnsIdleTime     time.Duration
	DbHealthCheckPeriod    time.Duration
	DbStatementTimeout     string
	DbIdleTransSessTimeout string
	DbConnectionTimeout    string
	MetricsPort            string
	GRPCPort               string
}

func LoadConfig() *Config {
	return &Config{
		DBHost:                 getEnvOrDefault("APP_ROUTER_MANAGER_DATASOURCE_HOST", "localhost"),
		DBPort:                 getEnvOrDefault("APP_ROUTER_MANAGER_DATASOURCE_PORT", "5439"),
		DBUser:                 getEnvOrDefault("APP_ROUTER_MANAGER_DATASOURCE_USERNAME", "user"),
		DBPassword:             getEnvOrDefault("APP_ROUTER_MANAGER_DATASOURCE_PASSWORD", "password"),
		DBName:                 getEnvOrDefault("APP_ROUTER_MANAGER_DATASOURCE_DB", "router_db"),
		Profile:                getEnvOrDefault("PROFILE", "local"),
		TimeExpired:            getTimeOrDefault("APP_ROUTER_MANAGER_SENT_EXPIRED", "1m"),
		CheckExpiredInterval:   getTimeOrDefault("APP_ROUTER_MANAGER_CHECK_EXPIRED_INTERVAL", "30s"),
		AlloyUrl:               getEnvOrDefault("FAILED_PROCESSOR_MANAGEMENT_TRACING_OTLP_ENDPOINT_GRPC", "http://localhost:4317"),
		RedisUrl:               getEnvOrDefault("APP_ROUTER_MANAGER_REDIS_HOST", "localhost"),
		RedisPort:              getEnvOrDefault("APP_ROUTER_MANAGER_REDIS_PORT", "6379"),
		RedisPassword:          getEnvOrDefault("APP_ROUTER_MANAGER_REDIS_PASSWORD", "redis_pass"),
		DbMaxConns:             getEnvOrDefault("APP_ROUTER_MANAGER_MAXCONNS", "50"),
		DbMinConns:             getEnvOrDefault("APP_ROUTER_MANAGER_MINCONNS", "10"),
		DbMaxConnsLifeTime:     getTimeOrDefault("APP_ROUTER_MANAGER_MAXCONN_LIFETIME", "10s"),
		DbMaxConnsIdleTime:     getTimeOrDefault("APP_ROUTER_MANAGER_MAXCONN_IDLETIME", "5s"),
		DbHealthCheckPeriod:    getTimeOrDefault("APP_ROUTER_MANAGER_HEALTHCHECK_PERIOD", "1s"),
		DbStatementTimeout:     getEnvOrDefault("APP_ROUTER_MANAGER_STATEMENT_TIMEOUT", "30000"),
		DbIdleTransSessTimeout: getEnvOrDefault("APP_ROUTER_MANAGER_IDLE_IN_TRANSACTION_SESSION_TIMEOUT", "10000"),
		DbConnectionTimeout:    getEnvOrDefault("APP_ROUTER_MANAGER_CONNECTION_TIMEOUT", "5"),
		MetricsPort:            getEnvOrDefault("APP_ROUTER_MANAGER_METRICS_PORT", "9091"),
		GRPCPort:               getEnvOrDefault("APP_ROUTER_MANAGER_GRPC_PORT", "9092"),
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
			log.Error("неверный формат TimeExpired в ENV APP_ROUTER_MANAGER_SENT_EXPIRED")
		}
		return duration
	}
	duration, err := time.ParseDuration(defaultValue)
	if err != nil {
		log.Error("неверный формат TimeExpired в defaultValue, ENV APP_ROUTER_MANAGER_SENT_EXPIRED отсутствует")
	}
	return duration
}
