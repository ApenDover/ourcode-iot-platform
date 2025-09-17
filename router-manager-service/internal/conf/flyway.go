package conf

import (
	"context"
	"errors"
	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"log/slog"
	"router-manager-service/internal/conf/util"
	"strings"
)

func RunMigrations(dbURL string, migrationSource string) {
	log := util.GetLogger(context.Background())

	if !strings.HasPrefix(migrationSource, "file://") {
		migrationSource = "file://" + migrationSource
	}

	migrations, errCreateConnection := migrate.New(
		migrationSource,
		dbURL,
	)
	if errCreateConnection != nil {
		log.Error("Ошибка подключения к БД", slog.String("error", errCreateConnection.Error()))
	}

	errRunMigrations := migrations.Up()
	if errRunMigrations != nil && !errors.Is(errRunMigrations, migrate.ErrNoChange) {
		log.Error("ошибка выполнения миграций", slog.String("error", errRunMigrations.Error()))
	}
	log.Info("миграции применены к базе", slog.String("databaseUrl", dbURL))
}
