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
		log.Error("Ошибка подключения к БД", slog.String("error", err.Error()))
		return fmt.Errorf("Ошибка подключения к БД: %w", err)
	}

	err = m.Up()
	if err != nil && err != migrate.ErrNoChange {
		log.Error("ошибка выполнения миграций", slog.String("error", err.Error()))
		return fmt.Errorf("ошибка выполнения миграций: %w", err)
	}
	log.Info("миграции применены к базе", slog.String("databaseUrl", dbURL))

	return nil
}
