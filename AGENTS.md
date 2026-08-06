# AGENTS.md

Two-package project: a Spring Boot backend and a Vue frontend, each containerized and deployed behind Traefik. No root-level build tooling — operate within each package directory.

## Layout

- `code/backend/inmemory-manager-service/` — Spring Boot 3.2.5, Java 17, Maven. Package root `com.qingfox.inmemory.manager`. Entry: `InmemoryManagerApplication.java`.
- `code/frontend/inmemory-manager-frontend/` — Vue 3 + Vite 5, Element Plus, Pinia, vue-router.
- `project.properties` — source of truth for project name, tech stack, and DB connection strings (postgres `bill`, mysql `nacos`).
- `code/backend/inmemory-manager-service/api.md` — backend API docs; update when endpoints change.

## Commands

Backend (run inside `code/backend/inmemory-manager-service/`):
- Build: `mvn clean package -DskipTests`
- Test all: `mvn test`
- Single test class: `mvn test -Dtest=HealthControllerTest`
- Run locally: `mvn spring-boot:run` (needs Nacos/Redis/Postgres reachable; Nacos config is optional and env-gated, see below)

Frontend (run inside `code/frontend/inmemory-manager-frontend/`):
- Dev server: `npm run dev` (port 3000, proxies `/api` → `http://localhost:8080`)
- Build: `npm run build` (outputs `dist/`, served under the subpath below)

Deploy (per package, do not expose ports — Traefik routes by Host/Path labels):
- `docker compose up -d` in the package directory.

## URL path conventions (non-obvious — verified across config)

All routes are subpath-mounted; do not assume `/` roots.
- Backend context-path: `/inmemory-manager/inmemory-manager-service`, APIs under `.../api/` (e.g. `/inmemory-manager/inmemory-manager-service/api/health`).
- Frontend base path: `/inmemory-manager/inmemory-manager-frontend/` (set in `vite.config.js` `base` and `nginx.conf`).
- In production the frontend nginx proxies `/inmemory-manager/inmemory-manager-service/api/` → backend container `inmemory-manager-inmemory-manager-service-backend:8080`. If you rename the backend service/container, update `nginx.conf` too.
- Swagger UI: `/inmemory-manager/inmemory-manager-service/swagger-ui.html`.

## Backend runtime dependencies

- Nacos (config + discovery): optional, controlled by `NACOS_CONFIG_ENABLED` (default false). `compose.yaml` enables it pointing at `ubuntu-pc:58848`, namespace `local`. Many `NACOS_*` env vars cascade with fallbacks — see `application.yml`.
- Redis: required by `RedisStreamService` (stream/message handling).
- PostgreSQL: persistence via MyBatis-Plus; mappers in `src/main/resources/mapper/`.
- Lombok: used throughout; excluded from the repackaged jar via `spring-boot-maven-plugin`.

## Conventions

- Controller responses are wrapped in `model/ApiResponse.java` — follow that shape for new endpoints.
- DTOs live under `model/dto/`; keep request/response shapes there.
- Frontend API calls go through `src/utils/request.js` + `src/api/index.js`; add new endpoints there rather than calling axios directly in views.
