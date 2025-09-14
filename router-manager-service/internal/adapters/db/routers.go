package db

import (
	"context"
	"fmt"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/prometheus/client_golang/prometheus"
	"router-manager-service/internal/domain"
	"router-manager-service/internal/metrics"
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
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("router_save"))
	defer timer.ObserveDuration()

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
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("router_save_all"))
	defer timer.ObserveDuration()

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

func (r *PostgresRouterRepository) GetBySerial(serial string) (domain.Router, error) {
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("router_get_by_serial"))
	defer timer.ObserveDuration()

	var router domain.Router

	query := `
		SELECT id, serial_number, ip_address, last_seen_at, created_at
		FROM routers
		WHERE serial_number = $1
		`

	row := r.pool.QueryRow(context.Background(), query, serial)

	var lastSeen *time.Time
	err := row.Scan(&router.ID, &router.SerialNumber, &router.IpAddress, &lastSeen, &router.CreatedAt)
	if err != nil {
		return domain.Router{}, err
	}
	router.LastSeenAt = lastSeen

	return router, nil
}

func (r *PostgresRouterRepository) GetAllIds() ([]uuid.UUID, error) {
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("router_get_all_ids"))
	defer timer.ObserveDuration()

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

func (r *PostgresRouterRepository) GetAllRouters() ([]domain.Router, error) {
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("router_get_all_routers"))
	defer timer.ObserveDuration()

	query := `
			SELECT id, serial_number, ip_address, last_seen_at, created_at 
			FROM routers
			`

	rows, err := r.pool.Query(context.Background(), query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var routers []domain.Router
	for rows.Next() {
		var r domain.Router
		var ipAddress *string
		var lastSeenAt *time.Time

		if err := rows.Scan(&r.ID, &r.SerialNumber, &ipAddress, &lastSeenAt, &r.CreatedAt); err != nil {
			return nil, err
		}

		r.IpAddress = ipAddress
		r.LastSeenAt = lastSeenAt

		routers = append(routers, r)
	}

	if err := rows.Err(); err != nil {
		return nil, err
	}

	return routers, nil
}

func (r *PostgresRouterRepository) GetAllRoutersBySerials(serials []string) ([]domain.Router, error) {
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("router_get_all_routers_by_serial"))
	defer timer.ObserveDuration()

	query := `SELECT id, serial_number, ip_address, last_seen_at, created_at 
			 FROM routers
			 WHERE serial_number = ANY(&1)
			 `

	rows, err := r.pool.Query(context.Background(), query, serials)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var routers []domain.Router
	for rows.Next() {
		var router domain.Router
		var ipAddress *string
		var lastSeenAt *time.Time

		if err := rows.Scan(&router.ID, &router.SerialNumber, &ipAddress, &lastSeenAt, &router.CreatedAt); err != nil {
			return nil, err
		}

		router.IpAddress = ipAddress
		router.LastSeenAt = lastSeenAt

		routers = append(routers, router)
	}

	if err := rows.Err(); err != nil {
		return nil, err
	}

	return routers, nil
}

func (r *PostgresRouterRepository) UpdateSeenAt(serials []string) error {
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("router_update_seen_at"))
	defer timer.ObserveDuration()

	if len(serials) == 0 {
		return nil
	}

	placeholders := make([]string, len(serials))
	args := make([]any, len(serials))
	for i, id := range serials {
		placeholders[i] = fmt.Sprintf("$%d", i+1)
		args[i] = id
	}

	query := fmt.Sprintf(`
		UPDATE routers
		SET last_seen_at = NOW()
		WHERE serial_number IN (%s)
	`, strings.Join(placeholders, ","))

	_, err := r.pool.Exec(context.Background(), query, args...)
	return err
}
