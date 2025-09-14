package db

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/prometheus/client_golang/prometheus"
	"log/slog"
	"router-mananger-service/internal/domain"
	"router-mananger-service/internal/metrics"
	"router-mananger-service/internal/util"
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
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("command_save"))
	defer timer.ObserveDuration()

	payloadBytes, err := json.Marshal(cmd.Payload)
	if err != nil {
		return err
	}

	_, err = r.pool.Exec(
		context.Background(),
		`
		 INSERT INTO commands (id, router_id, command_type, payload, status, created_at) 
         VALUES ($1, $2, $3, $4, $5, $6)
         `, cmd.ID, cmd.RouterID, cmd.CommandType, payloadBytes, cmd.Status, cmd.CreatedAt,
	)
	return err
}

func (r *PostgresCommandRepository) SaveAll(cmds []domain.Command) error {
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("command_save_all"))
	defer timer.ObserveDuration()

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

func (r *PostgresCommandRepository) GetBySerialAndStatuses(serial string, statuses []domain.CommandStatus) ([]domain.Command, error) {
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("command_get_by_serial_and_statuses"))
	defer timer.ObserveDuration()

	if len(statuses) == 0 {
		return nil, nil
	}

	query := `
		SELECT c.id, c.router_id, c.command_type, c.payload, c.status, c.sent_at, c.acked_at, c.created_at
		FROM commands c
		INNER JOIN routers r ON c.router_id = r.id
		WHERE r.serial_number = $1 AND status = ANY($2)
	`

	rows, err := r.pool.Query(context.Background(), query, serial, statuses)
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

func (r *PostgresCommandRepository) GetAllByRouterSerial(serial string) ([]domain.Command, error) {
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("command_get_all_router_serial"))
	defer timer.ObserveDuration()

	query := `
		SELECT c.id, c.router_id, c.command_type, c.payload, c.status, c.sent_at, c.acked_at, c.created_at
		FROM commands c
		INNER JOIN routers r ON c.router_id = r.id
		WHERE r.serial_number = $1
	`

	rows, err := r.pool.Query(context.Background(), query, serial)
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

func (r *PostgresCommandRepository) SetSentStatusForPendingByRouterSerial(serial string) error {
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("command_set_sent_status_for_pending_by_router_serial"))
	defer timer.ObserveDuration()

	query := `
		UPDATE commands SET status=$1, sent_at=$2 
		                WHERE serial_number=$3 
		                  AND status=$4
		`

	_, err := r.pool.Exec(
		context.Background(), query,
		domain.CommandStatusSent, time.Now(),
		serial, domain.CommandStatusPending,
	)
	return err
}

func (r *PostgresCommandRepository) SetSentStatusForPendingByCommandIds(cmds []domain.Command) error {
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("command_set_sent_status_for_pending_by_command_ids"))
	defer timer.ObserveDuration()

	if len(cmds) == 0 {
		return nil
	}

	ctx := context.Background()
	batch := &pgx.Batch{}

	query := `
			 UPDATE commands
			 SET status=$1, sent_at=$2
			 WHERE id=$3 
			   AND status=$4
			 `

	for _, cmd := range cmds {
		batch.Queue(query, domain.CommandStatusSent, time.Now(), cmd.ID, domain.CommandStatusPending)
	}

	br := r.pool.SendBatch(ctx, batch)
	defer br.Close()

	for range cmds {
		_, err := br.Exec()
		if err != nil {
			return err
		}
	}
	return nil
}

func (r *PostgresCommandRepository) UpdateStatusToAcked(routerId, commandID uuid.UUID) error {
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("command_update_status_to_acked"))
	defer timer.ObserveDuration()

	query := `
		 UPDATE commands
		 SET status = $1, acked_at = $2
		 WHERE id = $3 AND router_id = $4 AND status=$5
		 `

	_, err := r.pool.Exec(
		context.Background(), query, domain.CommandStatusAcked, time.Now(), commandID, routerId, domain.CommandStatusSent,
	)
	return err
}

func (r *PostgresCommandRepository) MarkExpiredAsError(timeout time.Duration) error {
	timer := prometheus.NewTimer(metrics.DatabaseDuration.WithLabelValues("command_mark_expired_as_error"))
	defer timer.ObserveDuration()

	query := `
		 UPDATE commands
		 SET status = $1
		 WHERE status = $2
		 AND sent_at < $3
		`

	now := time.Now()
	deadline := now.Add(-timeout)
	util.GetLogger().Info("Проверка просроченных ответов",
		slog.String("now", now.String()),
		slog.String("deadline", deadline.String()))
	_, err := r.pool.Exec(
		context.Background(), query, domain.CommandStatusError, domain.CommandStatusSent, deadline,
	)
	return err
}
