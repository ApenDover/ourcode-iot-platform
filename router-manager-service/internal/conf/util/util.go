package util

import (
	"context"
	"path/filepath"
	"runtime"
	"strconv"
)

func MigrationsPath(migrationPath string) string {
	_, b, _, _ := runtime.Caller(0)
	basePath := filepath.Dir(b)
	path := filepath.Join(basePath, migrationPath)
	return "file://" + path
}

func StringToInt(str string) int32 {
	res, err := strconv.Atoi(str)
	if err != nil {
		GetLogger(context.Background()).Error("error converting string to int", err)
		return 1
	}
	return int32(res)
}
