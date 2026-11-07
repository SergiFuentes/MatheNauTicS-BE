# REST API Reference

**Base URL (production):** `https://mathenautics-be.onrender.com/api/v1`  
**Content type:** JSON (`application/json`) for request and response bodies unless noted.  
**Authentication:** JWT bearer token on protected endpoints.

This reference documents the HTTP contract implemented by the current controllers and DTOs. It is not an OpenAPI-generated specification. Validation and business rules may also be enforced in services and database constraints.

## Conventions

### Authentication

Protected requests must include:

```http
Authorization: Bearer <token>
```

Tokens are issued by user creation or login, signed with HS256, and expire after 24 hours. There is no refresh-token endpoint. User-scoped operations derive the user ID from the token; clients must not select another user by sending a user ID.

### Game identifiers

- `gameMode`: used for game-mode-specific operations; currently `adventure` and `training` are documented by the application.
- `minigameId`: optional on session start, finish, progress, and save creation; defaults to `default`.
- A game session must be started before it is finished. Start returns a `sessionToken`, which is supplied to finish.

### Error response

Handled application errors use this shape:

```json
{
  "status": 409,
  "code": "GAME_SAVE_LIMIT_REACHED",
  "message": "Human-readable explanation",
  "timestamp": "2026-11-07T12:00:00Z"
}
```

The `message` and `timestamp` vary. The following error codes are explicitly mapped by the global exception handler:

| HTTP | Code | Meaning |
|---|---|---|
| 400 | `INVALID_REQUEST` | An argument or request value was rejected |
| 401 | `INVALID_CREDENTIALS` | Login credentials are invalid |
| 404 | `USER_NOT_FOUND` | User was not found |
| 404 | `GAME_SAVE_NOT_FOUND` | Save was not found for the authenticated user |
| 409 | `REGISTRATION_FAILED` | Username or email conflicts with an existing account |
| 409 | `GUEST_CONVERSION_FAILED` | Guest conversion could not be completed |
| 409 | `DUPLICATE_SESSION` | Session result conflicts with a previously submitted result |
| 409 | `GAME_SAVE_LIMIT_REACHED` | User already has the maximum of 10 saves |

Spring Security may reject missing/invalid authentication before the controller runs. Do not assume every framework-level error uses the application error schema.

## Endpoint summary

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/auth/login` | Public | Log in |
| POST | `/users` | Public | Create guest or registered account |
| GET | `/users/me` | Bearer | Get current account |
| PUT | `/users/me` | Bearer | Update current account |
| POST | `/users/me/convert` | Bearer | Convert guest to registered account |
| DELETE | `/users/me` | Bearer | Delete current account |
| POST | `/games/start` | Bearer | Start game session |
| POST | `/games/finish` | Bearer | Finish game session and record result |
| GET | `/games/player/coins` | Bearer | Get current player's coin total |
| GET | `/games/leaderboard` | Public | List leaderboard entries |
| GET | `/games/progress` | Bearer | Read progress |
| POST | `/games/progress` | Bearer | Create/update progress |
| GET | `/games/saves` | Bearer | List current player's saves |
| POST | `/games/saves` | Bearer | Create save |
| GET | `/games/saves/{saveId}` | Bearer | Read save |
| PUT | `/games/saves/{saveId}` | Bearer | Update save |
| DELETE | `/games/saves/{saveId}` | Bearer | Delete save |
| GET | `/health` | Public | Health and database connectivity check |
| GET | `/` | Public | Basic service banner |

The paths above are relative to the production base URL, except `/health` and `/`, which are root-level endpoints on `https://mathenautics-be.onrender.com`.

## Authentication and accounts

### POST `/auth/login`

Request:

```json
{ "identifier": "username-or-email", "password": "your-password" }
```

`identifier` accepts the username or email.

Success: `200 OK`

```json
{
  "userId": "uuid",
  "username": "player",
  "email": "player@example.com",
  "isGuest": false,
  "token": "jwt"
}
```

Invalid credentials return `401` with code `INVALID_CREDENTIALS`.

