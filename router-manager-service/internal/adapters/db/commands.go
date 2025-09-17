package db

import (
	"context"
	"fmt"
	"github.com/google/uuid"
	"router-manager-service/internal/core/domain"
	"router-manager-service/internal/ports"

	"github.com/jackc/pgx/v5/pgxpool"
)

type PostgresCommandsRepository struct {
	pool *pgxpool.Pool
}

func NewPostgresCommandsAdapter(pool *pgxpool.Pool) *PostgresCommandsRepository {
	return &PostgresCommandsRepository{pool: pool}
}

type Command struct {
	ID   int64
	Name string
}

var _ ports.CommandPort = (*PostgresCommandsRepository)(nil)

func (r *PostgresCommandsRepository) GetAll(ctx context.Context) ([]domain.Command, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT id, router_id, command_type, payload, status, sent_at, acked_at, created_at
		FROM commands
		ORDER BY created_at DESC
	`)
	if err != nil {
		return nil, fmt.Errorf("query failed: %w", err)
	}
	defer rows.Close()

	var commands []domain.Command
	for rows.Next() {
		var c domain.Command
		if err := rows.Scan(
			&c.ID,
			&c.RouterID,
			&c.CommandType,
			&c.Payload,
			&c.Status,
			&c.SentAt,
			&c.AckedAt,
			&c.CreatedAt,
		); err != nil {
			return nil, fmt.Errorf("scan failed: %w", err)
		}
		commands = append(commands, c)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("rows iteration error: %w", err)
	}

	return commands, nil
}

func (r *PostgresCommandsRepository) GetAllByRouterId(ctx context.Context, routerId uuid.UUID) ([]domain.Command, error) {

	rows, err := r.pool.Query(ctx, `
		SELECT id, router_id, command_type, payload, status, sent_at, acked_at, created_at
		FROM commands
		WHERE router_id = $1
	`, routerId)
	if err != nil {
		return nil, fmt.Errorf("query failed: %w", err)
	}
	defer rows.Close()

	var commands []domain.Command
	for rows.Next() {
		var c domain.Command
		if err := rows.Scan(
			&c.ID,
			&c.RouterID,
			&c.CommandType,
			&c.Payload,
			&c.Status,
			&c.SentAt,
			&c.AckedAt,
			&c.CreatedAt,
		); err != nil {
			return nil, fmt.Errorf("scan failed: %w", err)
		}
		commands = append(commands, c)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("rows iteration error: %w", err)
	}

	return commands, nil
}
