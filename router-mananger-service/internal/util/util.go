package util

import (
	"path/filepath"
	"runtime"
)

func MigrationsPath() string {
	_, b, _, _ := runtime.Caller(0)
	basePath := filepath.Dir(b)
	path := filepath.Join(basePath, "../../db/migrations")
	return "file://" + path
}
