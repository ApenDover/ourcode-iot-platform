package util

import (
	"path/filepath"
	"runtime"
)

func MigrationsPath(migrationPath string) string {
	_, b, _, _ := runtime.Caller(0)
	basePath := filepath.Dir(b)
	path := filepath.Join(basePath, migrationPath)
	return "file://" + path
}
