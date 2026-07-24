# Ledger

Full-stack financial ledger application in a single repository:

```
LedgerApp/
├── backend/            Spring Boot API (Java 17, Maven)  → :8080
├── frontend/           React + TypeScript + Vite         → :5173
├── docker-compose.yml  Runs Postgres + backend + frontend
└── README.md
```

## Run the whole stack with one command

Requires only **Docker** (Docker Desktop or Docker Engine + Compose).

```bash
docker compose up --build
```

This builds and starts three containers:

| Service    | URL                            | Notes                                   |
| ---------- | ------------------------------ | --------------------------------------- |
| frontend   | http://localhost:5173          | nginx serving the built React app       |
| backend    | http://localhost:8080          | Spring Boot API (`/api/v1`, `/actuator`)|
| postgres   | localhost:5432                 | database `ledger` / user `ledger`       |

The frontend proxies `/api` and `/actuator` to the backend, so open
**http://localhost:5173** and everything works — no CORS or extra config.

Stop with `Ctrl+C`, or run detached and manage it:

```bash
docker compose up --build -d      # start in background
docker compose logs -f            # follow logs
docker compose down               # stop and remove containers
docker compose down -v            # also wipe the database volume
```

Startup order is handled automatically: the backend waits for Postgres to be
healthy, and the frontend waits for the backend. On first run the backend
applies its Flyway migrations to create the schema.

### Optional: database admin UI (pgAdmin)

```bash
docker compose --profile tools up
```

pgAdmin is then at http://localhost:5050 (admin@ledger.local / admin).

## Local development (hot reload)

For active development you may prefer running the apps directly with hot reload,
using Docker only for Postgres:

```bash
# 1. Database
docker compose up -d postgres

# 2. Backend (in backend/)
cd backend && ./mvnw spring-boot:run

# 3. Frontend (in frontend/)
cd frontend && npm install && npm run dev
```

The Vite dev server (http://localhost:5173) proxies `/api` and `/actuator` to
the backend on `:8080` (see [frontend/vite.config.ts](frontend/vite.config.ts)).

See [backend/README.md](backend/README.md) and
[frontend/README.md](frontend/README.md) for project-specific details.
