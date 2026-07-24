# LedgerReact

A React + TypeScript + Vite frontend for the Ledger service. The app talks to a
Spring Boot backend, which the Vite dev server proxies at `/api` and `/actuator`
(see [vite.config.ts](vite.config.ts)).

## Prerequisites

- Node.js 20+ and npm
- A running Ledger backend (Spring Boot) — Java 17+ and its build tool (Maven/Gradle)

## Getting started

Run the backend and frontend in **two separate terminals**.

### 1. Start the backend

The Spring Boot backend must be running on `http://localhost:8080` before the
frontend can log in or load data. From the backend project directory:

```bash
# Maven
./mvnw spring-boot:run

# or Gradle
./gradlew bootRun
```

Verify it is up:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

### 2. Start the frontend

From this directory (`LedgerReact`):

```bash
npm install      # first time only
npm run dev
```

Vite prints a local URL (default http://localhost:5173). Open it in your browser.
Requests to `/api` and `/actuator` are proxied to the backend on `:8080`, so no
CORS configuration is needed.

> If you see "Network error — is the backend running on :8080?", start the
> backend (step 1) and reload.

## Available scripts

| Command           | Description                                    |
| ----------------- | ---------------------------------------------- |
| `npm run dev`     | Start the Vite dev server with HMR             |
| `npm run build`   | Type-check and build for production (`dist/`)  |
| `npm run preview` | Serve the production build locally             |
| `npm run lint`    | Run Oxlint                                     |
