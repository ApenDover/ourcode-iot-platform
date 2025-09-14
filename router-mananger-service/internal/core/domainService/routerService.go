package domainService

import (
	"log/slog"
	"router-mananger-service/internal/util"
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

func (s *RouterService) Create(routerId string) domain.Router {
	cmd := domain.Router{
		ID:           uuid.New(),
		SerialNumber: &routerId,
		CreatedAt:    time.Now(),
	}
	err := s.repo.Save(cmd)
	if err != nil {
		log.Error("не удалось сохранить роутер", slog.String("routerId", routerId), slog.String("error", err.Error()))
	}
	log.Debug("роутер сохранен",
		slog.String("routerId", routerId))
	return cmd
}

func (s *RouterService) CreateAll(routerSerials []string) []domain.Router {
	var batch []domain.Router

	for _, id := range routerSerials {
		cmd := domain.Router{
			ID:           uuid.New(),
			SerialNumber: &id,
			CreatedAt:    time.Now(),
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

func (s *RouterService) UpdateSeenAt(routerIds []uuid.UUID) {
	err := s.repo.UpdateSeenAt(routerIds)
	if err != nil {
		log.Error("не удалось обновить lastSeenAt для роутеров",
			slog.String("routers", util.UUIDsToStrings(routerIds)),
			slog.String("error", err.Error()),
		)
	}
}
