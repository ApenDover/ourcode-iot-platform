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
	if len(cmds) == 0 {
		return nil
	}
	const copyThreshold = 50
	if len(cmds) > copyThreshold {
		return a.createCommandsWithCOPY(ctx, cmds)
	}
	return a.createCommandsWithMultiInsert(ctx, cmds)
}

func (a *PostgresDataAdapter) createCommandsWithCOPY(ctx context.Context, cmds []domain.Command) error {
	const maxBatchSize = 1000

	for i := 0; i < len(cmds); i += maxBatchSize {
		end := i + maxBatchSize
		if end > len(cmds) {
			end = len(cmds)
		}

		batch := cmds[i:end]
		copySource := pgx.CopyFromSlice(len(batch), func(i int) ([]interface{}, error) {
			cmd := batch[i]

			payloadBytes, err := json.Marshal(cmd.Payload)
			if err != nil {
				return nil, err
			}

			return []interface{}{
				cmd.ID,
				cmd.RouterID,
				cmd.CommandType,
				payloadBytes,
				cmd.Status,
				cmd.CreatedAt,
			}, nil
		})

		_, err := a.pool.CopyFrom(
			ctx,
			pgx.Identifier{"commands"},
			[]string{"id", "router_id", "command_type", "payload", "status", "created_at"},
			copySource,
		)

		if err != nil {
			return fmt.Errorf("COPY batch failed at offset %d: %w", i, err)
		}
	}

	return nil
}

func (a *PostgresDataAdapter) createCommandsWithMultiInsert(ctx context.Context, cmds []domain.Command) error {
	var valueArgs []interface{}
	valueStrings := make([]string, 0, len(cmds))

	for i, cmd := range cmds {
		payloadBytes, err := json.Marshal(cmd.Payload)
		if err != nil {
			return err
		}

		start := i*6 + 1
		placeholder := fmt.Sprintf("($%d, $%d, $%d, $%d, $%d, $%d)",
			start, start+1, start+2, start+3, start+4, start+5)

		valueStrings = append(valueStrings, placeholder)

		valueArgs = append(valueArgs,
			cmd.ID, cmd.RouterID, cmd.CommandType, payloadBytes, cmd.Status, cmd.CreatedAt)
	}

	query := fmt.Sprintf(`
		INSERT INTO commands (id, router_id, command_type, payload, status, created_at)
		VALUES %s`,
		strings.Join(valueStrings, ","))

	_, err := a.pool.Exec(ctx, query, valueArgs...)
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
