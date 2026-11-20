# Bingo REST API

The HTTP contract shared by every client: the React frontend, lanceBot,
and anything else. Live updates over WebSockets are not built yet; until
then, clients poll `GET /api/games/{gameId}`.

All requests and responses are JSON. IDs are UUIDs.

The running server also describes itself: the OpenAPI spec is at
`/v3/api-docs`, and Swagger UI at `/swagger-ui.html` lets you try each
endpoint from the browser. This document covers the concepts the spec
does not, such as why player IDs are secret.

## Player IDs are secret

Joining a game returns a `playerId`. It is the only thing that lets a
client fetch that player's card or claim Bingo for them, so treat it
like a password:

-   The public game view lists players by **name only**, never by ID.
-   Keep the ID in the client that joined. Use it to reconnect.

This is a stand-in for real authentication, which is not built yet.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/games` | Create a game |
| `GET` | `/api/games` | List games |
| `GET` | `/api/games/{gameId}` | Public game state |
| `POST` | `/api/games/{gameId}/players` | Join a game |
| `GET` | `/api/games/{gameId}/players/{playerId}` | Your card (after reconnecting) |
| `POST` | `/api/games/{gameId}/start` | Start the game |
| `POST` | `/api/games/{gameId}/players/{playerId}/claims` | Claim Bingo |

### Create a game

`POST /api/games` → `201 Created`, with a `Location` header for the new
game. The body is a [game](#game).

### List games

`GET /api/games` → `200 OK`

``` json
[
  {"id": "51e4cb87-...", "status": "WAITING_FOR_PLAYERS", "playerCount": 2, "calledCount": 0}
]
```

### Get a game

`GET /api/games/{gameId}` → `200 OK`, a [game](#game).

### Join a game

`POST /api/games/{gameId}/players`

``` json
{"name": "Lance"}
```

→ `201 Created`, with a `Location` header for the player. The body is a
[player](#player), including the `playerId` to keep.

Names are trimmed and must be unique in the game, ignoring case. Players
can only join before the game starts.

### Get your player

`GET /api/games/{gameId}/players/{playerId}` → `200 OK`, a
[player](#player). Use this to get your card back after reconnecting.

### Start a game

`POST /api/games/{gameId}/start` → `200 OK`, the [game](#game), now
`IN_PROGRESS`. The game needs at least one player. Anyone can start a
game for now.

The server then calls a number every `bingo.call-interval` (5 seconds by
default), starting one interval after the start.

### Claim Bingo

`POST /api/games/{gameId}/players/{playerId}/claims` (no body) → `200 OK`

``` json
{"result": "WON", "lines": ["ROW_3", "DIAGONAL_UP"]}
```

| `result` | Meaning |
|---|---|
| `WON` | Valid claim. You won and the game is finished. `lines` lists your complete lines. |
| `NO_BINGO` | Your card has no complete line yet. No penalty; the game continues. |
| `GAME_NOT_IN_PROGRESS` | The game has not started, or someone else already won. |

The server checks the claim against your card and the numbers it has
called. It does not use anything the client has marked.

Line names: `ROW_1`-`ROW_5` (top to bottom), `COLUMN_B`, `COLUMN_I`,
`COLUMN_N`, `COLUMN_G`, `COLUMN_O`, `DIAGONAL_DOWN` (top left to bottom
right), and `DIAGONAL_UP` (bottom left to top right).

## Response types

### Game

``` json
{
  "id": "51e4cb87-a77a-4e86-9b57-52002ac308aa",
  "status": "IN_PROGRESS",
  "players": ["Lance", "Bot"],
  "calledNumbers": [70, 72, 30],
  "winner": null,
  "sequence": 7
}
```

-   `status`: `WAITING_FOR_PLAYERS`, then `IN_PROGRESS`, then `FINISHED`.
-   `calledNumbers`: in the order they were called. The last one is the
    most recent.
-   `winner`: the winner's name, or `null`.
-   `sequence`: the number of the last game event this state includes.
    Live events carry the same number; see [EVENTS.md](EVENTS.md).

### Player

``` json
{
  "playerId": "d971fc99-2258-4381-9308-78af20419ab4",
  "name": "Lance",
  "card": [
    [12, 18, 41, 60, 75],
    [15, 27, 35, 55, 64],
    [ 2, 26,  0, 58, 65],
    [ 8, 25, 40, 52, 70],
    [ 5, 23, 38, 57, 66]
  ]
}
```

`card` is five rows, top to bottom. Each row is B, I, N, G, O from left
to right. `0` is the free center square.

## Errors

Errors use the standard problem format (RFC 9457), with content type
`application/problem+json`:

``` json
{
  "status": 409,
  "title": "Not allowed in the game's current state",
  "detail": "Cannot join: game 51e4cb87-... is IN_PROGRESS, not WAITING_FOR_PLAYERS",
  "instance": "/api/games/51e4cb87-.../players"
}
```

| Status | When |
|---|---|
| `400 Bad Request` | Malformed ID or body, missing or blank name, duplicate name |
| `404 Not Found` | No such game, or no such player in the game |
| `409 Conflict` | The game's status does not allow it: joining after the start, starting twice, starting with no players |
