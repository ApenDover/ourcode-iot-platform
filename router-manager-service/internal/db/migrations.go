package db

import (
	"embed"
	"errors"
	"fmt"
	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	"github.com/golang-migrate/migrate/v4/source/iofs"
)

//go:embed migrations/*.sql
var migrationsFS embed.FS

type Migrator struct {
	migrator *migrate.Migrate
}

func NewMigrator(databaseURL string) (*Migrator, error) {
	driver, err := iofs.New(migrationsFS, "migrations")
	if err != nil {
		return nil, fmt.Errorf("ошибка создания iofs драйвера: %w", err)
	}

	migrator, err := migrate.NewWithSourceInstance("iofs", driver, databaseURL)
	if err != nil {
		return nil, fmt.Errorf("Ошибка создания NewMigrator: %w", err)
	}

	return &Migrator{migrator: migrator}, nil
}

func (m *Migrator) Up() error {
	if err := m.migrator.Up(); err != nil && !errors.Is(err, migrate.ErrNoChange) {
		return fmt.Errorf("Ошибка при попытке применить миграции: %w", err)
	}
	return nil
}

func (m *Migrator) Down() error {
	if err := m.migrator.Down(); err != nil && !errors.Is(err, migrate.ErrNoChange) {
		return fmt.Errorf("Ошибка отката миграций: %w", err)
	}
	return nil
}

func (m *Migrator) Close() {
	if m.migrator != nil {
		m.migrator.Close()
	}
}
