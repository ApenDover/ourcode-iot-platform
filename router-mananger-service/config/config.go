package config

import (
	"os"
	"router-mananger-service/internal/util"
	"time"
)

type Config struct {
	DBHost      string
	DBPort      string
	DBUser      string
	DBPassword  string
	DBName      string
	Profile     string
	TimeExpired time.Duration
}

func LoadConfig() *Config {
	return &Config{
		DBHost:      getEnvOrDefault("APP_ROUTER_MANAGER_DATASOURCE_HOST", "localhost"),
		DBPort:      getEnvOrDefault("APP_ROUTER_MANAGER_DATASOURCE_PORT", "5439"),
		DBUser:      getEnvOrDefault("APP_ROUTER_MANAGER_DATASOURCE_USERNAME", "user"),
		DBPassword:  getEnvOrDefault("APP_ROUTER_MANAGER_DATASOURCE_PASSWORD", "password"),
		DBName:      getEnvOrDefault("APP_ROUTER_MANAGER_DATASOURCE_DB", "router_db"),
		Profile:     getEnvOrDefault("PROFILE", "local"),
		TimeExpired: getTimeOrDefault("SENT_EXPIRED", "5m"),
	}
}

func getEnvOrDefault(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

func getTimeOrDefault(key, defaultValue string) time.Duration {
	log := util.GetLogger()
	if value := os.Getenv(key); value != "" {
		duration, err := time.ParseDuration(os.Getenv(key))
		if err != nil {
			log.Error("неверный формат TimeExpired в ENV SENT_EXPIRED")
		}
		return duration
	}
	duration, err := time.ParseDuration(os.Getenv(defaultValue))
	if err != nil {
		log.Error("неверный формат TimeExpired в defaultValue, ENV SENT_EXPIRED отсутствует")
	}
	return duration
}
