.PHONY: up down reset logs status smoke chaos-mongo recover-mongo

up:
	docker compose up --build -d

down:
	docker compose down

reset:
	docker compose down -v

logs:
	docker compose logs -f --tail=150

status:
	docker compose ps

smoke:
	bash scripts/smoke.sh

chaos-mongo:
	docker compose stop mongo audit-api

recover-mongo:
	docker compose up -d --wait mongo audit-api
