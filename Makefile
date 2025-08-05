COMPOSE_FILE= ./infrastructure/docker-compose.yml
DC=docker compose -f $(COMPOSE_FILE)

.PHONY: up down restart logs

up:
	$(DC) up -d

down:
	$(DC) down

restart:
	$(DC) down && $(DC) up -d

logs:
	$(DC) logs -f

logs-%:
	$(DC) logs -f $*
