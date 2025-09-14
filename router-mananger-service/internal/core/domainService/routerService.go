package domainService

import (
	"log/slog"
	"strings"
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

func (s *RouterService) Create(serial string) domain.Router {
	router := domain.Router{
		ID:           uuid.New(),
		SerialNumber: serial,
		CreatedAt:    time.Now(),
	}
	err := s.repo.Save(router)
	if err != nil {
		log.Error("не удалось сохранить роутер",
			slog.String("serial", serial),
			slog.String("error", err.Error()))
	}
	log.Debug("роутер сохранен",
		slog.String("serial", serial))
	saved, err := s.repo.GetBySerial(serial)
	if err != nil {
		log.Error("не удалось найти роутер после сохранения",
			slog.String("serial", serial),
			slog.String("error", err.Error()))
	}
	return saved
}

func (s *RouterService) CreateAll(routerSerials []string) []domain.Router {
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
		log.Error("не удалось сохранить роутеры", slog.String("error", err.Error()))
	}
	log.Debug("все роутеры созданы")
	routers, err := s.repo.GetAllRoutersBySerials(routerSerials)
	if err != nil {
		return nil
	}
	return routers
}

func (s *RouterService) GetAllRouters() []domain.Router {
	ids, err := s.repo.GetAllRouters()
	if err != nil {
		log.Error("не удалось получить все роутеры", slog.String("error", err.Error()))
	}
	return ids
}

func (s *RouterService) GetBySerial(serial string) domain.Router {
	router, err := s.repo.GetBySerial(serial)
	if err != nil {
		log.Error("не удалось получить все роутеры", slog.String("error", err.Error()))
	}
	return router
}

func (s *RouterService) UpdateSeenAt(serial []string) {
	err := s.repo.UpdateSeenAt(serial)
	if err != nil {
		log.Error("не удалось обновить lastSeenAt для роутеров",
			slog.String("serial", strings.Join(serial, ", ")),
			slog.String("error", err.Error()),
		)
	}
}
