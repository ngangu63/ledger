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
| backend    | http://localhost:8081          | Spring Boot API (`/api/v1`, `/actuator`)|
| postgres   | localhost:5432                 | database `ledger` / user `ledger`       |

The frontend proxies `/api` and `/actuator` to the backend, so open
**http://localhost:5173** and everything works — no CORS or extra config.

> The backend container listens on `8080` internally but is published on host
> port **8081** (see `docker-compose.yml`) so it doesn't clash with a backend
> you might run locally on `8080`.

## Configuration & secrets

Every setting has a safe **dev default baked in**, so a fresh clone runs with
`docker compose up --build` and no extra steps — the JWT signing secret,
database password, and seed login all fall back to development values.

For any shared or production deployment, override them. Copy the template and
edit it — Compose reads `.env` automatically:

```bash
cp .env.example .env
```

| Variable            | Purpose                                   | Dev default        |
| ------------------- | ----------------------------------------- | ------------------ |
| `LEDGER_JWT_SECRET` | Base64 256-bit secret that signs JWTs     | public dev value   |
| `POSTGRES_PASSWORD` | Postgres password (DB **and** backend)    | `ledger`           |

Generate a strong JWT secret:

```bash
openssl rand -base64 32
```

Seed login for the API/UI is `admin` / `admin123` (role `ADMIN`); these live in
the backend source and are intended for development only.

> **Never deploy with the committed dev defaults.** They are public in this
> repository. `.env` is gitignored so your real secrets stay out of version
> control; only `.env.example` is tracked.

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
