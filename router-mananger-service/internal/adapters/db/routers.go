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

type PostgresRouterRepository struct {
	pool *pgxpool.Pool
}

func NewPostgresRouterRepository(pool *pgxpool.Pool) *PostgresRouterRepository {
	return &PostgresRouterRepository{pool: pool}
}

func (r *PostgresRouterRepository) Save(cmd domain.Router) error {
	_, err := r.pool.Exec(
		context.Background(), `
		INSERT INTO routers (id, serial_number, ip_address, last_seen_at, created_at) 
        VALUES ($1, $2, $3, $4, $5) 
        ON CONFLICT (serial_number) DO NOTHING
        `, cmd.ID, cmd.SerialNumber, cmd.IpAddress, cmd.LastSeenAt, cmd.CreatedAt,
	)
	return err
}

func (r *PostgresRouterRepository) SaveAll(cmds []domain.Router) error {
	if len(cmds) == 0 {
		return nil
	}

	var values []interface{}
	var placeholders []string

	for i, cmd := range cmds {
		values = append(values, cmd.ID, cmd.SerialNumber, cmd.IpAddress, cmd.LastSeenAt, cmd.CreatedAt)
		base := i*5 + 1
		placeholders = append(placeholders, fmt.Sprintf("($%d,$%d,$%d,$%d,$%d)", base, base+1, base+2, base+3, base+4))
	}

	query := fmt.Sprintf(`
			INSERT INTO routers (id, serial_number, ip_address, last_seen_at, created_at)
        	VALUES %s
        	ON CONFLICT (serial_number) 
        	DO NOTHING
        	`, strings.Join(placeholders, ","))

	_, err := r.pool.Exec(context.Background(), query, values...)
	return err
}

func (r *PostgresRouterRepository) GetById(id uuid.UUID) (domain.Router, error) {
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

func (r *PostgresRouterRepository) GetAllIds() ([]uuid.UUID, error) {
	rows, err := r.pool.Query(context.Background(), "SELECT id FROM routers")
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var ids []uuid.UUID
	for rows.Next() {
		var id uuid.UUID
		if err := rows.Scan(&id); err != nil {
			return nil, err
		}
		ids = append(ids, id)
	}
	return ids, nil
}

func (r *PostgresRouterRepository) UpdateSeenAt(routerIds []uuid.UUID) error {
	if len(routerIds) == 0 {
		return nil
	}

	placeholders := make([]string, len(routerIds))
	args := make([]any, len(routerIds))
	for i, id := range routerIds {
		placeholders[i] = fmt.Sprintf("$%d", i+1)
		args[i] = id
	}

	query := fmt.Sprintf(`
		UPDATE routers
		SET last_seen_at = NOW()
		WHERE id IN (%s)
	`, strings.Join(placeholders, ","))

	_, err := r.pool.Exec(context.Background(), query, args...)
	return err
}
