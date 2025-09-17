package db

import (
	"context"
	"fmt"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/prometheus/client_golang/prometheus"
	"router-manager-service/internal/core/domain"
	"router-manager-service/internal/metrics"
	"router-manager-service/internal/ports"
	"strings"
	"time"
)

type PostgresRouterRepository struct {
	pool *pgxpool.Pool
}

func NewPostgresRouterAdapter(pool *pgxpool.Pool) *PostgresRouterRepository {
	return &PostgresRouterRepository{pool: pool}
}

var _ ports.RouterPort = (*PostgresRouterRepository)(nil)

func (r *PostgresRouterRepository) EnsureRoutersExist(ctx context.Context, serials []string) error {
	if len(serials) == 0 {
		return nil
	}

	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("routers_ensure_exist"))
	defer timer.ObserveDuration()

	tx, err := r.pool.BeginTx(ctx, pgx.TxOptions{})
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)

	now := time.Now()
	for _, serial := range serials {
		_, err := tx.Exec(ctx, `
			INSERT INTO routers (id, serial_number, created_at)
			VALUES ($1, $2, $3)
			ON CONFLICT (serial_number) DO NOTHING
		`, uuid.New(), serial, now)
		if err != nil {
			return err
		}
	}

	return tx.Commit(ctx)
}

func (r *PostgresRouterRepository) GetBySerial(ctx context.Context, serial string) (*domain.Router, error) {
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("router_get_by_serial"))
	defer timer.ObserveDuration()

	var router domain.Router
	query := `SELECT id, serial_number, ip_address, last_seen_at, created_at FROM routers WHERE serial_number=$1`

	row := r.pool.QueryRow(ctx, query, serial)
	var lastSeen *time.Time
	if err := row.Scan(&router.ID, &router.SerialNumber, &router.IpAddress, &lastSeen, &router.CreatedAt); err != nil {
		return nil, err
	}
	router.LastSeenAt = lastSeen
	return &router, nil
}

func (r *PostgresRouterRepository) GetAllRoutersBySerials(ctx context.Context, serials []string) ([]*domain.Router, error) {
	if len(serials) == 0 {
		return nil, nil
	}

	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("router_get_all_by_serials"))
	defer timer.ObserveDuration()

	query := `SELECT id, serial_number, ip_address, last_seen_at, created_at FROM routers WHERE serial_number = ANY($1)`
	rows, err := r.pool.Query(ctx, query, serials)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var routers []*domain.Router
	for rows.Next() {
		var r domain.Router
		var ipAddress *string
		var lastSeenAt *time.Time
		if err := rows.Scan(&r.ID, &r.SerialNumber, &ipAddress, &lastSeenAt, &r.CreatedAt); err != nil {
			return nil, err
		}
		r.IpAddress = ipAddress
		r.LastSeenAt = lastSeenAt
		routers = append(routers, &r)
	}
	return routers, nil
}

func (r *PostgresRouterRepository) UpdateSeenAt(ctx context.Context, serials []string) error {
	if len(serials) == 0 {
		return nil
	}

	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("router_update_seen_at"))
	defer timer.ObserveDuration()

	placeholders := make([]string, len(serials))
	args := make([]any, len(serials))
	for i, s := range serials {
		placeholders[i] = fmt.Sprintf("$%d", i+1)
		args[i] = s
	}

	query := fmt.Sprintf(`
		UPDATE routers SET last_seen_at = NOW() WHERE serial_number IN (%s)
	`, strings.Join(placeholders, ","))

	_, err := r.pool.Exec(ctx, query, args...)
	return err
}

func (r *PostgresRouterRepository) GetAllRouters(ctx context.Context) ([]*domain.Router, error) {
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("router_get_all_routers"))
	defer timer.ObserveDuration()

	query := `SELECT id, serial_number, ip_address, last_seen_at, created_at FROM routers`
	rows, err := r.pool.Query(ctx, query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var routers []*domain.Router
	for rows.Next() {
		var r domain.Router
		var ipAddress *string
		var lastSeenAt *time.Time
		if err := rows.Scan(&r.ID, &r.SerialNumber, &ipAddress, &lastSeenAt, &r.CreatedAt); err != nil {
			return nil, err
		}
		r.IpAddress = ipAddress
		r.LastSeenAt = lastSeenAt
		routers = append(routers, &r)
	}
	return routers, nil
}

func (r *PostgresRouterRepository) Save(ctx context.Context, router domain.Router) error {
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("router_save"))
	defer timer.ObserveDuration()

	_, err := r.pool.Exec(ctx, `
		INSERT INTO routers (id, serial_number, ip_address, last_seen_at, created_at)
		VALUES ($1, $2, $3, $4, $5)
		ON CONFLICT (serial_number) DO NOTHING
	`, router.ID, router.SerialNumber, router.IpAddress, router.LastSeenAt, router.CreatedAt)

	return err
}
