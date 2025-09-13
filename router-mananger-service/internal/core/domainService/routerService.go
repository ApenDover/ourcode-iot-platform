package domainService

import (
	"log/slog"
	"time"

	"github.com/google/uuid"
	"router-mananger-service/internal/domain"
	"router-mananger-service/internal/ports"
)

type RouterService struct {
	repo ports.RouterRepository
}

func NewRouterService(repo ports.RouterRepository) *RouterService {
	return &RouterService{repo: repo}
}

func (s *RouterService) Create(routerID uuid.UUID) domain.Router {
	cmd := domain.Router{
		ID:        routerID,
		CreatedAt: time.Now(),
	}

	err := s.repo.Save(cmd)
	if err != nil {
		log.Error("не удалось сохранить роутер", slog.String("routerId", routerID.String()), slog.String("error", err.Error()))
	}
	log.Debug("роутер сохранен",
		slog.String("routerId", routerID.String()))
	return cmd
}

func (s *RouterService) CreateAll(routerIDs []uuid.UUID) []domain.Router {
	var batch []domain.Router

	for _, id := range routerIDs {
		cmd := domain.Router{
			ID:        id,
			CreatedAt: time.Now(),
		}
		batch = append(batch, cmd)
	}

	err := s.repo.SaveAll(batch)
	if err != nil {
		log.Error("не удалось сохранить роутеры", slog.String("error", err.Error()))
	}
	log.Debug("все роутеры созданы")
	return batch
}

func (s *RouterService) GetAllRouterIds() []uuid.UUID {
	ids, err := s.repo.GetAllIds()
	if err != nil {
		log.Error("не удалось получить все id роутеров", slog.String("error", err.Error()))
	}
	return ids
}