### POST `/users`

Creates a guest or registered user. The controller returns `200 OK`.

Request:

```json
{
  "username": "player",
  "email": "player@example.com",
  "password": "password",
  "isGuest": false
}
```

`isGuest` defaults to `true` when omitted. The response contains `userId`, `username`, `email`, `isGuest`, and `token`.

### GET `/users/me`

Returns `id`, `username`, `email`, `createdAt`, and `isGuest`.

### PUT `/users/me`

All fields are optional; only supplied fields are candidates for update. Request DTO:

```json
{
  "username": "new-name",
  "email": "new@example.com",
  "password": "new-password",
  "currentPassword": "current-password"
}
```

The response contains account fields and a `token`. Service-level rules determine which combinations are accepted.

### POST `/users/me/convert`

Converts the authenticated guest account using the same request shape as `POST /users`. The target account is determined by the bearer token, and existing game data is intended to be preserved. Returns the account fields and token.

### DELETE `/users/me`

Deletes the authenticated account. Success: `204 No Content`.

## Game sessions, coins, and leaderboard

### POST `/games/start`

Starts a session for the authenticated player.

Request:

```json
{ "gameMode": "adventure", "minigameId": "default" }
```

`minigameId` defaults to `default`.

Success: `200 OK`

```json
{
  "sessionToken": "uuid",
  "gameMode": "adventure",
  "expiresAt": "2026-11-07T12:00:00Z",
  "minigameId": "default"
}
```

### POST `/games/finish`

Submits a result for a previously started session.

Request:

```json
{
  "gameMode": "adventure",
  "score": 100,
  "coinsEarned": 5,
  "durationSeconds": 120,
  "sessionToken": "uuid-from-start",
  "minigameId": "default"
}
```

`minigameId` defaults to `default`.

Success: `200 OK`

```json
{ "sessionId": 123, "userId": "uuid", "score": 100, "totalCoins": 25 }
```

**Integrity note:** the current API accepts `coinsEarned` from the client. A session token binds a result to a started session, but it does not independently prove the number of coins collected in the game. Treat coin totals as client-reported rather than tamper-proof.

### GET `/games/player/coins`

Returns the authenticated player's total:

```json
{ "userId": "uuid", "totalCoins": 25 }
```

### GET `/games/leaderboard?limit=10&offset=0&gameMode=adventure`

Public endpoint. `limit` defaults to `10`, `offset` to `0`, and `gameMode` is optional. Returns a JSON array of entries:

```json
[
  {
    "username": "player",
    "score": 100,
    "totalCoins": 25,
    "createdAt": "2026-11-07T12:00:00Z",
    "gameMode": "adventure"
  }
]
```

Guest accounts are excluded from leaderboard results.

## Progress

### GET `/games/progress?gameMode=adventure&minigameId=default`

Requires `gameMode`; `minigameId` defaults to `default`. Returns the current player's progress:

```json
{
  "userId": "uuid",
  "gameMode": "adventure",
  "currentLevel": 1,
  "score": 0,
  "lives": 3,
  "coins": 0,
  "difficulty": "normal",
  "lastPlayedAt": "2026-11-07T12:00:00Z",
  "minigameId": "default"
}
```

### POST `/games/progress`

Request:

```json
{
  "gameMode": "adventure",
  "currentLevel": 1,
  "score": 0,
  "lives": 3,
  "coins": 0,
  "difficulty": "normal",
  "minigameId": "default"
}
```

Defaults: `score=0`, `lives=3`, `coins=0`, `difficulty=normal`, `minigameId=default`. Returns the persisted progress using the response shape above.

Progress is scoped to the authenticated user and keyed by game mode and minigame identifier in the current API contract.

## Saved games

A user can store up to 10 saves. The authenticated user is inferred from the token; save IDs cannot be used to access another user's saves.

### POST `/games/saves`

Request:

```json
{
  "gameMode": "adventure",
  "name": "Before the boss",
  "state": { "level": 3, "score": 120 },
  "minigameId": "default"
}
```

