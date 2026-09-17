# MatheNauTicS Backend — Security Remediation Report

**Assessment reference:** MatheNauTicS Penetration Test Report (2026-09-16 to 2026-09-17)
**Remediation period:** 2026-09-17
**Scope:** Backend (`SergiFuentes/MatheNauTicS-BE`)
**Related report:** Frontend companion report in `SergiFuentes/MatheNauTicS`
**Status:** Complete — all in-scope findings verified, deployment validated in production

---

## 1. Executive Summary

This report documents the remediation of the backend-side findings raised in the MatheNauTicS penetration test. All changes were implemented, tested locally against PostgreSQL 16.4, and verified end-to-end against the production deployment (Render + Supabase PostgreSQL 17.6).

**Summary of the backend remediation:**

| Category | Findings | Status |
|---|---|---|
| Fully remediated (backend principal) | F-05, F-07, F-08, F-12, F-15, F-18, F-20, F-25 | Verified |
| Partially mitigated (backend principal) | F-01a, F-01b, F-01c, F-04, F-24 | Verified with documented limitations |
| Out of scope (tracked) | F-09, F-19, F-22 | Not addressed |

**Key deliverables:**

- Server-side validation of all client-controlled game inputs.
- JWT revocation via a database-backed `token_version`.
- Idempotency for `/games/finish` enforced by a database UNIQUE constraint.
- Password change requires re-authentication via `currentPassword`.
- Generic registration errors to eliminate account enumeration.
- Flyway migration `V10__2026_09_17.sql` applied to production.
- 27 files changed; unit, integration, and controller tests updated.

