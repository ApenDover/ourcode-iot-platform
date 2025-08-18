ifneq (,$(wildcard infrastructure/.env))
	include infrastructure/.env
	export
endif

COMPOSE_FILE=./infrastructure/docker-compose.yml
COMPOSE_FILE_LOCAL=./infrastructure/docker-compose.override.yml
DC=docker compose -f $(COMPOSE_FILE)
DCL=docker compose -f $(COMPOSE_FILE) -f $(COMPOSE_FILE_LOCAL)
ACTUATOR_URL := http://localhost:
LOGGER_NAME := ts.andrey

.PHONY: up down downv restart logs help exec logs-

help: ## Показать список доступных команд
	@echo "Usage: make <command>\n"
	@echo "Доступные команды:"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z0-9_-]+:.*?## / {printf "  \033[36m%-15s\033[0m			%s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo "  \033[36mlogs-<name>\033[0m     			Вывести логи конкретного контейнера (пример: make logs-keycloak)"
	@echo "  \033[36mrestart-<name>\033[0m  			Перезапуск конкретного контейнера (пример: make restart-keycloak)"
	@echo "  \033[36mexec-<name>\033[0m     			Вход в конкретный контейнер (пример: make exec-keycloak)"
	@echo "  \033[36mset-log-<level>-<port>\033[0m  	Установить логирование (пример: make set-log-debug-8080)"
	@echo "  \033[36mupdate-<service>\033[0m  		Пересобрать проект и развернуть контейнер"

up: ## Запустить контейнеры в фоне
	$(DC) up -d

up-local: ## Запустить все контейнеры в фоне c пересборкой проектов
	$(DCL) up -d

up-force-app: boot ## Пересобрать и перезагрузить проекты
	$(DCL) up -d --build --force-recreate event-collector device-collector kafka-producer

update-%:
	$(DC) up -d --build --force-recreate --no-deps $*

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

set-log-%:
	$(eval PARTS := $(subst -, ,$*))
	$(eval LEVEL := $(word 1,$(PARTS)))
	$(eval PORT := $(word 2,$(PARTS)))
	@curl --location '$(ACTUATOR_URL)$(PORT)/actuator/loggers/$(LOGGER_NAME)' \
		--header 'Content-Type: application/json' \
		--data '{"configuredLevel": "$(LEVEL)"}'

exec-%: ## Зайти в контейнер по имени
	docker exec -it $* bash

boot:  ## локально пересобрать образы
	cd event-collector && ./gradlew bootJar
	cd device-collector && ./gradlew bootJar
	cd kafka-producer && ./gradlew bootJar

rebuild:  ## локально пересобрать образы
	cd event-collector && ./gradlew clean build
	cd device-collector && ./gradlew clean build
	cd kafka-producer && ./gradlew clean build
