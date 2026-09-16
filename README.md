# MatheNauTicS Backend

Backend REST API for **MatheNauTicS**, an educational mathematics game built with Phaser 3.

The backend provides persistent user accounts, guest users, JWT-based authentication, game sessions, player progression, leaderboards, and account management functionality.

It is built with **Kotlin**, **Spring Boot**, and **PostgreSQL**, using **Spring JDBC and pure SQL** instead of an ORM.

---

## Table of Contents

* [Overview](#overview)
* [Features](#features)
* [Technology Stack](#technology-stack)
* [Requirements](#requirements)
* [Project Structure](#project-structure)
* [Database](#database)
* [Database Schema](#database-schema)
* [Database Migrations](#database-migrations)
* [Configuration](#configuration)
* [Running the Application](#running-the-application)
* [API](#api)
* [Authentication](#authentication)
* [CORS](#cors)
* [Security](#security)
* [Connection Pool](#connection-pool)
* [Testing](#testing)
* [Frontend Integration](#frontend-integration)
* [Dependencies](#dependencies)
* [Build Configuration](#build-configuration)
* [Development Workflow](#development-workflow)
* [Current Status](#current-status)
* [Roadmap](#roadmap)
* [Known Limitations](#known-limitations)
* [Security Considerations](#security-considerations)
* [License](#license)

---

# Overview

MatheNauTicS is an educational game focused on mathematics and multiplication tables.

The backend is responsible for:

* User account management
* Guest player management
* Guest-to-registered user conversion
* JWT-based authentication
* Game session persistence
* Coin persistence
* Player progression
* Leaderboards
* Game mode tracking
* Health monitoring

The frontend is a Phaser 3 web application that communicates with this backend through a REST API.

### Architecture

```text
┌────────────────────────────────┐
│       Phaser 3 Frontend        │
│                                │
│  Game / UI / User Interface    │
└────────────────┬───────────────┘
                 │
                 │ REST / JSON + JWT
                 ▼
┌────────────────────────────────┐
│      Spring Boot Backend       │
│                                │
│  Controllers                   │
│       ↓                        │
│  Security Filter (JWT)         │
│       ↓                        │
│  Services                      │
│       ↓                        │
│  Repositories                  │
│       ↓                        │
│  Spring JDBC / SQL             │
└────────────────┬───────────────┘
                 │
                 │ JDBC
                 ▼
┌────────────────────────────────┐
│      PostgreSQL / Supabase     │
│                                │
│  users                         │
│  game_sessions                 │
│  player_progress               │
│  leaderboard (view)            │
└────────────────────────────────┘
```

---

# Features

## User Management

The API supports both guest and registered players.

### Registered Users

Registered users have:

* Username
* Email
* Password
* Persistent game data
* Persistent progression
* Leaderboard visibility

Passwords are never stored in plain text and are hashed using BCrypt.

### Guest Users

Players can start playing without creating an account.

A guest user is automatically created when required by the game.

Guest users:

* Can play normally
* Can save game sessions
* Can accumulate coins
* Can maintain their progression
* Are excluded from the leaderboard
* Can later be converted into registered users

### Guest → Registered Conversion

The registration flow allows an existing guest player to create an account while preserving their existing game data.

The conversion endpoint is `POST /api/v1/users/me/convert` and is called with the guest's JWT. The target user is derived from the authenticated token, not from the request body.

This means that registering does not require starting the game again from scratch.

---

## Authentication

Authentication is JWT-based.

Clients obtain a token by sending credentials to the login endpoint. Subsequent requests to protected endpoints include the token in the `Authorization` header.

Login endpoint:

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "identifier": "<username or email>",
  "password": "<password>"
}
```

The response contains a JWT and basic user information:

```json
{
  "userId": "<uuid>",
  "username": "<username>",
  "email": "<email>",
  "isGuest": false,
  "token": "<jwt>"
}
```

Subsequent requests send the token as:

```http
Authorization: Bearer <jwt>
```

Tokens are signed with **HS256** using the `JWT_SECRET` environment variable (≥ 32 bytes). Expiration is 24 hours (86400000 ms), configured via `jwt.expiration` in `application.yaml`.

There is no refresh-token flow: once expired, the client must log in again. Logout is client-side only (the frontend discards the token).

---

## Game Sessions

Completed games can be persisted through the API.

Each game session stores information such as:

* User
* Game mode
* Score
* Total coins
* Duration
* Creation timestamp

Supported game modes include:

* `adventure`
* `training`

Game sessions are used as the basis for player statistics and leaderboard information.

---

## Player Progress

Player progression is persisted independently from individual game sessions.

Depending on the game mode, the backend stores:

### Adventure Mode

* Current level
* Score, lives, coins, difficulty (full runner state)

### Training Mode

* Current multiplication table

Progress is updated when the player advances through the corresponding game content.

Progress is keyed by `(user_id, game_mode)`.

---

## Leaderboard

The backend provides a score-based leaderboard.

Supported functionality includes:

* Score ranking
* Pagination
* Game mode filtering
* Registered users only

Guest players are excluded from leaderboard results.

Example:

```http
GET /api/v1/games/leaderboard?limit=10&offset=0&gameMode=adventure
```

---

# Technology Stack

| Technology      | Version             | Purpose                |
| --------------- | ------------------- | ---------------------- |
| Kotlin          | 2.0.21              | Backend language       |
| Spring Boot     | 3.4.3               | Application framework  |
| Spring JDBC     | Spring Boot managed | Database access        |
| Spring Security | Spring Boot managed | Authentication / JWT   |
| JJWT            | 0.12.6              | JWT signing & parsing  |
| PostgreSQL      | 42.7.x JDBC driver  | Database driver        |
| Supabase        | Managed PostgreSQL  | Cloud database         |
| Flyway          | 10.21.0             | Database migrations    |
| BCrypt          | 0.4                 | Password hashing       |
| Gradle          | Kotlin DSL          | Build system           |
| JUnit 5         | Spring Boot managed | Testing                |
| MockK           | 1.13.13             | Kotlin mocking         |
| Testcontainers  | 1.21.4              | Integration testing    |

---

# Requirements

Before running the backend locally, make sure the following are installed:

* Java 17+
* Git
* PostgreSQL-compatible database
* Gradle Wrapper included in the project

A Supabase PostgreSQL database can be used instead of running PostgreSQL locally.

Check the installed Java version:

```bash
java -version
```

---

# Project Structure

The project follows a layered architecture with a separation between application, domain, infrastructure, and security concerns.

```text
src/
├── main/
│   ├── kotlin/com/mathenautics/backend/
│   │   ├── BackendApplication.kt
│   │   ├── application/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   │   └── impl/
│   │   │   └── exception/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   └── repository/
│   │   ├── infrastructure/
│   │   │   └── repository/
│   │   ├── dto/
│   │   ├── security/
│   │   └── util/
│   │
│   └── resources/
│       ├── application.yaml
│       ├── application-prod.yaml
│       └── db/migration/
│           ├── V1__initial_schema.sql
│           ├── V2__2026_08_22.sql
│           ├── V3__2026_08_22.sql
│           ├── V4__2026_08_28.sql
│           ├── V5__2026_09_02.sql
│           ├── V6__2026_09_02.sql
│           ├── V7__2026_09_02.sql
│           ├── V8__2026__09_03.sql
│           └── V9__2026_09_04.sql
│
└── test/kotlin/
```

### Main Layers

#### `application/controller`

REST controllers exposing the HTTP API.

#### `application/service`

Business logic and application use cases. Interfaces live here; implementations under `service/impl`.

#### `application/exception`

Domain exceptions and the global exception handler.

#### `domain/model`

Domain models such as `User` and `UserCredentials`.

#### `domain/repository`

Repository interfaces defining persistence contracts.

#### `infrastructure/repository`

Concrete JDBC implementations built on `NamedParameterJdbcTemplate`.

#### `dto`

Request and response data transfer objects.

#### `security`

JWT service, JWT filter, and Spring Security configuration.

#### `util`

Small shared utilities (e.g. `OffsetDateTime` extensions).

---

# Database

The application uses PostgreSQL.

The recommended hosted database for the project is **Supabase**.

Database access is implemented using:

```text
NamedParameterJdbcTemplate
```

No ORM is used.

All database queries are written using SQL directly.

This provides explicit control over:

* SQL queries
* Joins
* Constraints
* Transactions
* Performance
* Database-specific functionality

---

# Database Schema

The current schema (Flyway version 9) contains the following tables and views.

## `users`

Stores player accounts.

```text
users
├── id              UUID PK, default gen_random_uuid()
├── username        VARCHAR(50) UNIQUE NOT NULL
├── email           VARCHAR(255) UNIQUE NOT NULL
├── password_hash   VARCHAR(255) NOT NULL
├── created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
└── is_guest        BOOLEAN NOT NULL DEFAULT TRUE
```

Indexes on `LOWER(username)`, `LOWER(email)`, and `is_guest` support case-insensitive lookups and guest filtering.

## `game_sessions`

Stores completed game sessions.

```text
game_sessions
├── id                BIGSERIAL PK
├── user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE
├── game_mode         VARCHAR(20) NOT NULL  CHECK IN ('adventure','training')
├── score             INTEGER NOT NULL      CHECK >= 0
├── total_coins       INTEGER NOT NULL      CHECK >= 0
├── duration_seconds  INTEGER NOT NULL      CHECK >= 0
└── created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
```

Indexed on `user_id`, `score DESC`, and `game_mode`.

## `player_progress`

Stores persistent player progression, keyed by `(user_id, game_mode)`.

```text
player_progress
├── user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE
├── game_mode       VARCHAR(20) NOT NULL  CHECK IN ('adventure','training')
├── current_level   INTEGER NOT NULL DEFAULT 1  CHECK >= 1
├── current_table   INTEGER DEFAULT 1
├── last_played_at  TIMESTAMPTZ NOT NULL DEFAULT now()
├── score           INTEGER NOT NULL DEFAULT 0
├── lives           INTEGER NOT NULL DEFAULT 3
├── coins           INTEGER NOT NULL DEFAULT 0
└── difficulty      VARCHAR(20) NOT NULL DEFAULT 'normal'

PRIMARY KEY (user_id, game_mode)
```

## `leaderboard` (view)

Read-only view used to generate leaderboard results.

```sql
SELECT u.username, gs.game_mode, gs.score, gs.total_coins, gs.created_at
FROM game_sessions gs
JOIN users u ON gs.user_id = u.id
WHERE u.is_guest = FALSE
ORDER BY gs.score DESC
```

Guest users are excluded from the leaderboard.

---

# Database Migrations

Database schema changes are managed with **Flyway**.

Migration files are located under:

```text
src/main/resources/db/migration/
```

Current production schema version: **9**.

Migration history (abbreviated):

| Version | Purpose                                                     |
| ------- | ----------------------------------------------------------- |
| V1      | Initial schema: users, game_sessions, player_progress, view |
| V2      | Add `is_guest` to users                                     |
| V3      | Leaderboard excludes guests; add game_mode to view          |
| V4      | Enforce `is_guest NOT NULL`; add lower-case indexes         |
| V5      | Add unique `(user_id, game_mode)` on player_progress        |
| V6      | Ensure `game_mode` exists and is NOT NULL                   |
| V7      | Constrain `game_mode IN ('adventure','training')`           |
| V8      | Add `score`, `lives`, `coins`, `difficulty` to progress     |
| V9      | Composite primary key `(user_id, game_mode)`                |

Flyway automatically applies pending migrations when the application starts.

---

# Configuration

Application configuration is split into:

* `application.yaml` — shared, default configuration.
* `application-prod.yaml` — production overrides (activated by `SPRING_PROFILES_ACTIVE=prod`).

Database credentials and secrets are never committed to the repository. They are supplied exclusively through environment variables.

## Environment Variables

| Variable               | Required | Purpose                                                 |
| ---------------------- | -------- | ------------------------------------------------------- |
| `DB_URL`               | yes      | JDBC URL (`jdbc:postgresql://host:5432/db`)             |
| `DB_USERNAME`          | yes      | Database user                                           |
| `DB_PASSWORD`          | yes      | Database password                                       |
| `JWT_SECRET`           | yes      | JWT signing secret (HS256, ≥ 32 bytes)                  |
| `CORS_ALLOWED_ORIGINS` | prod     | Comma-separated list of allowed frontend origins        |
| `PORT`                 | prod     | HTTP port; injected by Render. Defaults to `8080`       |
| `SPRING_PROFILES_ACTIVE` | prod   | Set to `prod` on Render                                 |

### Relevant defaults in `application.yaml`

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

server:
  port: ${PORT:8080}

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000   # 24 hours in milliseconds

cors:
  allowed-origins: http://localhost:5500,http://127.0.0.1:5500,...
```

### Production overrides in `application-prod.yaml`

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 3
      minimum-idle: 1
      connection-timeout: 10000

logging:
  level:
    root: WARN
    com.mathenautics: INFO
    org.springframework.web: WARN
    org.springframework.security: WARN
    org.flywaydb: INFO

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS}
```

> Never commit real database passwords, JWT secrets, API keys, tokens, or other secrets to Git.

---

# Running the Application

## Clone the Repository

```bash
git clone <repository-url>
cd <repository-directory>
```

## Configure Environment Variables

Set the required environment variables before starting the application.

### Linux / macOS

```bash
export DB_URL="jdbc:postgresql://<host>:5432/<database>"
export DB_USERNAME="<username>"
export DB_PASSWORD="<password>"
export JWT_SECRET="<at-least-32-bytes-secret>"
```

### Windows PowerShell

```powershell
$env:DB_URL="jdbc:postgresql://<host>:5432/<database>"
$env:DB_USERNAME="<username>"
$env:DB_PASSWORD="<password>"
$env:JWT_SECRET="<at-least-32-bytes-secret>"
```

## Build the Project

```bash
./gradlew clean build
```

## Run the Application

```bash
./gradlew bootRun
```

The API will be available at:

```text
http://localhost:8080
```

---

# API

The API is versioned under `/api/v1`, except `/health` and `/` which are top-level.

## Authentication

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "identifier": "<username or email>",
  "password": "<password>"
}
```

Returns `200 OK` with:

```json
{
  "userId": "<uuid>",
  "username": "<username>",
  "email": "<email>",
  "isGuest": false,
  "token": "<jwt>"
}
```

Invalid credentials return `401 Unauthorized`.

---

## Users

All user-scoped endpoints operate on the **authenticated user**, derived from the JWT. There is no `userId` path parameter.

### Create User

```http
POST /api/v1/users
Content-Type: application/json
```

Public endpoint. Used to create both guest and registered users.

### Get Current User

```http
GET /api/v1/users/me
Authorization: Bearer <jwt>
```

Returns the authenticated user's information.

### Update Current User

```http
PUT /api/v1/users/me
Authorization: Bearer <jwt>
```

Updates the authenticated user's information.

### Convert Guest to Registered

```http
POST /api/v1/users/me/convert
Authorization: Bearer <guest-jwt>
Content-Type: application/json
```

Converts the authenticated guest into a registered user, preserving game data.

### Delete Current User

```http
DELETE /api/v1/users/me
Authorization: Bearer <jwt>
```

Deletes the authenticated user and associated data (via cascade).

---

## Game Sessions

### Finish Game

```http
POST /api/v1/games/finish
Authorization: Bearer <jwt>
Content-Type: application/json
```

Stores a completed game session for the authenticated user.

### Get Player Coins

```http
GET /api/v1/games/player/coins
Authorization: Bearer <jwt>
```

Returns the authenticated player's accumulated coins.

### Get Leaderboard

```http
GET /api/v1/games/leaderboard
```

Public endpoint.

Optional query parameters:

```text
limit    (default 10)
offset   (default 0)
gameMode (optional, "adventure" | "training")
```

Example:

```http
GET /api/v1/games/leaderboard?limit=10&offset=0&gameMode=training
```

---

## Player Progress

### Get Progress

```http
GET /api/v1/games/progress?gameMode=adventure
Authorization: Bearer <jwt>
```

Returns the authenticated player's progress for the given game mode.

### Update Progress

```http
POST /api/v1/games/progress
Authorization: Bearer <jwt>
Content-Type: application/json
```

Creates or updates the authenticated player's progress for the given game mode.

Progress is keyed by `(user_id, game_mode)`, so this endpoint is idempotent per mode.

---

## Health Check

```http
GET /health
```

Public endpoint. Used by Render's health check and by the external keepalive monitor.

It performs a lightweight `SELECT 1` against the database and returns:

```json
{
  "status": "UP",
  "database": "connected"
}
```

Or, if the database is unreachable:

```json
{
  "status": "DOWN",
  "database": "disconnected",
  "error": "<message>"
}
```

Example:

```bash
curl http://localhost:8080/health
```

### Root

```http
GET /
```

Returns a simple text banner. Useful as a sanity check.

---

# Authentication

Authentication is JWT-based and enforced by Spring Security.

## Public endpoints

The following paths are `permitAll()` in `SecurityConfig`:

* `OPTIONS /**` (CORS preflight)
* `GET /`
* `GET /health`
* `POST /api/v1/auth/login`
* `POST /api/v1/users`
* `POST /api/v1/users/*/convert`
* `GET /api/v1/games/leaderboard`

Everything else requires a valid JWT.

## Protected endpoints

Any request to a non-public path must include:

```http
Authorization: Bearer <jwt>
```

If the token is missing or invalid, the backend responds with `401 Unauthorized` (via `HttpStatusEntryPoint`).

## Token contents

JWTs are signed with HS256 and contain:

| Claim      | Value                                    |
| ---------- | ---------------------------------------- |
| `sub`      | User UUID                                |
| `username` | Username                                 |
| `isGuest`  | `"true"` / `"false"` (string)            |
| `iat`      | Issued-at timestamp                      |
| `exp`      | Expiration (issued-at + 24h)             |

## Security filter

`JwtAuthenticationFilter` runs once per request, extracts the `Bearer` token, validates it, and — if valid — populates the `SecurityContext` with an `AuthenticatedUser(userId, username, isGuest)` principal and a `ROLE_USER` or `ROLE_GUEST` authority.

Controllers therefore never read a `userId` from the request body or query string. The authenticated identity is authoritative.

There is no server-side logout endpoint: the client simply discards the JWT.

---

# CORS

CORS is configured via `cors.allowed-origins`.

## Development

Defaults from `application.yaml` include local frontends:

```text
http://localhost:5500
http://127.0.0.1:5500
http://192.168.1.49:5500
http://192.168.0.16:5500
http://localhost:3000
http://127.0.0.1:3000
http://172.21.224.1:5500
http://192.168.61.55:5500
```

## Production

The production origin is supplied via the `CORS_ALLOWED_ORIGINS` environment variable and is restricted to:

```text
https://mathenautics.pages.dev
```

CORS is never configured with `*`. Development origins are not present in production.

---

# Security

The current implementation includes several security and data-integrity mechanisms.

## Password Hashing

Passwords are hashed using BCrypt before being persisted.

```text
Plain password
      ↓
    BCrypt
      ↓
password_hash
      ↓
   Database
```

Passwords are never stored in plain text. BCrypt verification is used on login.

## JWT Authentication

Protected endpoints require a valid JWT. Tokens are validated by `JwtAuthenticationFilter` before the request reaches the controller.

Tokens are signed with HS256 using `JWT_SECRET`, loaded from the environment.

## Stateless Sessions

`SessionCreationPolicy.STATELESS` is set. No HTTP session or cookie is used. CSRF is disabled accordingly.

## Identity from Token, not from Client

Controllers obtain the user identity from the `AuthenticatedUser` principal attached to the `Authentication` object. There is no `userId` parameter in the request body or query string for user-scoped operations. This eliminates a whole class of IDOR attacks by construction.

## Input Validation

Request DTOs are validated before reaching the business logic.

## Database Constraints

The database uses constraints to protect data integrity, including:

* Foreign keys with cascade deletion
* `CHECK` constraints on scores, coins, durations, and levels
* Enforced `game_mode IN ('adventure','training')`
* Unique `(user_id, game_mode)` in `player_progress`

## Guest Separation

Guest users are identified by `is_guest`. The leaderboard view filters them out.

---

# Connection Pool

The backend uses HikariCP. In production, the pool is intentionally limited:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 3
      minimum-idle: 1
      connection-timeout: 10000
```

This value was determined from actual production constraints:

* Supabase Session Pooler connection limits.
* Render Free resource limits.
* Actual API traffic concurrency.

It is an operational configuration, not a theoretical default. Do not increase it without concrete evidence that the production environment can support it.

---

# Testing

The backend has been tested using both automated tests and manual end-to-end API testing.

## Automated Tests

Run the test suite with:

```bash
./gradlew test
```

Test stack includes:

* JUnit 5
* Spring Boot Test
* Spring Security Test
* MockK (`io.mockk:mockk:1.13.13`)
* SpringMockK (`com.ninja-squad:springmockk:4.0.2`)
* Testcontainers (`1.21.4`, PostgreSQL module)

## CI

Backend CI runs on GitHub Actions via:

```text
.github/workflows/backend-ci.yml
```

It uses Java 17 (Temurin), Gradle, and runs a clean build.

## Manual API Testing

The REST API has been tested manually and end-to-end against production.

Verified endpoints:

| Endpoint                              | Method | Status |
| ------------------------------------- | ------ | ------ |
| `/api/v1/users`                       | POST   | ✅      |
| `/api/v1/users/me`                    | GET    | ✅      |
| `/api/v1/users/me`                    | PUT    | ✅      |
| `/api/v1/users/me`                    | DELETE | ✅      |
| `/api/v1/users/me/convert`            | POST   | ✅      |
| `/api/v1/auth/login`                  | POST   | ✅      |
| `/api/v1/games/finish`                | POST   | ✅      |
| `/api/v1/games/player/coins`          | GET    | ✅      |
| `/api/v1/games/leaderboard`           | GET    | ✅      |
| `/api/v1/games/progress`              | GET    | ✅      |
| `/api/v1/games/progress`              | POST   | ✅      |
| `/health`                             | GET    | ✅      |

## Edge Cases Tested

| Case                                            | Status |
| ----------------------------------------------- | ------ |
| Empty leaderboard                               | ✅      |
| Guest users excluded from leaderboard           | ✅      |
| First game initializes coin total               | ✅      |
| Guest registration preserves existing game data | ✅      |
| Invalid input validation                        | ✅      |
| User CRUD operations                            | ✅      |
| HTTP error handling with non-JSON bodies        | ✅      |

---

# Frontend Integration

The backend is designed to be consumed by the Phaser 3 frontend.

Development configuration:

```text
Frontend
http://localhost:5500

Backend
http://localhost:8080
```

The frontend uses the API base URL:

```text
http://localhost:8080/api/v1
```

In production this URL is provided by the frontend build-time configuration: Cloudflare Pages rewrites `config.js` with the value of `MATHENAUTICS_API_URL`, which points to:

```text
https://mathenautics-be.onrender.com/api/v1
```

The frontend stores the JWT returned by `/api/v1/auth/login` and sends it on protected requests as `Authorization: Bearer <jwt>`.

---

# Dependencies

## Runtime Dependencies

```kotlin
implementation("org.springframework.boot:spring-boot-starter-web")
implementation("org.springframework.boot:spring-boot-starter-jdbc")
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
implementation("org.jetbrains.kotlin:kotlin-reflect")
implementation("org.jetbrains.kotlin:kotlin-stdlib")
runtimeOnly("org.postgresql:postgresql")
implementation("org.mindrot:jbcrypt:0.4")
implementation("org.flywaydb:flyway-core:10.21.0")
implementation("org.flywaydb:flyway-database-postgresql:10.21.0")
implementation("io.jsonwebtoken:jjwt-api:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
```

## Test Dependencies

```kotlin
testImplementation("org.springframework.boot:spring-boot-starter-test")
testImplementation("org.springframework.security:spring-security-test")
testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
testRuntimeOnly("org.junit.platform:junit-platform-launcher")
testImplementation("io.mockk:mockk:1.13.13")
testImplementation("com.ninja-squad:springmockk:4.0.2")
testImplementation("org.testcontainers:testcontainers:1.21.4")
testImplementation("org.testcontainers:postgresql:1.21.4")
testImplementation("org.testcontainers:junit-jupiter:1.21.4")
testImplementation("org.springframework.boot:spring-boot-testcontainers")
```

---

# Build Configuration

The main Gradle configuration uses:

```kotlin
plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
}
```

Java toolchain: **17**.

---

# Development Workflow

A typical development workflow is:

```text
1. Start PostgreSQL / Supabase
           ↓
2. Configure environment variables (DB_*, JWT_SECRET)
           ↓
3. Start Spring Boot
           ↓
4. Flyway applies migrations
           ↓
5. Backend starts on port 8080
           ↓
6. Start Phaser frontend
           ↓
7. Frontend logs in, receives JWT, communicates with /api/v1
```

---

# Current Status

The backend provides the core persistence and authentication layer required by the game.

| Feature                           | Status     |
| --------------------------------- | ---------- |
| User creation                     | ✅ Complete |
| Guest users                       | ✅ Complete |
| Registered users                  | ✅ Complete |
| Guest → Registered conversion     | ✅ Complete |
| User CRUD (via `/me`)             | ✅ Complete |
| Password hashing (BCrypt)         | ✅ Complete |
| JWT authentication                | ✅ Complete |
| Identity derived from JWT only    | ✅ Complete |
| Game session persistence          | ✅ Complete |
| Coin persistence                  | ✅ Complete |
| Leaderboard                       | ✅ Complete |
| Leaderboard pagination            | ✅ Complete |
| Leaderboard game-mode filtering   | ✅ Complete |
| Player progression                | ✅ Complete |
| Flyway migrations                 | ✅ Complete |
| PostgreSQL / Supabase integration | ✅ Complete |
| Input validation                  | ✅ Complete |
| Error handling                    | ✅ Complete |
| Health check                      | ✅ Complete |
| CORS                              | ✅ Complete |
| Production deployment (Render)    | ✅ Complete |
| Backend CI (GitHub Actions)       | ✅ Complete |

---

# Roadmap

## Short-term

### Anti-cheat Validation

Move critical game validation to the server.

The server should validate, where possible:

* Scores
* Coins
* Game duration
* Progression
* Game session consistency

This prevents players from modifying client-side requests to artificially increase their score or currency.

---

## Medium-term

### Rate Limiting

Protect the API against excessive requests and abuse.

### Email Verification

Implement email ownership verification for registered accounts.

### Password Reset

Provide a secure password recovery flow.

### Refresh Tokens

Current JWTs expire after 24 hours with no refresh mechanism. A refresh-token flow may be introduced to improve UX without weakening security.

### Game Analytics

Collect non-sensitive gameplay statistics such as:

* Session duration
* Games completed
* Tables completed
* Progression
* Game mode usage

---

# Known Limitations

| Limitation                         | Status                        |
| ---------------------------------- | ----------------------------- |
| Server-side anti-cheat validation  | ⚠️ Planned                    |
| Rate limiting                      | ⚠️ Planned                    |
| Email verification                 | ⚠️ Planned                    |
| Password reset                     | ⚠️ Planned                    |
| Refresh-token flow                 | ⚠️ Planned                    |
| Render Free cold starts            | ⚠️ Mitigated with UptimeRobot |

The current backend should therefore be considered a **pre-production implementation** until server-side game validation is implemented.

---

# Security Considerations

The following items remain open for the security assessment phase:

* Implement server-side score validation
* Implement server-side coin validation
* Implement rate limiting
* Review database permissions
* Review Supabase Row Level Security configuration
* Add appropriate logging and monitoring
* Conduct a structured security assessment (pentest)

Items already addressed:

* JWT authentication (HS256, 24h expiration)
* Identity derived exclusively from the authenticated principal
* BCrypt password hashing
* Stateless sessions (no cookies, CSRF disabled accordingly)
* Production CORS restricted to the deployed frontend origin
* Secrets stored exclusively in environment variables
* HTTPS in production
* No JWTs in application logs

---

# License

This project is currently a private project.

Add the appropriate license here if the project is released publicly.