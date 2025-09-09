package ports

import "router-mananger-service/internal/domain"

type CommandRepository interface {
	Save(cmd domain.Command) error
}
