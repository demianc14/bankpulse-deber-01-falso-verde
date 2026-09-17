# Ejecutar BankPulse completamente online

GitHub Codespaces proporciona un entorno aislado por estudiante con VS Code, Java 21, GitHub CLI y Docker Compose. El laboratorio utiliza Docker-in-Docker dentro del Codespace; no requiere Docker Desktop en el equipo del participante.

## 1. Crear el Codespace

1. Suba esta carpeta a un repositorio de GitHub.
2. Abra el repositorio y seleccione **Code → Codespaces → Create codespace on main**.
3. Espere a que finalicen las tareas **postCreateCommand** y **postStartCommand**. El primer build descarga imágenes y dependencias Maven, por lo que puede tardar varios minutos.
4. Cuando aparezca la notificación del puerto, seleccione **Open in Browser**.

La consola se publica en el puerto `8080`. MariaDB (`3306`) y MongoDB (`27017`) permanecen como puertos privados del laboratorio.

## 2. Verificar el entorno

En el terminal integrado:

```bash
docker compose ps
bash scripts/smoke.sh
```

El smoke test comprueba health checks, pago idempotente, publicación del outbox y recepción del evento de auditoría.

## 3. Ejecutar el chaos drill

```bash
docker compose stop mongo audit-api
```

Cree un pago desde la consola. El pago debe quedar confirmado en MariaDB y el evento permanecer pendiente en el outbox. Después recupere el servicio:

```bash
docker compose up -d --wait mongo audit-api
```

La consola debe mostrar el drenaje del backlog y completar la misión de recuperación.

## 4. Detener o reconstruir

```bash
docker compose down             # conserva los volúmenes
docker compose up -d --wait     # reanuda el laboratorio
docker compose down -v          # borra las bases; acción destructiva
```

Al detener el Codespace desde GitHub se deja de consumir cómputo. El almacenamiento del Codespace continúa existiendo hasta que se elimine o expire.

## Seguridad para clases

- Mantenga el puerto `8080` con visibilidad **Private** para trabajo individual.
- Use visibilidad **Public** únicamente durante demostraciones y sin datos reales.
- No almacene tokens en `.env` ni en el código. Configure credenciales de publicación como Codespaces secrets o GitHub Actions secrets.
- Cada estudiante debe trabajar en su propio Codespace o fork para aislar bases y experimentos de caos.
