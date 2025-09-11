package db

import (
	"context"
	"fmt"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"router-mananger-service/internal/domain"
	"strings"
	"time"
)

type PostgresRoutersRepository struct {
	pool *pgxpool.Pool
}

func NewPostgresRoutersRepository(pool *pgxpool.Pool) *PostgresRoutersRepository {
	return &PostgresRoutersRepository{pool: pool}
}

func (r *PostgresRoutersRepository) Save(cmd domain.Router) error {

	_, err := r.pool.Exec(
		context.Background(),
		`INSERT INTO routers (id, serial_number, ip_address, last_seen_at, created_at) 
         VALUES ($1, $2, $3, $4, $5)`,
		cmd.ID, cmd.SerialNumber, cmd.IpAddress, cmd.LastSeenAt, cmd.CreatedAt,
	)
	return err
}

func (r *PostgresRoutersRepository) SaveAll(cmds []domain.Router) error {
	if len(cmds) == 0 {
		return nil
	}

	var values []interface{}
	var placeholders []string

	for i, cmd := range cmds {

		values = append(values, cmd.ID, cmd.SerialNumber, cmd.IpAddress, cmd.LastSeenAt, cmd.CreatedAt)

		base := i*6 + 1
		placeholders = append(placeholders, fmt.Sprintf("($%d,$%d,$%d,$%d,$%d)", base, base+1, base+2, base+3, base+4))
	}

	query := fmt.Sprintf(`
        INSERT INTO commands (id, serial_number, ip_address, last_seen_at, created_at)
        VALUES %s
    `, strings.Join(placeholders, ","))

	_, err := r.pool.Exec(context.Background(), query, values...)
	return err
}

func (r *PostgresRoutersRepository) GetById(id uuid.UUID) (domain.Router, error) {
	var router domain.Router

	query := `
		SELECT id, serial_number, ip_address, last_seen_at, created_at
		FROM routers
		WHERE id = $1
	`

	row := r.pool.QueryRow(context.Background(), query, id)

	var lastSeen *time.Time
	err := row.Scan(&router.ID, &router.SerialNumber, &router.IpAddress, &lastSeen, &router.CreatedAt)
	if err != nil {
		return domain.Router{}, err
	}
	router.LastSeenAt = lastSeen

	return router, nil
}
