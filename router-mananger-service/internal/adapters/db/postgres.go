package db

import (
	"context"
	"encoding/json"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"router-mananger-service/internal/domain"
	"time"
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

func (r *PostgresCommandRepository) GetByIdAndStatuses(uuid uuid.UUID, statuses []string) ([]domain.Command, error) {

	if len(statuses) == 0 {
		return nil, nil
	}

	query := `
		SELECT id, router_id, command_type, payload, status, created_at
		FROM commands
		WHERE router_id = $1 AND status = ANY($2)
	`

	rows, err := r.pool.Query(context.Background(), query, uuid, statuses)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var commands []domain.Command
	for rows.Next() {
		var cmd domain.Command
		var payloadBytes []byte
		if err := rows.Scan(&cmd.ID, &cmd.RouterID, &cmd.CommandType, &payloadBytes, &cmd.Status, &cmd.CreatedAt); err != nil {
			return nil, err
		}

		if err := json.Unmarshal(payloadBytes, &cmd.Payload); err != nil {
			return nil, err
		}

		commands = append(commands, cmd)
	}

	return commands, nil
}

func (r *PostgresCommandRepository) UpdateStatusById(routerId uuid.UUID) error {
	_, err := r.pool.Exec(
		context.Background(),
		"UPDATE commands SET status=$1, sent_at=$2 WHERE router_id=$3",
		"SENT", time.Now(), routerId,
	)
	return err
}

func (r *PostgresCommandRepository) UpdateStatusToAcked(routerID, commandID uuid.UUID) error {
	_, err := r.pool.Exec(
		context.Background(),
		`UPDATE commands
		 SET status = $1,
		     acked_at = $2
		 WHERE id = $3 AND router_id = $4`,
		"ACKED", time.Now(), commandID, routerID,
	)
	return err
}
