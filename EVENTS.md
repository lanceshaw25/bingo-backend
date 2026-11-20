# Game events

The backend publishes every change to a game as a JSON event on Kafka.
Browser clients and lanceBot will not read Kafka directly; they will get
these events over WebSockets (not built yet). This document is the event
contract either way.

## Topic

| | |
|---|---|
| Topic | `bingo.game-events` |
| Partitions | 3 |
| Key | the game ID (string) |
| Value | JSON, described below |
| Headers | none |

Kafka sends every message with the same key to the same partition and
keeps each partition in order. Because the key is the game ID, **one
game's events are always read in the order they happened**. Different
games can be on different partitions and read in parallel.

To watch events locally, run the app and open Kafka UI at
<http://localhost:8081>.

## Common fields

``` json
{
  "type": "NumberCalled",
  "eventId": "acc83945-ef80-4dab-b179-56f2b615d9c1",
  "gameId": "630c69ea-330d-4773-b80a-8586f3584b32",
  "sequence": 4,
  "occurredAt": "2026-11-19T22:42:17.913529820Z",
  "number": 3,
  "column": "B",
  "calledCount": 1
}
```

| Field | Meaning |
|---|---|
| `type` | Which event this is. Decides the other fields. |
| `eventId` | Unique per event. A consumer that sees the same ID twice can drop the duplicate. |
| `gameId` | The game. Same as the message key. |
| `sequence` | 1 for a game's first event, then 2, 3, ... with no gaps. |
| `occurredAt` | When the server published the event (ISO-8601, UTC). |

Player IDs are secret (see [API.md](API.md)), so no event contains one.
Players are identified by name.

## Event types

| `type` | When | Extra fields |
|---|---|---|
| `GameCreated` | A game is created | none |
| `PlayerJoined` | A player joins | `name` |
| `GameStarted` | The game starts | `players`: names, in join order |
| `NumberCalled` | A number is called | `number`, `column` (`B`-`O`), `calledCount` (including this one) |
| `GameWon` | A claim wins; the game is finished | `winner` (name), `lines` (as in the claim response) |

Claims that do not win, and the end of the numbers, publish nothing yet.

## Using `sequence` to stay in sync

The REST game view (`GET /api/games/{gameId}`) has the same `sequence`:
the last event its state includes. To start following a game, or to
recover after disconnecting:

1.  Start receiving events, and hold on to them.
2.  Fetch the game view. Say its `sequence` is 12.
3.  Drop held and new events with `sequence` 12 or lower; they are
    already in the view. Apply 13 and up, in order.

Receiving first means nothing can be missed between the two steps. If a
`sequence` is ever skipped (13 then 15), an event was lost; fetch the
game view again and continue from its `sequence`.

## Delivery

Events are sent asynchronously after the game's state changes. The
Kafka producer retries failed sends for up to two minutes. If Kafka is
unavailable longer than that, the event is logged and lost, and the game
carries on; clients recover with the steps above. The game's state on
the server is always the source of truth.
