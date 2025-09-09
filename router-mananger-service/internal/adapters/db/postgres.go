package db

import (
	"context"
	"encoding/json"

	"github.com/jackc/pgx/v5/pgxpool"
	"router-mananger-service/internal/domain"
)

type PostgresCommandRepository struct {
	pool *pgxpool.Pool
}

func NewPostgresCommandRepository(pool *pgxpool.Pool) *PostgresCommandRepository {
	return &PostgresCommandRepository{pool: pool}
}

func (r *PostgresCommandRepository) Save(cmd domain.Command) error {
	payloadBytes, err := json.Marshal(cmd.Payload)
	if err != nil {
		return err
	}

	_, err = r.pool.Exec(
		context.Background(),
		`INSERT INTO commands (id, router_id, command_type, payload, status, created_at) 
         VALUES ($1, $2, $3, $4, $5, $6)`,
		cmd.ID, cmd.RouterID, cmd.CommandType, payloadBytes, cmd.Status, cmd.CreatedAt,
	)
	return err
}
