package util

import (
	"fmt"
	"path/filepath"
	"router-mananger-service/config"
	"runtime"
)

func MigrationsPath() string {
	_, b, _, _ := runtime.Caller(0)
	basePath := filepath.Dir(b)
	path := filepath.Join(basePath, "../../db/migrations")
	return "file://" + path
}

func DatabasePath() string {
	c := config.LoadConfig()
	return fmt.Sprintf(
		"postgres://%s:%s@%s:%s/%s?sslmode=disable",
		c.DBUser,
		c.DBPassword,
		c.DBHost,
		c.DBPort,
		c.DBName,
	)
}
