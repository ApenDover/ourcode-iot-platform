ifneq (,$(wildcard infrastructure/.env))
	include infrastructure/.env
	export
endif

COMPOSE_FILE=./infrastructure/docker-compose.yml
COMPOSE_FILE_BUILD=./infrastructure/docker-compose.build.yml
COMPOSE_FILE_LOCAL=./infrastructure/docker-compose.override.yml
DC=docker compose -f $(COMPOSE_FILE)
DCB=docker compose -f $(COMPOSE_FILE_BUILD)
DCL=docker compose -f $(COMPOSE_FILE) -f $(COMPOSE_FILE_LOCAL)
ACTUATOR_URL=http://localhost:
LOGGER_NAME=ts.andrey

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

up:  ## Запустить контейнеры в фоне
	$(DC) up nexus -d
	@echo "⏳ Жду пока контейнер nexus станет healthy..."
	@until [ $$(docker inspect --format='{{.State.Health.Status}}' nexus) = "healthy" ]; do \
		sleep 2; \
	done
	@sleep 5;
	$(DCB) up -d build-iot-avro
	$(DCB) up -d build-iot-common
	$(DCB) up -d build-device-api
	$(DC) up -d
	@make keycloak-setup-users

up-local: ## Запустить все контейнеры в фоне
	$(DCL) up -d
	@make keycloak-setup-users

up-rebuild-local: boot ## Пересобрать и запустить все модули
	$(DCL) build event-collector device-collector kafka-producer failed-events-processor
	$(DCL) up -d

up-rebuild: ## Пересобрать и запустить все модули
	$(DC) build event-collector device-collector kafka-producer failed-events-processor
	$(DC) up -d

recreate-local: boot ## Пересобрать и перезагрузить все модули
	$(DCL) up -d --build --force-recreate event-collector device-collector kafka-producer failed-events-processor

recreate: ## Пересобрать и перезагрузить все модули
	$(DC) up -d --build --force-recreate event-collector device-collector kafka-producer failed-events-processor

update-%: ## Пересобрать и перезагрузить указанный модуль
	$(DCL) up -d --build --force-recreate --no-deps $*

down: ## Остановить и удалить контейнеры
	$(DC) down

downv: ## Остановить контейнеры и удалить тома
	$(DC) down -v

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

boot: nexus-deploy  ## локально пересобрать образы
	docker image rm infrastructure-device-collector -f
	docker image rm infrastructure-device-service -f
	docker image rm infrastructure-event-collector -f
	docker image rm infrastructure-kafka-producer -f
	docker image rm infrastructure-failed-events-processor -f
	cd event-collector && ./gradlew bootJar
	cd event-collector && ./gradlew bootJar
	cd device-collector && ./gradlew bootJar
	cd device-service && ./gradlew bootJar
	cd kafka-producer && ./gradlew bootJar
	cd failed-events-processor && ./gradlew bootJar

rebuild: nexus-deploy  ## локально пересобрать образы
	cd event-collector && ./gradlew clean build
	cd device-collector && ./gradlew clean build
	cd device-service && ./gradlew clean build
	cd kafka-producer && ./gradlew clean build
	cd failed-events-processor && ./gradlew clean build


wait-for-keycloak:
	@echo "⏳ Жду пока keycloak запустится..."
	@until docker logs keycloak 2>&1 | grep -q "started in"; do \
		sleep 5; \
	done
	@sleep 2
	@echo "Keycloak запущен"

publish-nexus:
	@echo "⏳ Жду пока контейнер nexus станет healthy..."
	@until [ $$(docker inspect --format='{{.State.Health.Status}}' nexus) = "healthy" ]; do \
		sleep 2; \
	done
	@sleep 5;
	@echo "Выгружаю api"
	@if curl -s -f http://localhost:7777/repository/maven-releases/ts/andrey/device-api/1.0.0/device-api-1.0.0.pom >/dev/null 2>&1; then \
		echo "device-api уже опубликован, пропускаем публикацию"; \
	else \
		echo "device-api не найден, выполняем публикацию Gradle..."; \
		cd device-api && ./gradlew publish; \
	fi
	@echo "Выгружаю avro"
	@if curl -s -f http://localhost:7777/repository/maven-releases/ts/andrey/iot-avro/1.0.0/iot-avro-1.0.0.pom >/dev/null 2>&1; then \
		echo "iot-avro уже опубликован, пропускаем публикацию"; \
	else \
		echo "iot-avro не найден, выполняем публикацию Gradle..."; \
		cd iot-avro && ./gradlew publish; \
	fi
	@echo "Выгружаю common"
	@if curl -s -f http://localhost:7777/repository/maven-releases/ts/andrey/iot-common/1.0.0/iot-common-1.0.0.pom >/dev/null 2>&1; then \
		echo "iot-common уже опубликован, пропускаем публикацию"; \
	else \
		echo "iot-common не найден, выполняем публикацию Gradle..."; \
		cd iot-common && ./gradlew publish; \
	fi

keycloak-setup-users: wait-for-keycloak
	@chmod +x ./infrastructure/keycloak/create-admin.sh
	@chmod +x ./infrastructure/keycloak/create-user.sh
	@chmod +x ./infrastructure/keycloak/create-secret.sh
	@cd ./infrastructure/keycloak && ./create-admin.sh
	@cd ./infrastructure/keycloak && ./create-user.sh
	@cd ./infrastructure/keycloak && ./create-secret.sh

nexus-deploy:
	$(DC) up nexus -d
	@make publish-nexus
