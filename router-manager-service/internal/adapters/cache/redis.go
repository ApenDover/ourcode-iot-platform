package cache

import (
	"context"
	"encoding/json"
	"router-manager-service/internal/core/domain"
	"time"

	"github.com/redis/go-redis/v9"
	"router-manager-service/internal/ports"
)

type Adapter struct {
	client *redis.Client
}

func NewRedisClient(client *redis.Client) *Adapter {
	return &Adapter{client: client}
}

var _ ports.CachePort = (*Adapter)(nil)

func (c *Adapter) GetRouter(ctx context.Context, serial string) (*domain.Router, error) {
	val, err := c.client.Get(ctx, c.key(serial)).Result()
	if err == redis.Nil {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}

	var router domain.Router
	if err := json.Unmarshal([]byte(val), &router); err != nil {
		return nil, err
	}
	return &router, nil
}

func (c *Adapter) GetRouters(ctx context.Context, serials []string) ([]*domain.Router, error) {
	if len(serials) == 0 {
		return []*domain.Router{}, nil
	}

	keys := make([]string, len(serials))
	for i, s := range serials {
		keys[i] = c.key(s)
	}

	values, err := c.client.MGet(ctx, keys...).Result()
	if err != nil {
		return nil, err
	}

	routers := make([]*domain.Router, 0, len(values))
	for _, v := range values {
		if v == nil {
			continue
		}
		str, ok := v.(string)
		if !ok {
			continue
		}
		var router domain.Router
		if err := json.Unmarshal([]byte(str), &router); err == nil {
			routers = append(routers, &router)
		}
	}
	return routers, nil
}

func (c *Adapter) SetRouter(ctx context.Context, router domain.Router, ttl time.Duration) error {
	data, err := json.Marshal(router)
	if err != nil {
		return err
	}
	return c.client.Set(ctx, c.key(router.SerialNumber), data, ttl).Err()
}

func (c *Adapter) SetRouters(ctx context.Context, routers []domain.Router, ttl time.Duration) error {
	if len(routers) == 0 {
		return nil
	}

	pipe := c.client.Pipeline()
	for _, r := range routers {
		data, err := json.Marshal(r)
		if err != nil {
			return err
		}
		pipe.Set(ctx, c.key(r.SerialNumber), data, ttl)
	}
	_, err := pipe.Exec(ctx)
	return err
}

func (c *Adapter) key(serial string) string {
	return "router:" + serial
}
