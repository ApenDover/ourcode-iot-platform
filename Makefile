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
PROJECT_ROOT := $(shell pwd)
PROTO_DIR := $(PROJECT_ROOT)/router-manager-service/protobuf
GENPROTO_DIR_SERVER := $(PROJECT_ROOT)/router-manager-service/internal/ports/genproto
GENPROTO_DIR_CLIENT := $(PROJECT_ROOT)/router/internal/ports/genproto
PROTO_FILES := $(wildcard $(PROTO_DIR)/*.proto)

.PHONY: up down downv restart logs help exec logs- proto-gen

help: ## Показать список доступных команд
	@echo "Usage: make <command>\n"
	@echo "Доступные команды:"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z0-9_-]+:.*?## / {printf "  \033[36m%-15s\033[0m			%s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo "  \033[36mlogs-<name>\033[0m     			Вывести логи конкретного контейнера (пример: make logs-keycloak)"
	@echo "  \033[36mrestart-<name>\033[0m  			Перезапуск конкретного контейнера (пример: make restart-keycloak)"
	@echo "  \033[36mexec-<name>\033[0m     			Вход в конкретный контейнер (пример: make exec-keycloak)"
	@echo "  \033[36mset-log-<level>-<port>\033[0m  	Установить логирование (пример: make set-log-debug-8080)"
	@echo "  \033[36mupdate-<service>\033[0m  		Пересобрать проект и развернуть контейнер"

up: proto-gen clear  ## Запустить контейнеры в фоне
	$(DC) up nexus -d
	@echo "⏳ Жду пока контейнер nexus станет healthy..."
	@until [ $$(docker inspect --format='{{.State.Health.Status}}' nexus) = "healthy" ]; do \
		echo "жду.." & sleep 10; \
	done
	@sleep 5;
	$(DCB) up -d iot-avro
	@until curl -s -f http://localhost:7777/repository/maven-releases/ts/andrey/iot-avro/1.0.0/iot-avro-1.0.0.pom >/dev/null 2>&1; do \
		echo "жду публикацию iot-avro.." & sleep 5; \
	done
	$(DCB) up -d iot-common
	$(DCB) up -d device-api
	$(DCB) up -d router-manager-proto-client
	@until curl -s -f http://localhost:7777/repository/maven-releases/ts/andrey/iot-common/1.0.0/iot-common-1.0.0.pom >/dev/null 2>&1; do \
    	echo "жду публикацию iot-common.." & sleep 10; \
    done
	$(DC) up -d
	@make keycloak-setup-users

up-local: clear ## Запустить все контейнеры в фоне
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

boot: nexus-deploy proto-gen  ## локально пересобрать образы
	docker image rm infrastructure-device-collector -f
	docker image rm infrastructure-device-service -f
	docker image rm infrastructure-event-service -f
	docker image rm infrastructure-orchestrator -f
	docker image rm infrastructure-event-collector -f
	docker image rm infrastructure-kafka-producer -f
	docker image rm infrastructure-failed-events-processor -f
	docker image rm infrastructure-router-manager-service -f
	docker image rm infrastructure-router -f
	docker image rm infrastructure-device-api -f
	docker image rm infrastructure-iot-avro -f
	docker image rm infrastructure-iot-common -f
	docker image rm infrastructure-router-manager-proto-client -f
	cd event-collector && env -u NEXUS_URL -u NEXUS_USER -u NEXUS_PASSWORD ./gradlew bootJar
	cd device-collector && env -u NEXUS_URL -u NEXUS_USER -u NEXUS_PASSWORD ./gradlew bootJar
	cd device-service && env -u NEXUS_URL -u NEXUS_USER -u NEXUS_PASSWORD ./gradlew bootJar
	cd orchestrator && env -u NEXUS_URL -u NEXUS_USER -u NEXUS_PASSWORD ./gradlew bootJar
	cd event-service && env -u NEXUS_URL -u NEXUS_USER -u NEXUS_PASSWORD ./gradlew bootJar
	cd kafka-producer && env -u NEXUS_URL -u NEXUS_USER -u NEXUS_PASSWORD ./gradlew bootJar
	cd failed-events-processor && env -u NEXUS_URL -u NEXUS_USER -u NEXUS_PASSWORD ./gradlew bootJar
	cd router-manager-service && go build -o router-manager ./cmd/app
	cd router && go build -o router ./cmd/app

rebuild: nexus-deploy  ## локально пересобрать образы
	cd event-collector && ./gradlew clean build
	cd device-collector && ./gradlew clean build
	cd device-service && ./gradlew clean build
	cd event-service && ./gradlew clean build
	cd orchestrator && ./gradlew clean build
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
		cd device-api && env -u NEXUS_URL -u NEXUS_USER -u NEXUS_PASSWORD ./gradlew clean build publish; \
	fi
	@if curl -s -f http://localhost:7777/repository/maven-releases/ts/andrey/device-api/1.0.0/event-api-1.0.0.pom >/dev/null 2>&1; then \
		echo "event-api уже опубликован, пропускаем публикацию"; \
	else \
		echo "event-api не найден, выполняем публикацию Gradle..."; \
		cd event-api && env -u NEXUS_URL -u NEXUS_USER -u NEXUS_PASSWORD ./gradlew clean build publish; \
	fi
	@echo "Выгружаю avro"
	@if curl -s -f http://localhost:7777/repository/maven-releases/ts/andrey/iot-avro/1.0.0/iot-avro-1.0.0.pom >/dev/null 2>&1; then \
		echo "iot-avro уже опубликован, пропускаем публикацию"; \
	else \
		echo "iot-avro не найден, выполняем публикацию Gradle..."; \
		cd iot-avro && env -u NEXUS_URL -u NEXUS_USER -u NEXUS_PASSWORD ./gradlew clean build publish; \
	fi
	@echo "Выгружаю common"
	@if curl -s -f http://localhost:7777/repository/maven-releases/ts/andrey/iot-common/1.0.0/iot-common-1.0.0.pom >/dev/null 2>&1; then \
		echo "iot-common уже опубликован, пропускаем публикацию"; \
	else \
		echo "iot-common не найден, выполняем публикацию Gradle..."; \
		cd iot-common && env -u NEXUS_URL -u NEXUS_USER -u NEXUS_PASSWORD ./gradlew clean build publish; \
	fi
	@echo "Выгружаю proto"
	@if curl -s -f http://localhost:7777/repository/maven-releases/ts/andrey/router-manager-proto-client/1.0.0/router-manager-proto-client-1.0.0.pom >/dev/null 2>&1; then \
		echo "router-manager-proto-client уже опубликован, пропускаем публикацию"; \
	else \
		echo "router-manager-proto-client не найден, выполняем публикацию Gradle..."; \
		cd router-manager-proto-client && env -u NEXUS_URL -u NEXUS_USER -u NEXUS_PASSWORD ./gradlew clean build publish; \
	fi
	@echo "Выгружаю proto"

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

proto-gen:
	@echo "Generating Go code from .proto files..."
	@mkdir -p $(GENPROTO_DIR_SERVER)
	@mkdir -p $(GENPROTO_DIR_CLIENT)
	@docker run --rm \
		-v $(PROTO_DIR):/protos \
		-v $(GENPROTO_DIR_SERVER):/gen \
		rvolosatovs/protoc \
		--proto_path=/protos \
		--go_out=/gen \
		--go_opt=paths=source_relative \
		--go-grpc_out=/gen \
		--go-grpc_opt=paths=source_relative \
		$(notdir $(PROTO_FILES))
	@docker run --rm \
    		-v $(PROTO_DIR):/protos \
    		-v $(GENPROTO_DIR_CLIENT):/gen \
    		rvolosatovs/protoc \
    		--proto_path=/protos \
    		--go_out=/gen \
    		--go_opt=paths=source_relative \
    		--go-grpc_out=/gen \
    		--go-grpc_opt=paths=source_relative \
    		$(notdir $(PROTO_FILES))
	@echo "Done!"

clear:
	docker rm -f device-api iot-common iot-avro router-manager-proto-client

jmeter: j-prepare
	JVM_ARGS="-Xms512m -Xmx2g" jmeter -n -t $(PROJECT_ROOT)/infrastructure/jmeter/router-manager-service.jmx -l $(PROJECT_ROOT)/infrastructure/jmeter/results.jtl -e -o ./report

j-prepare:
	@docker exec -i -e PGPASSWORD=$(APP_ROUTER_MANAGER_DATASOURCE_PASSWORD) postgres_router_manager \
    		psql -U $(APP_ROUTER_MANAGER_DATASOURCE_USERNAME) -d $(APP_ROUTER_MANAGER_DATASOURCE_DB) \
    		-c "TRUNCATE TABLE routers CASCADE;"

	@docker exec -i -e PGPASSWORD=$(APP_ROUTER_MANAGER_DATASOURCE_PASSWORD) postgres_router_manager \
    		psql -U $(APP_ROUTER_MANAGER_DATASOURCE_USERNAME) -d $(APP_ROUTER_MANAGER_DATASOURCE_DB) \
    		-c "INSERT INTO public.routers (id, serial_number, ip_address, last_seen_at, created_at) \
    			VALUES ('581811df-8b37-44fd-84ce-9b7791b0e4c0', '8354d1bd-ea67-494f-90fc-4b88bd874e95', null, null, '2025-09-19 19:56:18.389983')\
    			ON CONFLICT DO NOTHING;"

	@docker exec -i -e PGPASSWORD=$(APP_ROUTER_MANAGER_DATASOURCE_PASSWORD) postgres_router_manager \
    		psql -U $(APP_ROUTER_MANAGER_DATASOURCE_USERNAME) -d $(APP_ROUTER_MANAGER_DATASOURCE_DB) \
    		-c "INSERT INTO public.commands (id, router_id, command_type, payload, status, sent_at, acked_at, created_at) \
    			VALUES ('9354d1bd-ea67-494f-90fc-4b88bd874e95', '581811df-8b37-44fd-84ce-9b7791b0e4c0', 'FALLBACK', '{}', 'PENDING', null, null, '2025-09-19 19:56:18.410128')\
    			ON CONFLICT DO NOTHING;"

	@docker exec -i -e PGPASSWORD=$(APP_ROUTER_MANAGER_DATASOURCE_PASSWORD) postgres_router_manager \
    		psql -U $(APP_ROUTER_MANAGER_DATASOURCE_USERNAME) -d $(APP_ROUTER_MANAGER_DATASOURCE_DB) \
    		< ./infrastructure/jmeter/routers.sql

	@docker exec -i redis redis-cli -a $(REDIS_PASSWORD) FLUSHALL
	@docker exec -i redis-rms redis-cli -a $(APP_ROUTER_MANAGER_REDIS_PASSWORD) FLUSHALL

gotest:
	cd router-manager-service && go test ./internal/it/routes_test
