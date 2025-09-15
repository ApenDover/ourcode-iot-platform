package domainService

import (
	"context"
	"log/slog"
	"router-manager-service/internal/metrics"
	"router-manager-service/internal/util"
	"strings"
	"time"

	"github.com/google/uuid"
	"router-manager-service/internal/domain"
	"router-manager-service/internal/ports"
)

type RouterService struct {
	repo ports.RouterRepository
}

func NewRouterService(repo ports.RouterRepository) *RouterService {
	return &RouterService{repo: repo}
}

func (s *RouterService) Create(ctx context.Context, serial string) domain.Router {
	log := util.GetLogger(ctx)
	router := domain.Router{
		ID:           uuid.New(),
		SerialNumber: serial,
		CreatedAt:    time.Now(),
	}
	err := s.repo.Save(router)
	if err != nil {
		metrics.RouterErrors.WithLabelValues("create").Inc()
		log.Error("не удалось сохранить роутер",
			slog.String("serial", serial),
			slog.String("error", err.Error()))
	}
	log.Debug("роутер сохранен",
		slog.String("serial", serial))
	saved, err := s.repo.GetBySerial(serial)
	if err != nil {
		metrics.RouterErrors.WithLabelValues("create").Inc()
		log.Error("не удалось найти роутер после сохранения",
			slog.String("serial", serial),
			slog.String("error", err.Error()))
	}
	return saved
}

func (s *RouterService) CreateAll(ctx context.Context, routerSerials []string) []domain.Router {
	log := util.GetLogger(ctx)
	var batch []domain.Router

	for _, id := range routerSerials {
		cmd := domain.Router{
			ID:           uuid.New(),
			SerialNumber: id,
			CreatedAt:    time.Now(),
		}
		batch = append(batch, cmd)
	}

	err := s.repo.SaveAll(batch)
	if err != nil {
		metrics.RouterErrors.WithLabelValues("create-all").Inc()
		log.Error("не удалось сохранить роутеры", slog.String("error", err.Error()))
	}
	log.Debug("все роутеры созданы")
	routers, err := s.repo.GetAllRoutersBySerials(routerSerials)
	if err != nil {
		metrics.RouterErrors.WithLabelValues("create-all").Inc()
		return nil
	}
	return routers
}

func (s *RouterService) GetAllRouters(ctx context.Context) []domain.Router {
	log := util.GetLogger(ctx)
	ids, err := s.repo.GetAllRouters()
	if err != nil {
		metrics.RouterErrors.WithLabelValues("get-all-routers").Inc()
		log.Error("не удалось получить все роутеры", slog.String("error", err.Error()))
	}
	return ids
}

func (s *RouterService) GetBySerial(ctx context.Context, serial string) domain.Router {
	log := util.GetLogger(ctx)
	router, err := s.repo.GetBySerial(serial)
	if err != nil {
		metrics.RouterErrors.WithLabelValues("get-by-serial").Inc()
		log.Error("не удалось получить все роутеры", slog.String("error", err.Error()))
	}
	return router
}

func (s *RouterService) UpdateSeenAt(ctx context.Context, serial []string) {
	log := util.GetLogger(ctx)
	err := s.repo.UpdateSeenAt(serial)
	if err != nil {
		metrics.RouterErrors.WithLabelValues("update-seen-at").Inc()
		log.Error("не удалось обновить lastSeenAt для роутеров",
			slog.String("serial", strings.Join(serial, ", ")),
			slog.String("error", err.Error()),
		)
	}
}
