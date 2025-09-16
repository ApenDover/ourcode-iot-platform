package redisDomainService

import (
	"context"
	"encoding/json"
	"log/slog"
	"router-manager-service/internal/adapters/rediscli"
	"router-manager-service/internal/domain"
	"router-manager-service/internal/util"
	"time"
)

type RedisCommandRepository struct {
	client *rediscli.Client
}

func NewRedisCommandRepository(client *rediscli.Client) *RedisCommandRepository {
	return &RedisCommandRepository{
		client: client,
	}
}

func (r *RedisCommandRepository) Save(ctx context.Context, cmd domain.Command) {
	commandJSON, err := json.Marshal(cmd)
	log := util.GetLogger(ctx)
	if err != nil {
		log.Error("не удалось преобразовать объект для сохранения в redis",
			slog.String("command_id", cmd.ID.String()),
			slog.String("error", err.Error()))
	}
	err = r.client.Set(ctx, cmd.ID.String()+cmd.RouterID.String(), commandJSON, time.Duration(0))
	if err != nil {
		log.Error("не удалось сохранить объект в redis",
			slog.String("command_id", cmd.ID.String()),
			slog.String("error", err.Error()))
	}
	log.Debug("команда успешно сохранена в Redis",
		slog.String("command_id", cmd.ID.String()))
}

func (r *RedisCommandRepository) SaveAll(ctx context.Context, cmds []domain.Command) {
	log := util.GetLogger(ctx)

	kvMap := make(map[string]interface{}, len(cmds))
	for _, cmd := range cmds {
		cmdJSON, err := json.Marshal(cmd)
		if err != nil {
			log.Error("не удалось сериализовать команду",
				slog.String("command_id", cmd.ID.String()),
				slog.String("error", err.Error()))
			continue
		}
		kvMap[cmd.ID.String()] = cmdJSON
	}

	if len(kvMap) == 0 {
		log.Warn("нет команд для сохранения в Redis")
		return
	}

	if err := r.client.MSet(ctx, kvMap); err != nil {
		log.Error("не удалось сохранить команды в Redis",
			slog.Int("count", len(kvMap)),
			slog.String("error", err.Error()))
		return
	}

	log.Debug("команды успешно сохранены в Redis",
		slog.Int("count", len(kvMap)))
}