**Commit reference:** `416b299` on `main` (PR #10).

---

## 2. Scope

### In scope (backend)

- REST API under `/api/v1`
- Authentication and JWT issuance
- Authorization and object ownership
- Game session persistence
- Player progress persistence
- Leaderboard queries
- Input validation on all user-controlled fields
- Database schema and migrations

### Out of scope

- Frontend JavaScript execution (covered in the frontend companion report)
- Supabase infrastructure configuration
- Render infrastructure configuration
- Rate limiting (tracked as F-09, F-22)
- Content-Security-Policy (tracked as F-19)

---

## 3. Findings Remediation Matrix

| ID | Severity | Title | Mitigation | Verification | Status |
|---|---|---|---|---|---|
| F-01a | HIGH | Client-controlled score | `score` bounded to `[0, 1_000_000]` in `GameSessionServiceImpl` | `POST /games/finish` with `score=999999999` returns 400 | Partially mitigated |
| F-01b | HIGH | Client-controlled coins | `coinsEarned` bounded to `[0, 1_000]` per session | `POST /games/finish` with `coinsEarned=999999` returns 400 | Partially mitigated |
| F-01c | LOW | Client-controlled duration | `durationSeconds` bounded to `[0, 3_600]` | `POST /games/finish` with `durationSeconds=999999` returns 400 | Verified |
| F-04 | HIGH | Replay of `/games/finish` | `sessionToken` UUID required + `UNIQUE` constraint on `game_sessions.session_token` + idempotent handling in `JdbcGameSessionRepository` | Two identical requests with same token → single row (`COUNT(*) = 1`) | Partially mitigated |
| F-05 | MEDIUM | Stale JWT after guest conversion | `token_version` incremented on conversion; `JwtAuthenticationFilter` compares JWT claim against database value | Pre-conversion JWT returns 401 after conversion | Verified |
| F-07 | MEDIUM | PII in backend logs | Removed `println` statements from `UserServiceImpl` | Code review | Verified |
| F-08 | MEDIUM | Username / email enumeration | Duplicate registration mapped to generic `REGISTRATION_FAILED` | Duplicate username → 409 `REGISTRATION_FAILED`; duplicate email → 409 `REGISTRATION_FAILED` | Verified |
| F-12 | MEDIUM | Unbounded `currentLevel` | `currentLevel` bounded to `[1, 13]` in `GameSessionServiceImpl.updatePlayerProgress` | `POST /games/progress` with `currentLevel=999999` returns 400 | Verified |
| F-15 | LOW | Free-form `difficulty` | `difficulty` restricted to `easy`, `normal`, `pro` | `POST /games/progress` with `difficulty=hacker` returns 400 | Verified |
| F-18 | MEDIUM | No JWT revocation | `token_version` claim compared against `users.token_version` on every authenticated request | JWT issued before a credential change returns 401 | Verified |
| F-20 | MEDIUM | Password change without current password | `currentPassword` required and BCrypt-verified before any password update | 400 without `currentPassword`; 401 with incorrect; 200 with correct | Verified |
| F-24 | LOW | BCrypt 72-byte truncation | Password validated to be ≤ 72 bytes UTF-8 before hashing | 100-byte password rejected at registration with 400 | Partially mitigated |
| F-25 | LOW | Permissive email regex | Stricter regex requiring real TLD (`[A-Za-z]{2,}`) and restricted local-part | `a@b.c` rejected with 400 | Verified |

---

## 4. Database Changes

### 4.1 Migration V10

New Flyway migration applied to production:

```sql
-- V10__2026_09_17.sql
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE game_sessions
    ADD COLUMN IF NOT EXISTS session_token UUID;

UPDATE game_sessions
SET session_token = gen_random_uuid()
WHERE session_token IS NULL;

ALTER TABLE game_sessions
    ALTER COLUMN session_token SET NOT NULL;

ALTER TABLE game_sessions
    ADD CONSTRAINT game_sessions_session_token_unique UNIQUE (session_token);
```
### 4.2 Backward compatibility
`users.token_version` defaults to 0, matching the value assumed by JwtService.extractTokenVersion when the claim is absent in existing JWTs.

Historical `game_sessions` rows are backfilled with `gen_random_uuid()`, so they satisfy the new `NOT NULL` and `UNIQUE` constraints.

No data is deleted or transformed. The migration is additive.

### 4.3 Applied to production
The migration was applied during the Render deployment for commit 416b299. Verification on Supabase:

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;
-- 10 | 2026 09 17 | t

SELECT COUNT(*) AS total,
       COUNT(*) FILTER (WHERE token_version = 0) AS zeros,
       COUNT(*) FILTER (WHERE token_version IS NULL) AS nulls
FROM users;
-- zeros = total, nulls = 0

SELECT COUNT(*) AS total,
       COUNT(*) FILTER (WHERE session_token IS NULL) AS nulls,
       COUNT(DISTINCT session_token) AS distinct_tokens
FROM game_sessions;
-- nulls = 0, distinct_tokens = total
```

## 5. Technical Changes
### 5.1 Game integrity (F-01a/b/c, F-04)
`GameSessionServiceImpl.finishGame` now validates all incoming bounds before touching the database:

```kotlin
if (request.score < 0) throw IllegalArgumentException("Score cannot be negative")
if (request.score > MAX_SCORE) throw IllegalArgumentException("Score exceeds the maximum allowed value")
if (request.coinsEarned < 0) throw IllegalArgumentException("Coins earned cannot be negative")
if (request.coinsEarned > MAX_COINS_PER_SESSION) throw IllegalArgumentException("Coins earned exceed the maximum allowed value")
if (request.durationSeconds < 0) throw IllegalArgumentException("Duration cannot be negative")
if (request.durationSeconds > MAX_DURATION_SECONDS) throw IllegalArgumentException("Duration exceeds the maximum allowed value")
if (request.gameMode !in VALID_GAME_MODES) throw IllegalArgumentException("Invalid game mode")
```

`GameResultRequest` now includes a required `sessionToken: UUID`.

`JdbcGameSessionRepository.saveGameSession` treats a repeated `sessionToken` as an idempotent re-submission: it returns the existing session instead of inserting a new one. Concurrent submissions with the same token are resolved by catching `DataIntegrityViolationException` and re-reading the winning row.

### 5.2 JWT revocation (F-05, F-18)
`JwtService` now includes a tokenVersion claim:

```kotlin
fun generateToken(userId: UUID, username: String, isGuest: Boolean, tokenVersion: Int = 0): String
```
`JwtService.extractTokenVersion` returns 0 when the claim is absent, preserving backward compatibility with tokens issued before the migration.

`JwtAuthenticationFilter` now takes UserRepository as a dependency and validates the token version on every authenticated request:

```kotlin
val currentVersion = userRepository.findTokenVersionById(userId)
if (currentVersion != null && currentVersion == tokenVersion) {
    // Authentication proceeds
}
```
`UserServiceImpl` increments `token_version` on:

- Guest-to-registered conversion

- Password change

- AuthServiceImpl.login includes the current tokenVersion in every newly issued JWT.

### 5.3 Credential handling (F-20, F-24, F-25)
`UserUpdateRequest` gained `currentPassword: String?`.

`UserServiceImpl.updateUser` verifies `currentPassword` against the stored **BCrypt** hash before applying any password change:

```kotlin
if (request.password != null) {
    val currentPassword = request.currentPassword
        ?: throw IllegalArgumentException("Current password is required to change the password")
    if (!BCrypt.checkpw(currentPassword, credentials.passwordHash)) {
        throw InvalidCredentialsException()
    }
    validatePassword(request.password)
}
```
validatePassword enforces the BCrypt input limit in bytes:
```
require(password.toByteArray(Charsets.UTF_8).size <= MAX_PASSWORD_BYTES) {
    "Password exceeds the maximum length of $MAX_PASSWORD_BYTES bytes"
}
```
`isValidEmail` uses a stricter regex requiring a real TLD.

### 5.4 Logging hygiene (F-07)
Removed `println` calls in `UserServiceImpl` that exposed usernames, emails, and password-adjacent data.

### 5.5 Enumeration protection (F-08)
GlobalExceptionHandler maps both `UsernameAlreadyExistsException` and `EmailAlreadyExistsException` to a single generic response:

```
@ExceptionHandler(UsernameAlreadyExistsException::class, EmailAlreadyExistsException::class)
fun registrationConflict(ex: RuntimeException): ResponseEntity<ApiError> =
    error(HttpStatus.CONFLICT, "REGISTRATION_FAILED", "Registration failed")
```
### 5.6 Progress validation (F-12, F-15)
`GameSessionServiceImpl.updatePlayerProgress` validates currentLevel ≤ 13 and difficulty ∈ {easy, normal, pro}.

## 6. Partially Mitigated Findings
These findings have a real control in place, but the control does not eliminate the underlying issue. This section documents honestly what remains.

### 6.1 F-01a / F-01b / F-01c — Client-controlled game results
What is now protected:

Bounds on `score`, `coinsEarned`, and `durationSeconds`.

`gameMode` restricted to adventure or training.

What is NOT protected:

The client remains authoritative for values inside the accepted bounds. A caller can submit `score=500000` or `coinsEarned=900` if those values are plausible for the game domain.

Server-side verification of the actual gameplay would require a redesigned protocol (server-issued game session, signed replay, or input-stream verification), which is out of scope.

### 6.2 F-04 — Replay of /games/finish
What is now protected:

A repeated sessionToken produces no new session row and no additional reward.

Concurrent submissions are resolved atomically at the database level.

What is NOT protected:

The client can generate a fresh sessionToken and submit a fabricated result within the bounds enforced by F-01a/b. Idempotency prevents duplication, not fabrication.

### 6.3 F-24 — BCrypt 72-byte truncation
What is now protected:

Passwords whose UTF-8 representation exceeds 72 bytes are rejected at registration, login-boundary validation, and password change.

What is NOT protected:

The **jbcrypt library** still truncates internally. If the validation layer is bypassed, truncation still occurs.

### 7. Out-of-Scope Findings
These findings were raised in the pentest and remain unaddressed:

F-09 — Login rate limiting. Not implemented.

F-19 — Content-Security-Policy. Not implemented.

F-22 — Guest creation rate limiting. Not implemented.

These are documented as follow-up work and do not affect the closure of the other findings.

## 8. Verification Evidence
### 8.1 Local verification against PostgreSQL 16.4
All findings were re-tested locally before deployment:

|Test	|Expected	| Observed| 
|---|---|---|
|POST /games/finish with score=999999999	|400	|400 Score exceeds the maximum allowed value
|POST /games/finish with coinsEarned=999999	|400	|400 Coins earned exceed the maximum allowed value
|POST /games/finish with durationSeconds=999999	|400	|400 Duration exceeds the maximum allowed value
|POST /games/progress with currentLevel=999999	|400	|400 Level exceeds the maximum allowed value
|POST /games/progress with difficulty=hacker	|400	|400 Invalid difficulty
|PUT /users/me {"password":"..."} without currentPassword	|400	|400 Current password is required...
|PUT /users/me with wrong currentPassword	|401	|401 INVALID_CREDENTIALS
|POST /users with email a@b.c	|400	|400 Valid email is required
|Duplicate username registration	|409	|409 REGISTRATION_FAILED
|Duplicate email registration	|409	|409 REGISTRATION_FAILED
|POST /users with 100-byte password	|400	|400 Password exceeds the maximum length of 72 bytes
|Two POST /games/finish with same sessionToken	|1 row	|1 row
|Pre-conversion JWT after conversion	|401	|401

### 8.2 Production smoke test
Executed against https://mathenautics-be.onrender.com on 2026-09-17. 24 of 24 assertions passed. 

Coverage:

* Guest creation and game completion.

* GET /users/me, GET /games/player/coins, POST /games/finish.

* Leaderboard guest exclusion.

* Guest-to-registered conversion with token rotation.

* Username change without token rotation.

* Password change with currentPassword in all three scenarios (missing / wrong / correct).

* Token invalidation after password change.

* Login with rotated credentials.

* Progress update and read-back.

* Account deletion and leaderboard cleanup.

* Token invalidation after deletion.

### 8.3 Deployment verification
* **Render** build logs confirm Successfully applied 1 migration to schema "public".

* **Supabase** `flyway_schema_history` contains version 10 with success = true.

* `/health` returns `{"status":"UP","database":"connected"}`.

## 9. Residual Risk
* **Game-result trust:** the client remains authoritative for values within the accepted bounds. Not addressed by this remediation.

* **Rate limiting:** absent on /auth/login and /users. Could enable credential attacks at low volume and guest creation abuse.

* **Content-Security-Policy:** absent on the backend. Mitigated in practice by the absence of user-generated HTML, but no defense-in-depth.

* **Statelessness:** JWT authentication now performs one extra database read per authenticated request. HikariCP pool size remains at 3. This is a deliberate trade-off to enable revocation without an external cache.

## 10. References
* Penetration test report: MatheNauTicS-Pentest-Report.md (internal document, provided separately (2026-09-16 to 2026-09-17))

* Commit: 416b299 on main

* Pull request: [PR #10](https://github.com/SergiFuentes/MatheNauTicS-BE/pull/10)

* Migration: `V10__2026_09_17.sql`

* Frontend companion report: SergiFuentes/MatheNauTicS — SECURITY_REMEDIATION_REPORT.md

## 11. Conclusion
The backend remediation closes all in-scope findings with reproducible evidence. Three findings (F-01, F-04, F-24) are honestly documented as partially mitigated because the underlying protocol would require a larger redesign to be fully eliminated. These are recorded in Section 6 and carried forward as residual risks.

The system as deployed in production is measurably more robust than before the remediation: unbounded input is rejected, tokens can be revoked, credentials require re-authentication for sensitive operations, and account enumeration is eliminated at the registration endpoint.

No behavioral regressions were observed in the 24-assertion end-to-end smoke test executed against production.