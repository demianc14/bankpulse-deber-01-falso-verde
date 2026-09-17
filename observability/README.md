# BankPulse Observability Demo

Este stack es independiente del `compose.yaml` de la aplicacion y se conecta a la red externa `bankpulse-network`.

## 1. Levantar BankPulse

Desde la raiz del laboratorio:

```bash
docker compose up -d --build
```

## 2. Levantar observabilidad

```bash
cd observability
docker compose up -d
```

## 3. Abrir herramientas

- Grafana: http://localhost:3000
  - usuario: `admin`
  - clave demo: `bankpulse_demo`
- Prometheus: http://localhost:9090
- cAdvisor: http://localhost:8088

En Codespaces, use la pestana **Ports** para abrir los puertos 3000, 9090 y 8088.

## 4. Verificar targets

En Prometheus abra **Status > Targets**. Deben aparecer `payments-api`, `audit-api`, `cadvisor` y `prometheus` en estado UP.

## 5. Dashboard

Grafana aprovisiona automaticamente:

`Dashboards > BankPulse Lab > BankPulse Platform Overview`

Incluye disponibilidad de APIs, solicitudes HTTP, memoria JVM y CPU de contenedores.

## Incidente de demostracion

Desde la raiz:

```bash
docker compose stop mongo audit-api
```

Observe en Grafana que `Audit API UP` cambia a 0. Cree un pago para demostrar que payments-api continua trabajando y que el outbox retiene el evento.

Recupere:

```bash
docker compose up -d mongo audit-api
```

El target de audit-api vuelve a 1 y el mecanismo de outbox puede completar la entrega pendiente.

> cAdvisor depende del acceso al Docker daemon del host. En algunos entornos Codespaces/Docker remotos sus metricas pueden estar limitadas; las metricas Spring Boot/Prometheus siguen funcionando.
