package logutil

import (
	"os"
)

type LogConfig struct {
	Profile string
	Level   string
}

func LoadConfig() *LogConfig {
	return &LogConfig{
		Profile: getEnvOrDefault("PROFILE", "local"),
		Level:   getEnvOrDefault("LOG_LEVEL", "INFO"),
	}
}

func getEnvOrDefault(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}
