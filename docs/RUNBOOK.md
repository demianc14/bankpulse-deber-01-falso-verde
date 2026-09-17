# Runbook operativo

## Estado general

```bash
docker compose ps
docker compose logs --tail=100 payments-api audit-api
curl -fsS http://localhost:8080/health/payments
curl -fsS http://localhost:8080/health/audit
curl -fsS http://localhost:8080/api/outbox
```

## Consultar MariaDB

```bash
docker compose exec mariadb mariadb -ubankpulse -p bankpulse \
  -e "SELECT id,status,amount,currency,created_at FROM payments ORDER BY created_at DESC LIMIT 10;"
```

La contraseña se solicitará de forma interactiva. Use el valor de `MARIADB_PASSWORD` en `.env`.

## Consultar MongoDB

```bash
docker compose exec mongo mongosh \
  -u root -p --authenticationDatabase admin audit \
  --eval 'db.audit_events.find().sort({receivedAt:-1}).limit(10).pretty()'
```

## Incidente: auditoría no disponible

1. Confirme que `payments-api` y MariaDB siguen saludables.
2. Consulte `/api/outbox`; `pending` debe crecer sin perder el pago.
3. Revise el último error del publicador en los logs.
4. Recupere MongoDB y `audit-api` con `docker compose up -d --wait mongo audit-api`.
5. Verifique que `pending` llega a cero y que MongoDB contiene un solo evento por `eventId`.

## Limpieza

- `docker compose down`: detiene sin borrar datos.
- `docker compose down -v`: elimina también las bases; acción destructiva y solo para reiniciar el laboratorio.
