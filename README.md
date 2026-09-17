# BankPulse Platform Engineering Lab

Laboratorio hiperrealista de microservicios para practicar persistencia políglota, contenedores, resiliencia y entrega continua. No es una simulación: el panel consume dos APIs Spring Boot, los pagos se guardan en MariaDB y los eventos de auditoría en MongoDB.

## Arquitectura

- **payments-api (Java 21 + Spring Boot):** pagos, idempotencia y Transactional Outbox sobre MariaDB.
- **audit-api (Java 21 + Spring Boot):** recepción idempotente y consulta de eventos sobre MongoDB.
- **console (Nginx + HTML/JS):** consola de operaciones, evidencia técnica y progreso de misiones.
- **Docker Compose:** red privada, volúmenes, health checks y dependencias saludables.
- **GitHub Actions:** construcción, smoke test y publicación opcional en Docker Hub.

## Inicio rápido

Requisitos: Docker Desktop o Docker Engine con Compose v2 y Git.

```bash
cp .env.example .env
docker compose up --build -d
docker compose ps
```

Abra <http://localhost:8080>. El primer build puede tardar varios minutos porque Maven descarga dependencias dentro de los contenedores.

## Opción completamente online: GitHub Codespaces

El proyecto incluye una configuración `.devcontainer` lista para Codespaces. Después de subir la carpeta a GitHub:

1. Seleccione **Code → Codespaces → Create codespace on main**.
2. Espere a que se construya y levante el stack automáticamente.
3. Abra el puerto reenviado **BankPulse Operations Console (8080)**.

No es necesario instalar Java, Maven, MongoDB, MariaDB ni Docker en el computador del estudiante. Consulte la [guía detallada de Codespaces](docs/CODESPACES.md).

Para verificar todo el flujo:

```bash
bash scripts/smoke.sh
```

## Chaos drill reversible

```bash
docker compose stop mongo audit-api
# Cree un pago desde la consola; MariaDB lo confirma y el outbox queda pendiente.
docker compose up -d --wait mongo audit-api
# En pocos segundos el evento se entrega una sola vez y el outbox vuelve a cero.
```

No use `docker compose down -v` si desea conservar los datos. Consulte [Misiones](docs/MISSIONS.md) y [Runbook](docs/RUNBOOK.md).

## GitHub y Docker Hub

1. Cree un repositorio y suba esta carpeta.
2. La acción `ci.yml` construye el stack y ejecuta el smoke test en cada push y pull request.
3. Para publicar imágenes en Docker Hub, configure los secretos `DOCKERHUB_USERNAME` y `DOCKERHUB_TOKEN` y ejecute manualmente **Publish Docker Hub images**.

Nunca suba el archivo `.env`, contraseñas reales ni tokens al repositorio.
