ifneq (,$(wildcard infrastructure/.env))
	include infrastructure/.env
	export
endif

COMPOSE_FILE= ./infrastructure/docker-compose.yml
DC=docker compose -f $(COMPOSE_FILE)

.PHONY: up down downv restart logs help exec

help: ## Показать список доступных команд
	@echo "Usage: make <command>\n"
	@echo "Доступные команды:"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z0-9_-]+:.*?## / {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo "  \033[36mlogs-<name>\033[0m     Вывести логи конкретного контейнера (например: make logs-keycloak)"
	@echo "  \033[36mrestart-<name>\033[0m  Перезапуск конкретного контейнера (например: make restart-keycloak)"
	@echo "  \033[36mexec-<name>\033[0m     Вход в конкретный контейнер (например: make exec-keycloak)"

up: ## Запустить контейнеры в фоне
	$(DC) up -d
#	@bash -c '\
#	until docker exec cassandra cqlsh -e "describe keyspaces" > /dev/null 2>&1; do \
#		echo "Cassandra is not ready yet. Waiting 3 seconds..."; \
#		sleep 3; \
#	done'
#	@docker exec cassandra cqlsh -e "CREATE KEYSPACE IF NOT EXISTS $(KEYSPACE_NAME) WITH replication = {'class':'SimpleStrategy','replication_factor':1};"
#	@echo "Keyspace $(KEYSPACE_NAME) ready."

down: ## Остановить и удалить контейнеры
	$(DC) down

downv: ## Остановить контейнеры и удалить тома
	$(DC) down -v

restart: ## Перезапустить контейнеры
	$(DC) down && $(DC) up -d

restart-%: ## Перезапустить контейнер по имени
	$(DC) restart $*

logs: ## Вывести логи всех контейнеров
	$(DC) logs -f

logs-%: ## Вывести логи конкретного контейнера (например: make logs-keycloak)
	$(DC) logs -f $*

exec-%: ## Зайти в контейнер по имени
	docker exec -it $* bash