`state` is an arbitrary JSON value representing restorable game state; the example is illustrative. `minigameId` defaults to `default`.

Success: `201 Created`. Response fields: `id`, `userId`, `gameMode`, `name`, `state`, `createdAt`, `updatedAt`, `minigameId`.

### GET `/games/saves`

Returns a JSON array of the current player's saves.

### GET `/games/saves/{saveId}`

Returns one save. `saveId` must be a UUID.

### PUT `/games/saves/{saveId}`

Replaces the save's name and state:

```json
{ "name": "After the boss", "state": { "level": 4, "score": 180 } }
```

Returns the updated save.

### DELETE `/games/saves/{saveId}`

Success: `204 No Content`.

Creating a save beyond the limit returns `409 GAME_SAVE_LIMIT_REACHED`. Missing or non-owned saves are reported as `404 GAME_SAVE_NOT_FOUND`.

## Health endpoints

### GET `/health`

Public operational check. Executes a lightweight database query and returns a status/database result. Use this endpoint for service monitoring; do not treat it as a detailed diagnostics API.

### GET `/`

Returns a basic backend-running banner.

## CORS and rate limiting

CORS is configured on the backend; the allowed production origins must match the deployed clients. Browser clients cannot bypass CORS by adding request headers. Rate limiting is implemented by a backend filter; limits are configuration/implementation details and are not stated here because they can change.

## Client integration example

```js
const response = await fetch(
  "https://mathenautics-be.onrender.com/api/v1/games/progress?gameMode=adventure&minigameId=default",
  {
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "application/json"
    }
  }
);

if (!response.ok) {
  // Handle HTTP status and, when present, the API error code.
  throw new Error(`API request failed: ${response.status}`);
}

const progress = await response.json();
```

## Related operational documentation

- [Deployment and production verification](../DEPLOYMENT.md)
- [Backend README](../README.md)


## Minigame identifiers and training operations

The frontend currently exposes these minigame identifiers in the training menu:

| Menu label | Phaser scene | `minigameId` sent by the scene |
|---|---|---|
| Shooter | `MathSpaceShooterScene` | `shooter` |
| Quiz | `MathQuizScene` | `quiz` |
| Snake | `MathSnakeScene` | `snake` |
| Defense | `MathDefenseScene` | `defense` |

These are frontend identifiers used by the minigame session integration; the backend request field is still a string with a default of `default`. The API does not define a server-side enum or guarantee that arbitrary identifiers are rejected.

Keep the concepts separate:

- `gameMode` is the backend game-mode dimension (for the minigames above, the frontend sends `training`).
- `minigameId` identifies the minigame (`shooter`, `quiz`, `snake`, or `defense`).
- The selected training operation is frontend game state: `add`, `subtract`, `divide`, or `mixed`. It is not the `minigameId`.
- `multiply` is a separate training route through `TimeTableScene`; do not document it as one of the four minigame IDs.


### Example: start and finish a training minigame

For example, the Snake minigame starts a session with `gameMode: "training"` and `minigameId: "snake"`. The client receives the session token from the start response and reuses it when submitting the result:

```http
POST /api/v1/games/start
Authorization: Bearer <token>
Content-Type: application/json

{ "gameMode": "training", "minigameId": "snake" }
```

The finish request uses the returned `sessionToken`:

```json
{
  "gameMode": "training",
  "minigameId": "snake",
  "score": 300,
  "coinsEarned": 0,
  "durationSeconds": 90,
  "sessionToken": "<sessionToken-from-start>"
}
```

Use the matching `gameMode` and `minigameId` for the corresponding progress request, for example `GET /api/v1/games/progress?gameMode=training&minigameId=snake`. Replace `snake` with `shooter`, `quiz`, or `defense` for the other minigames. The selected operation (`add`, `subtract`, `divide`, or `mixed`) is not a field in the current session API payload; it is selected and used by the frontend question generator.

When checking progress or saved-game records for a minigame, use the same `gameMode` and `minigameId` consistently. The current API reference describes the request contract; it does not assert that the backend independently validates the frontend's menu combinations.
