COMPOSE_FILE= ./infrastructure/docker-compose.yml
DC=docker compose -f $(COMPOSE_FILE)

.PHONY: up down restart logs


help: ## Показать список доступных команд
	@echo "Usage: make <command>\n"
	@echo "Доступные команды:"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z0-9_-]+:.*?## / {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo "  \033[36mlogs-<name>\033[0m     Вывести логи конкретного контейнера (например: make logs-keycloak)"

up: ## Запустить контейнеры в фоне
	$(DC) up -d

down: ## Остановить и удалить контейнеры
	$(DC) down

downv: ## Остановить контейнеры и удалить тома
	$(DC) down -v

restart: ## Перезапустить контейнеры
	$(DC) down && $(DC) up -d

logs: ## Вывести логи всех контейнеров
	$(DC) logs -f

logs-%: ## Вывести логи конкретного контейнера (например: make logs-keycloak)
	$(DC) logs -f $*
