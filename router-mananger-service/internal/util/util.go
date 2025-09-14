package util

import (
	"github.com/google/uuid"
	"path/filepath"
	"runtime"
	"strings"
)

func MigrationsPath() string {
	_, b, _, _ := runtime.Caller(0)
	basePath := filepath.Dir(b)
	path := filepath.Join(basePath, "../../db/migrations")
	return "file://" + path
}

func UUIDsToStrings(uuids []uuid.UUID) string {
	res := make([]string, len(uuids))
	for i, u := range uuids {
		res[i] = u.String()
	}
	return strings.Join(res, ", ")
}
