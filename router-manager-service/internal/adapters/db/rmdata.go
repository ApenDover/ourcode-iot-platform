package db

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"router-manager-service/internal/conf/util"
	"router-manager-service/internal/core/domain"
	"router-manager-service/internal/ports"
	"strings"
	"time"
)

type PostgresDataAdapter struct {
	pool *pgxpool.Pool
}

func NewPostgresCommonAdapter(pool *pgxpool.Pool) ports.DataPort {
	return &PostgresDataAdapter{pool: pool}
}

var _ ports.DataPort = (*PostgresDataAdapter)(nil)

func (a *PostgresDataAdapter) CreateCommands(ctx context.Context, cmds []domain.Command) error {
	fmt.Sprintf("Вставка партии из %d команд", len(cmds))
	if len(cmds) == 0 {
		return nil
	}

	const maxBatchSize = 1000
	var err error

	for i := 0; i < len(cmds); i += maxBatchSize {
		end := i + maxBatchSize
		if end > len(cmds) {
			end = len(cmds)
		}

		batch := cmds[i:end]
		if err = a.insertBatch(ctx, batch); err != nil {
			return fmt.Errorf("batch insert failed at offset %d: %w", i, err)
		}
	}

	return nil
}

func (a *PostgresDataAdapter) insertBatch(ctx context.Context, batch []domain.Command) error {
	fmt.Sprintf("Вставка партии из %d команд", len(batch))
	if len(batch) == 0 {
		return nil
	}

	var values []interface{}
	var placeholders []string

	for i, cmd := range batch {
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

	_, err := a.pool.Exec(context.Background(), query, values...)
	return err
}

func (a *PostgresDataAdapter) PollCommands(ctx context.Context, routerSerial string) ([]domain.Command, error) {
	tx, err := a.pool.BeginTx(ctx, pgx.TxOptions{})
	if err != nil {
		return nil, err
	}
	defer func() {
		_ = tx.Rollback(ctx)
	}()

	rows, err := tx.Query(ctx, `
        WITH updated_commands AS (
            UPDATE commands 
            SET status = $1, sent_at = $2
            WHERE router_id = (
                SELECT id FROM routers WHERE serial_number = $3
            ) 
            AND status = $4
            RETURNING 
                id,
                router_id,
                command_type,
                payload,
                status,
                created_at,
                sent_at,
                acked_at
        )
        SELECT * FROM updated_commands
    `, domain.CommandStatusSent, time.Now(), routerSerial, domain.CommandStatusPending)

	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var commands []domain.Command

	for rows.Next() {
		var cmd domain.Command
		var payloadBytes []byte
		if err := rows.Scan(
			&cmd.ID,
			&cmd.RouterID,
			&cmd.CommandType,
			&payloadBytes,
			&cmd.Status,
			&cmd.CreatedAt,
			&cmd.SentAt,
			&cmd.AckedAt,
		); err != nil {
			return nil, err
		}

		if err := json.Unmarshal(payloadBytes, &cmd.Payload); err != nil {
			return nil, err
		}

		commands = append(commands, cmd)
	}

	if err := rows.Err(); err != nil {
		return nil, err
	}

	if _, err := tx.Exec(ctx, `
        UPDATE routers
        SET last_seen_at = $1
        WHERE serial_number = $2
    `, time.Now(), routerSerial); err != nil {
		return nil, err
	}

	if err := tx.Commit(ctx); err != nil {
		return nil, err
	}

	util.GetLogger(ctx).Info("Атомарная выборка команд с обновлением статуса",
		"router_serial", routerSerial,
		"command_count", len(commands),
		"status", "sent")

	return commands, nil
}

func (a *PostgresDataAdapter) AckCommand(ctx context.Context, routerSerial string, commandID uuid.UUID) error {
	tx, errPool := a.pool.BeginTx(ctx, pgx.TxOptions{})
	if errPool != nil {
		return errPool
	}
	defer tx.Rollback(ctx)

	tag, errExec := tx.Exec(ctx, `
		UPDATE commands c
		SET status=$1, acked_at=$2
		FROM routers r
		WHERE c.id=$3 AND c.router_id=r.id AND r.serial_number=$4 AND c.status=$5
	`, domain.CommandStatusAcked, time.Now(), commandID, routerSerial, domain.CommandStatusSent)
	if errExec != nil {
		return errExec
	}
	fmt.Sprintf("RowsAffected: %d", tag.RowsAffected())
	if tag.RowsAffected() == 0 {
		return fmt.Errorf("ack failed: no matching command with id=%s and router_serial=%s in SENT status", commandID, routerSerial)
	}

	if _, err := tx.Exec(ctx, `
		UPDATE routers
		SET last_seen_at=$1
		WHERE serial_number=$2
	`, time.Now(), routerSerial); err != nil {
		return err
	}

	return tx.Commit(ctx)
}

func (a *PostgresDataAdapter) MarkExpiredAsError(ctx context.Context, timeout time.Duration) error {
	tx, err := a.pool.BeginTx(ctx, pgx.TxOptions{})
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)

	query := `
		UPDATE commands
		SET status = $1
		WHERE status = $2
		  AND sent_at < $3
	`

	deadline := time.Now().Add(-timeout)

	_, err = tx.Exec(ctx, query, domain.CommandStatusError, domain.CommandStatusSent, deadline)
	if err != nil {
		return err
	}

	return tx.Commit(ctx)
}
