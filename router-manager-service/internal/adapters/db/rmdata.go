package db

import (
	"context"
	"encoding/json"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"router-manager-service/internal/conf/util"
	"router-manager-service/internal/core/domain"
	"router-manager-service/internal/ports"
	"time"
)

type PostgresDataAdapter struct {
	pool *pgxpool.Pool
}

func NewPostgresCommonAdapter(pool *pgxpool.Pool) ports.DataPort {
	return &PostgresDataAdapter{pool: pool}
}

var _ ports.DataPort = (*PostgresDataAdapter)(nil)

func (a *PostgresDataAdapter) CreateCommands(ctx context.Context, routerSerials []string, cmd domain.Command) error {
	tx, err := a.pool.BeginTx(ctx, pgx.TxOptions{})
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)

	payloadBytes, err := json.Marshal(cmd.Payload)
	if err != nil {
		return err
	}

	now := time.Now()
	for _, serial := range routerSerials {
		_, err := tx.Exec(ctx, `
			INSERT INTO commands (id, router_id, command_type, payload, status, created_at)
			SELECT $1, r.id, $2, $3, $4, $5
			FROM routers r
			WHERE r.serial_number = $6
		`, cmd.ID, cmd.CommandType, payloadBytes, domain.CommandStatusPending, now, serial)
		if err != nil {
			return err
		}
	}

	return tx.Commit(ctx)
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
		SELECT c.id, c.router_id, c.command_type, c.payload, c.status, 
		       c.created_at, c.sent_at, c.acked_at
		FROM commands c
		INNER JOIN routers r ON c.router_id = r.id
		WHERE r.serial_number = $1 AND c.status = $2
	`, routerSerial, domain.CommandStatusPending)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var commands []domain.Command
	var commandIDs []uuid.UUID

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
		commandIDs = append(commandIDs, cmd.ID)
	}

	if err := rows.Err(); err != nil {
		return nil, err
	}

	if len(commandIDs) == 0 {
		util.GetLogger(ctx).Info("Команды не найдены")
		if _, err := tx.Exec(ctx, `
			UPDATE routers
			SET last_seen_at=$1
			WHERE serial_number=$2
		`, time.Now(), routerSerial); err != nil {
			return nil, err
		}
		return commands, tx.Commit(ctx)
	}

	batch := &pgx.Batch{}
	for _, id := range commandIDs {
		batch.Queue(`
			UPDATE commands
			SET status=$1, sent_at=$2
			WHERE id=$3 AND status=$4
		`, domain.CommandStatusSent, time.Now(), id, domain.CommandStatusPending)
	}

	br := tx.SendBatch(ctx, batch)
	for range commandIDs {
		if _, err := br.Exec(); err != nil {
			_ = br.Close()
			return nil, err
		}
	}
	if err := br.Close(); err != nil {
		return nil, err
	}

	if _, err := tx.Exec(ctx, `
		UPDATE routers
		SET last_seen_at=$1
		WHERE serial_number=$2
	`, time.Now(), routerSerial); err != nil {
		return nil, err
	}

	if err := tx.Commit(ctx); err != nil {
		return nil, err
	}

	return commands, nil
}

func (a *PostgresDataAdapter) AckCommand(ctx context.Context, routerSerial string, commandID uuid.UUID) error {
	tx, err := a.pool.BeginTx(ctx, pgx.TxOptions{})
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)

	_, err = tx.Exec(ctx, `
		UPDATE commands c
		SET status=$1, acked_at=$2
		FROM routers r
		WHERE c.id=$3 AND c.router_id=r.id AND r.serial_number=$4 AND c.status=$5
	`, domain.CommandStatusAcked, time.Now(), commandID, routerSerial, domain.CommandStatusSent)
	if err != nil {
		return err
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
