package conf

import (
	"fmt"
	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"log/slog"
	"router-mananger-service/internal/util"
	"strings"
)

func RunMigrations(dbURL string, migrationSource string) error {
	log := util.GetLogger()

	if !strings.HasPrefix(migrationSource, "file://") {
		migrationSource = "file://" + migrationSource
	}

	m, err := migrate.New(
		migrationSource,
		dbURL,
	)
	if err != nil {
		log.Error("failed to create migrate instance", slog.String("error", err.Error()))
		return fmt.Errorf("failed to create migrate instance: %w", err)
	}

	err = m.Up()
	if err != nil && err != migrate.ErrNoChange {
		log.Error("failed to apply migrations", slog.String("error", err.Error()))
		return fmt.Errorf("failed to apply migrations: %w", err)
	}
	log.Info("миграции применены к базе", slog.String("databaseUrl", dbURL))

	return nil
}
