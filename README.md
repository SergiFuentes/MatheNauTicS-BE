# MatheNauTicS Backend

Backend REST API for **MatheNauTicS**, an educational mathematics game built with Phaser 3.

The backend provides persistent user accounts, guest users, game sessions, player progression, leaderboards, and account management functionality.

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
* [CORS](#cors)
* [Security](#security)
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
* Game session persistence
* Coin persistence
* Player progression
* Leaderboards
* Game mode tracking
* Health monitoring

The frontend is a Phaser 3 web application that communicates with this backend through a REST API.

### Architecture

```text
┌──────────────────────────────┐
│       Phaser 3 Frontend      │
│                              │
│  Game / UI / User Interface  │
└──────────────┬───────────────┘
               │
               │ REST / JSON
               ▼
┌──────────────────────────────┐
│      Spring Boot Backend     │
│                              │
│ Controllers                  │
│      ↓                       │
│ Services                     │
│      ↓                       │
│ Repositories                 │
│      ↓                       │
│ Spring JDBC / SQL            │
└──────────────┬───────────────┘
               │
               │ JDBC
               ▼
┌──────────────────────────────┐
│      PostgreSQL / Supabase   │
│                              │
│ users                        │
│ game_sessions                │
│ player_progress              │
│ leaderboard                  │
└──────────────────────────────┘
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

This means that registering does not require starting the game again from scratch.

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

### Training Mode

* Current multiplication table

Progress is updated when the player advances through the corresponding game content.

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

```text
GET /api/v1/games/leaderboard?limit=10&offset=0&gameMode=adventure
```

---

# Technology Stack

| Technology  | Version             | Purpose               |
| ----------- | ------------------- | --------------------- |
| Kotlin      | 2.0.21              | Backend language      |
| Spring Boot | 3.4.3               | Application framework |
| Spring JDBC | Spring Boot managed | Database access       |
| PostgreSQL  | 42.7.x JDBC driver  | Database              |
| Supabase    | Managed PostgreSQL  | Cloud database        |
| Flyway      | 10.21.0             | Database migrations   |
| BCrypt      | 0.4                 | Password hashing      |
| Gradle      | Kotlin DSL          | Build system          |
| JUnit 5     | Spring Boot managed | Testing               |

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

The project follows a layered architecture with a separation between application, domain, and infrastructure concerns.

```text
src/
├── main/
│   ├── kotlin/
│   │   └── ...
│   │       ├── application/
│   │       │   ├── controller/
│   │       │   └── service/
│   │       │
│   │       ├── domain/
│   │       │   └── repository/
│   │       │
│   │       ├── infrastructure/
│   │       │   └── repository/
│   │       │
│   │       ├── dto/
│   │       │
│   │       └── config/
│   │
│   └── resources/
│       ├── application.yaml
│       └── db/
│           └── migration/
│               └── V1__initial_schema.sql
│
└── test/
    └── kotlin/
```

### Main Layers

#### `application/controller`

Contains REST controllers and exposes the HTTP API.

#### `application/service`

Contains business logic and application use cases.

#### `domain/repository`

Contains repository interfaces defining the persistence contracts.

#### `infrastructure/repository`

Contains the concrete JDBC implementations.

#### `dto`

Contains request and response data transfer objects.

#### `config`

Contains application configuration such as database, CORS, and other Spring configuration.

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

The current schema contains the following tables and views.

## `users`

Stores player accounts.

```text
users
├── id
├── username
├── email
├── password_hash
├── is_guest
└── created_at
```

## `game_sessions`

Stores completed game sessions.

```text
game_sessions
├── id
├── user_id
├── game_mode
├── score
├── total_coins
├── duration_seconds
└── created_at
```

## `player_progress`

Stores persistent player progression.

```text
player_progress
├── user_id
├── game_mode
├── current_level
├── current_table
└── last_played_at
```

## `leaderboard`

Database view used to generate leaderboard results.

Guest users are excluded from the leaderboard.

---

# Database Migrations

Database schema changes are managed with **Flyway**.

Migration files are located under:

```text
src/main/resources/db/migration/
```

Current migration:

```text
V1__initial_schema.sql
```

Flyway automatically applies pending migrations when the application starts.

---

# Configuration

Application configuration is located in:

```text
src/main/resources/application.yaml
```

Database credentials should not be committed to the repository.

Use environment variables for sensitive configuration.

## Environment Variables

The backend currently requires:

```env
DB_PASSWORD=<Supabase database password>
```

The database URL, username, and other configuration values are defined in `application.yaml`.

### Example

```yaml
spring:
  datasource:
    url: jdbc:postgresql://<host>:5432/<database>
    username: <username>
    password: ${DB_PASSWORD}
```

> Never commit real database passwords, API keys, tokens, or other secrets to Git.

---

# Running the Application

## Clone the Repository

```bash
git clone <repository-url>
cd <repository-directory>
```

## Configure Environment Variables

Set the database password before starting the application.

### Linux / macOS

```bash
export DB_PASSWORD="your-password"
```

### Windows PowerShell

```powershell
$env:DB_PASSWORD="your-password"
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

The API is versioned under:

```text
/api/v1
```

## Users

### Create User

```http
POST /api/v1/users
```

Creates a guest or registered user.

### Get User

```http
GET /api/v1/users/{userId}
```

Returns the requested user's information.

### Update User

```http
PUT /api/v1/users/{userId}
```

Updates user information.

### Delete User

```http
DELETE /api/v1/users/{userId}
```

Deletes the user and associated data according to the configured database constraints.

---

## Game Sessions

### Finish Game

```http
POST /api/v1/games/finish
```

Stores a completed game session.

### Get Player Coins

```http
GET /api/v1/games/player/coins?userId={userId}
```

Returns the player's current accumulated coins.

### Get Leaderboard

```http
GET /api/v1/games/leaderboard
```

Optional parameters:

```text
limit
offset
gameMode
```

Example:

```http
GET /api/v1/games/leaderboard?limit=10&offset=0&gameMode=training
```

---

## Player Progress

### Get Progress

```http
GET /api/v1/games/progress?userId={userId}
```

Returns the player's current progression.

### Update Progress

```http
POST /api/v1/games/progress
```

Updates the player's level and/or multiplication table depending on the selected game mode.

---

## Health Check

The backend exposes a health endpoint:

```http
GET /health
```

This endpoint can be used to verify that the API and database connection are working correctly.

Example:

```bash
curl http://localhost:8080/health
```

---

# CORS

CORS is currently configured for local frontend development.

Allowed origins include:

```text
http://localhost:5500
http://192.168.1.49:5500
```

This allows the Phaser development server to communicate with the Spring Boot API.

When deploying the application, the production frontend origin must be added to the CORS configuration.

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

Passwords are never stored in plain text.

## Input Validation

Request DTOs are validated before reaching the business logic.

## Database Constraints

The database uses constraints to protect data integrity, including:

* Foreign keys
* Cascade deletion
* Non-negative score validation
* Non-negative coin validation
* User/game relationships

## Guest Separation

Guest users are identified using:

```text
is_guest
```

This allows the application to distinguish temporary players from registered users.

---

# Testing

The backend has been tested using both automated application tests and manual API testing.

## Automated Tests

Run the test suite with:

```bash
./gradlew test
```

## Manual API Testing

The REST API has also been tested using Postman.

The following endpoints have been verified:

| Endpoint                     | Method | Status |
| ---------------------------- | ------ | ------ |
| `/api/v1/users`              | POST   | ✅      |
| `/api/v1/users/{userId}`     | GET    | ✅      |
| `/api/v1/users/{userId}`     | PUT    | ✅      |
| `/api/v1/users/{userId}`     | DELETE | ✅      |
| `/api/v1/games/finish`       | POST   | ✅      |
| `/api/v1/games/player/coins` | GET    | ✅      |
| `/api/v1/games/leaderboard`  | GET    | ✅      |
| `/api/v1/games/progress`     | GET    | ✅      |
| `/api/v1/games/progress`     | POST   | ✅      |
| `/health`                    | GET    | ✅      |

## Edge Cases Tested

| Case                                            | Status |
| ----------------------------------------------- | ------ |
| Empty leaderboard                               | ✅      |
| Guest users excluded from leaderboard           | ✅      |
| First game initializes coin total               | ✅      |
| Guest registration preserves existing game data | ✅      |
| Invalid input validation                        | ✅      |
| User CRUD operations                            | ✅      |

---

# Frontend Integration

The backend is designed to be consumed by the Phaser 3 frontend.

The frontend communicates with the API using HTTP requests.

Development configuration:

```text
Frontend
http://localhost:5500

Backend
http://localhost:8080
```

The frontend should use the API base URL:

```text
http://localhost:8080/api/v1
```

For production, this URL must be replaced with the deployed backend URL.

---

# Dependencies

## Runtime Dependencies

```kotlin
implementation("org.springframework.boot:spring-boot-starter-web")
implementation("org.springframework.boot:spring-boot-starter-jdbc")
implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
implementation("org.jetbrains.kotlin:kotlin-reflect")
runtimeOnly("org.postgresql:postgresql")
implementation("org.mindrot:jbcrypt:0.4")
implementation("org.flywaydb:flyway-core:10.21.0")
implementation("org.flywaydb:flyway-database-postgresql:10.21.0")
```

## Test Dependencies

```kotlin
testImplementation("org.springframework.boot:spring-boot-starter-test")
testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

---

# Development Workflow

A typical development workflow is:

```text
1. Start PostgreSQL / Supabase
           ↓
2. Configure DB_PASSWORD
           ↓
3. Start Spring Boot
           ↓
4. Flyway applies migrations
           ↓
5. Backend starts on port 8080
           ↓
6. Start Phaser frontend
           ↓
7. Frontend communicates with /api/v1
```

---

# Current Status

The backend currently provides the core persistence layer required by the game.

| Feature                           | Status     |
| --------------------------------- | ---------- |
| User creation                     | ✅ Complete |
| Guest users                       | ✅ Complete |
| Registered users                  | ✅ Complete |
| Guest → Registered conversion     | ✅ Complete |
| User CRUD                         | ✅ Complete |
| Password hashing                  | ✅ Complete |
| Game session persistence          | ✅ Complete |
| Coin persistence                  | ✅ Complete |
| Leaderboard                       | ✅ Complete |
| Leaderboard pagination            | ✅ Complete |
| Leaderboard game-mode filtering   | ✅ Complete |
| Player progression                | ✅ Complete |
| Audio settings                    | ✅ Complete |
| Flyway migrations                 | ✅ Complete |
| PostgreSQL / Supabase integration | ✅ Complete |
| Input validation                  | ✅ Complete |
| Error handling                    | ✅ Complete |
| Health check                      | ✅ Complete |
| CORS                              | ✅ Complete |

---

# Roadmap

## Short-term

### JWT Authentication

Implement token-based authentication so that protected resources are associated with the authenticated user instead of relying exclusively on client-provided user IDs.

### Anti-cheat Validation

Move critical game validation to the server.

The server should validate, where possible:

* Scores
* Coins
* Game duration
* Progression
* Game session consistency

This prevents players from modifying client-side requests to artificially increase their score or currency.

### Deployment

Deploy the backend to a public hosting platform such as:

* Render
* Fly.io

### Production Frontend Integration

Update the Phaser frontend to use the deployed API.

---

## Medium-term

### Rate Limiting

Protect the API against excessive requests and abuse.

### Email Verification

Implement email ownership verification for registered accounts.

### Password Reset

Provide a secure password recovery flow.

### Game Analytics

Collect non-sensitive gameplay statistics such as:

* Session duration
* Games completed
* Tables completed
* Progression
* Game mode usage

---

# Known Limitations

| Limitation                         | Status     |
| ---------------------------------- | ---------- |
| JWT authentication not implemented | ⚠️ Planned |
| Server-side anti-cheat validation  | ⚠️ Planned |
| Rate limiting                      | ⚠️ Planned |
| Email verification                 | ⚠️ Planned |
| Password reset                     | ⚠️ Planned |
| Production deployment              | ⚠️ Planned |

The current backend should therefore be considered a **development / pre-production implementation** until authentication and server-side game validation are implemented.

---

# Security Considerations

Before deploying the backend publicly, the following items should be addressed:

* Implement JWT authentication
* Remove reliance on client-provided user identity
* Implement server-side score validation
* Implement server-side coin validation
* Configure production CORS origins
* Store all secrets exclusively in environment variables
* Enable HTTPS
* Implement rate limiting
* Review database permissions
* Review Supabase Row Level Security configuration
* Add appropriate logging and monitoring

---

# License

This project is currently a private project.

Add the appropriate license here if the project is released publicly.
