package conf

import (
	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"log/slog"
	"router-manager-service/internal/util"
	"strings"
)

func RunMigrations(dbURL string, migrationSource string) {
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
	}

	err = m.Up()
	if err != nil && err != migrate.ErrNoChange {
		log.Error("ошибка выполнения миграций", slog.String("error", err.Error()))
	}
	log.Info("миграции применены к базе", slog.String("databaseUrl", dbURL))
}
