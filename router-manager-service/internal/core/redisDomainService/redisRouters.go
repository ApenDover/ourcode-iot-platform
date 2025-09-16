package redisDomainService

import (
	"context"
	"encoding/json"
	"github.com/redis/go-redis/v9"
	"log/slog"
	"router-manager-service/internal/adapters/rediscli"
	"router-manager-service/internal/domain"
	"router-manager-service/internal/util"
	"time"
)

type RedisRouterRepository struct {
	client *rediscli.Client
}

func NewRedisRouterRepository(client *rediscli.Client) *RedisRouterRepository {
	return &RedisRouterRepository{
		client: client,
	}
}

func (r *RedisRouterRepository) Save(ctx context.Context, cmd domain.Router) {
	RouterJSON, err := json.Marshal(cmd)
	log := util.GetLogger(ctx)
	if err != nil {
		log.Error("не удалось преобразовать объект для сохранения в redis",
			slog.String("Router_id", cmd.ID.String()),
			slog.String("error", err.Error()))
	}
	err = r.client.Set(ctx, cmd.SerialNumber, RouterJSON, time.Duration(0))
	if err != nil {
		log.Error("не удалось сохранить объект в redis",
			slog.String("Router_id", cmd.ID.String()),
			slog.String("error", err.Error()))
	}
	log.Debug("команда успешно сохранена в Redis",
		slog.String("Router_id", cmd.ID.String()))
}

func (r *RedisRouterRepository) SaveAll(ctx context.Context, routers []domain.Router) {
	if len(routers) == 0 {
		return
	}

	log := util.GetLogger(ctx)

	kvMap := make(map[string]interface{}, len(routers))
	for _, router := range routers {
		cmdJSON, err := json.Marshal(router)
		if err != nil {
			log.Error("не удалось сериализовать команду",
				slog.String("Serial", router.SerialNumber),
				slog.String("error", err.Error()))
			continue
		}
		kvMap[router.ID.String()] = cmdJSON
	}

	if err := r.client.MSet(ctx, kvMap); err != nil {
		log.Error("не удалось роутеры команды в Redis",
			slog.Int("count", len(kvMap)),
			slog.String("error", err.Error()))
		return
	}

	log.Debug("роутеры успешно сохранены в Redis",
		slog.Int("count", len(kvMap)))
}

func (r *RedisRouterRepository) GetBySerial(ctx context.Context, serial string) *domain.Router {
	log := util.GetLogger(ctx)
	routerJson, err := r.client.Get(ctx, serial)
	if err != nil {
		if err == redis.Nil {
			log.Debug("роутер не найден в redis", slog.String("serial", serial))
			return nil // ключа нет
		}
		log.Error("не смог получить роутер из redis по serial", slog.String("serial", serial))
		return nil
	}

	var router domain.Router
	if err := json.Unmarshal([]byte(routerJson), &router); err != nil {
		log.Error("ошибка при десериализации роутера из redis",
			slog.String("serial", serial),
			slog.String("error", err.Error()))
		return nil
	}
	return &router
}

func (r *RedisRouterRepository) GetBySerials(ctx context.Context, serials []string) ([]domain.Router, error) {
	log := util.GetLogger(ctx)

	values, err := r.client.MGet(ctx, serials)
	if err != nil {
		log.Error("ошибка при получении роутеров из Redis",
			slog.Any("serials", serials),
			slog.String("error", err.Error()))
		return nil, err
	}

	var routers []domain.Router
	for i, v := range values {
		if v == "" {
			log.Warn("роутер не найден в Redis",
				slog.String("serial", serials[i]))
			continue
		}

		var router domain.Router
		if err := json.Unmarshal([]byte(v), &router); err != nil {
			log.Error("ошибка при десериализации роутера из Redis",
				slog.String("serial", serials[i]),
				slog.String("error", err.Error()))
			continue
		}

		routers = append(routers, router)
	}

	return routers, nil
}
