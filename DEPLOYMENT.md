# MatheNauTicS — Backend Deployment

- **Backend:** Render (Docker-based, direct Git integration from `main`)
- **Database:** Supabase PostgreSQL

This document covers the backend deployment only. For the frontend
deployment (Cloudflare Pages), see the deployment documentation in the
[MatheNauTicS frontend repository](https://github.com/SergiFuentes/MatheNauTicS).

---

## Overview

Production backend: `https://mathenautics-be.onrender.com`

The backend is deployed on Render, built from a Dockerfile, and deployed
directly from `main`. No GitHub Actions deployment workflow is used:
Render detects pushes to `main`, builds, and rolls out on its own.

### Request flow

```text
┌──────────────────────┐
│   Cloudflare Pages   │
│   MatheNauTicS FE    │
└──────────┬───────────┘
           │ HTTPS / JSON + JWT
           ▼
┌──────────────────────┐
│       Render         │
│ Spring Boot Backend  │
│  Docker container    │
└──────────┬───────────┘
           │ JDBC
           ▼
┌──────────────────────┐
│      Supabase        │
│   PostgreSQL DB      │
└──────────────────────┘
```

---

## Render service setup (one-time)

1. Render Dashboard → New → Web Service.
2. Connect to `SergiFuentes/MatheNauTicS-BE`.
3. Environment: **Docker**.
4. Dockerfile path: `Dockerfile` (repo root).
5. Branch: `main`.
6. Health check path: `/health`.

### Environment variables (Production)

| Variable                  | Value                                    |
| ------------------------- | ---------------------------------------- |
| `SPRING_PROFILES_ACTIVE`  | `prod`                                   |
| `JWT_SECRET`              | generated secret, ≥ 32 bytes (HS256)     |
| `CORS_ALLOWED_ORIGINS`    | `https://mathenautics.pages.dev`         |
| `DB_URL`                  | `jdbc:postgresql://<host>:5432/postgres` |
| `DB_USERNAME`             | Supabase user                            |
| `DB_PASSWORD`             | Supabase password                        |

`PORT` is injected by Render; Spring Boot reads it via `${PORT:8080}`
from `application.yaml`.

### Notes

- `DB_URL` must use the JDBC scheme (`jdbc:postgresql://...`), not the
  raw PostgreSQL URI (`postgresql://...`) shown by Supabase.
- No production `.env` file is committed.
- `/health` is public (added to `permitAll()` in `SecurityConfig`).
- The `prod` profile (`application-prod.yaml`) activates the HikariCP
  limit and restricts CORS to `CORS_ALLOWED_ORIGINS`.

---

## Deployment flow

```text
push to main (backend repo)
      ↓
Render build
      ↓
Docker image built from Dockerfile
      ↓
container starts, env vars injected
      ↓
Flyway applies pending migrations
      ↓
/health reports UP
```

---

## Local development

```powershell
cd backend
.\run-local.ps1
```

`run-local.ps1` loads `.env` literally into the environment and runs
`./gradlew bootRun`. Do not rely on Spring's `.properties` parser for
`.env` files: it mangles certain characters (e.g. the Supabase password).

The backend listens on `http://localhost:8080` locally.

---

## Secrets

Never commit:

- Supabase database password
- JWT production secret
- Render credentials or deploy hooks
- Any production `.env` file

The frontend may contain the public backend URL.
The frontend must never contain backend credentials.

---

## Operational Lessons

These lessons were learned during real deployment, not from theoretical
analysis.

### Supabase networking

Connecting to Supabase from a cloud platform exposed IPv6 considerations.
The direct connection host (`db.<project>.supabase.co`) can resolve to an
IPv6 address, which causes connection failures on platforms without
outbound IPv6 support.

The Supabase Session Pooler endpoint was used instead. It provides an
IPv4-compatible endpoint and avoids this class of problem.

### Session Pooler connection limits

The Supabase Session Pooler imposes practical connection limits. When
the backend opened more connections than the pooler allowed, requests
failed.

This is a constraint of the managed database layer, not of the
application code.

### HikariCP = 3

The backend's HikariCP connection pool is intentionally limited to
**3 connections** in the `prod` profile:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 3
      minimum-idle: 1
      connection-timeout: 10000
```

This value was determined from the actual production deployment:

- Supabase Session Pooler connection limits.
- Render Free resource constraints.
- The real concurrency of the game's API traffic.

Do not increase this value casually. It is an operational configuration
derived from real deployment behaviour, not an arbitrary default.

### Render Free cold starts

Render's Free tier can spin down a service after a period of inactivity.
The next inbound request wakes the service, which can produce a
noticeable delay (cold start).

This is a platform limitation, not an application bug. It must be
documented honestly: Render Free is not equivalent to an always-on paid
production service.

The project uses Render Free because the objective is a zero-cost
deployment at this stage.

---

## UptimeRobot keepalive

To mitigate cold starts, an external HTTP monitor (UptimeRobot Free)
pings the backend's health endpoint every 5 minutes.

```text
┌──────────────┐       HTTP GET /health       ┌──────────────┐
│ UptimeRobot  │ ──────────────────────────► │    Render    │
│ every ~5 min │                              │   Backend    │
└──────────────┘                              └──────────────┘
```

The monitor targets:

```text
https://mathenautics-be.onrender.com/health
```

The endpoint executes a lightweight `SELECT 1` against the database
before reporting `UP`, so the keepalive also exercises the Supabase
connection and helps avoid cold-start latency on the database side.

This is an operational workaround, not application logic. No GitHub
Actions keepalive workflow, no Render Cron Job, no custom `/keepalive`
endpoint, and no secondary application are used.

### Verification procedure

The keepalive is considered effective only after observation:

1. UptimeRobot monitor configured and receiving successful responses.
2. Service left under observation for longer than the Render sleep period.
3. Backend remains responsive after the observation period.
4. Opening the game after inactivity shows no noticeable cold-start delay.
5. Backend logs show no errors or abnormal database activity.

> **Note:** The cold-start mitigation should not be claimed as solved
> until the observation above has actually been performed.

---

## Production Verification

The following checks were performed against the production backend.

### API flows

| Flow               | Method                                      | Result   |
| ------------------ | ------------------------------------------- | -------- |
| Guest creation     | POST `/api/v1/users`                        | 200      |
| Normal login       | POST `/api/v1/auth/login`                   | 200      |
| Progress creation  | POST `/api/v1/games/progress`               | 200      |
| Progress update    | POST `/api/v1/games/progress`               | 200      |
| Progress retrieval | GET `/api/v1/games/progress?gameMode=adventure` | 200   |
| Leaderboard        | GET `/api/v1/games/leaderboard?limit=20&offset=0` | 200 |

### CORS

Preflight `OPTIONS` requests succeeded for progress, leaderboard, users,
and progress retrieval. The actual browser API requests then succeeded
as well.

The production CORS configuration is restricted to the Cloudflare Pages
origin. It is not `*`, and permissive development origins are not
present in production.

When validating CORS, distinguish between:

1. The HTTP status of the preflight.
2. The actual `Access-Control-Allow-*` headers.
3. The fact that real browser requests subsequently succeeded.

The combination of those checks is the meaningful validation.

### Security observations

- No JWTs appear in application logs.
- Authentication works (`POST /api/v1/auth/login` returns a valid token).
- CORS is restricted.
- Production frontend does not call `localhost`.
- Database migrations are current (schema version 9).
- The backend is accessible from the deployed frontend.

### CI

GitHub Actions is used for CI only. There is no deployment workflow.

```text
Git push / Pull Request
          │
          ▼
   GitHub Actions
      ┌───────┐
      │ Tests │
      └───┬───┘
          │
          ▼
       Result
```

Workflow file:

```text
.github/workflows/backend-ci.yml
```

It uses Java 17 (Temurin) and Gradle, and runs a clean build.

Deployment is handled by Render's Git integration.