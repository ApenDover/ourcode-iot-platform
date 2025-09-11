package db

import (
	"fmt"
	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"log/slog"
	"router-mananger-service/internal/util"
)

func RunMigrations(dbURL string, migrationSource string) error {
	log := util.GetLogger()
	m, err := migrate.New(
		migrationSource,
		dbURL,
	)
	if err != nil {
		return fmt.Errorf("failed to create migrate instance: %w", err)
	}

	err = m.Up()
	if err != nil && err != migrate.ErrNoChange {
		return fmt.Errorf("failed to apply migrations: %w", err)
	}
	log.Info("миграции применены к базе", slog.String("databaseUrl", dbURL))

	return nil
}
