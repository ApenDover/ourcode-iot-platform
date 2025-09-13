package db

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"router-mananger-service/internal/domain"
	"strings"
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

func (r *PostgresCommandRepository) SaveAll(cmds []domain.Command) error {
	if len(cmds) == 0 {
		return nil
	}

	var values []interface{}
	var placeholders []string

	for i, cmd := range cmds {
		payloadBytes, err := json.Marshal(cmd.Payload)
		if err != nil {
			return err
		}

		values = append(values, cmd.ID, cmd.RouterID, cmd.CommandType, payloadBytes, cmd.Status, cmd.CreatedAt)

		base := i*6 + 1
		placeholders = append(placeholders, fmt.Sprintf("($%d,$%d,$%d,$%d,$%d,$%d)", base, base+1, base+2, base+3, base+4, base+5))
	}

	query := fmt.Sprintf(`
        INSERT INTO commands (id, router_id, command_type, payload, status, created_at)
        VALUES %s
    `, strings.Join(placeholders, ","))

	_, err := r.pool.Exec(context.Background(), query, values...)
	return err
}

func (r *PostgresCommandRepository) GetByIdAndStatuses(uuid uuid.UUID, statuses []domain.CommandStatus) ([]domain.Command, error) {

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

func (r *PostgresCommandRepository) GetAllByRouterId(routerId uuid.UUID) ([]domain.Command, error) {

	query := `
		SELECT id, router_id, command_type, payload, status, sent_at, acked_at, created_at
		FROM commands
		WHERE router_id = $1
	`

	rows, err := r.pool.Query(context.Background(), query, routerId)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var commands []domain.Command
	for rows.Next() {
		var cmd domain.Command
		var payloadBytes []byte
		if err := rows.Scan(&cmd.ID, &cmd.RouterID, &cmd.CommandType, &payloadBytes, &cmd.Status, &cmd.SentAt, &cmd.AckedAt, &cmd.CreatedAt); err != nil {
			return nil, err
		}

		if err := json.Unmarshal(payloadBytes, &cmd.Payload); err != nil {
			return nil, err
		}

		commands = append(commands, cmd)
	}

	return commands, nil
}

func (r *PostgresCommandRepository) SetSentStatusForPendingByRouterId(routerId uuid.UUID) error {
	_, err := r.pool.Exec(
		context.Background(),
		"UPDATE commands SET status=$1, sent_at=$2 WHERE router_id=$3 AND status=$4",
		domain.CommandStatusSent, time.Now(), routerId, domain.CommandStatusPending,
	)
	return err
}

func (r *PostgresCommandRepository) SetSentStatusForPendingByCommandIds(cmds []domain.Command) error {
	if len(cmds) == 0 {
		return nil
	}

	ctx := context.Background()
	batch := &pgx.Batch{}

	// Добавляем в батч обновления для каждой команды
	for _, cmd := range cmds {
		batch.Queue(
			`UPDATE commands
			 SET status=$1, sent_at=$2
			 WHERE id=$3 AND status=domain.CommandStatusPending`,
			domain.CommandStatusSent, time.Now(), cmd.ID,
		)
	}

	// Отправляем батч и закрываем его
	br := r.pool.SendBatch(ctx, batch)
	defer br.Close()

	// Проходим по результатам каждого запроса, чтобы убедиться, что ошибки не было
	for range cmds {
		_, err := br.Exec()
		if err != nil {
			return err
		}
	}
	return nil
}

func (r *PostgresCommandRepository) UpdateStatusToAcked(routerID, commandID uuid.UUID) error {
	_, err := r.pool.Exec(
		context.Background(),
		`UPDATE commands
		 SET status = $1,acked_at = $2
		 WHERE id = $3 AND router_id = $4 AND status='SENT'`,
		domain.CommandStatusAcked, time.Now(), commandID, routerID,
	)
	return err
}

func (r *PostgresCommandRepository) FindAllIDs() ([]uuid.UUID, error) {
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
