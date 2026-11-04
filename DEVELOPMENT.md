# Real-Time Bingo: Development Notes

Design, plans, and project history. For running the backend, see
[README.md](README.md). This document covers the whole project,
including the frontend and lanceBot, and lives in `bingo-backend` for
now.

## Overview

A small multiplayer Bingo application built as a practical event-driven
systems project. Bingo is the example domain; the main goal is to
explore Java, Spring Boot, Kafka, React, real-time messaging, and
independently developed clients.

The project should start small and grow in deliberate steps. The game
server owns the rules and authoritative game state. Human players and
automated players use the same public client interface.

## Project goals

-   Build a working multiplayer game that people can join and play
    remotely.
-   Use Java and Spring Boot for the backend.
-   Use Apache Kafka to publish and consume game events.
-   Use React for the browser-based player experience.
-   Eventually build **lanceBot**, an automated player that uses the
    same API and real-time event interface as the React client.
-   Optionally give lanceBot a JavaFX desktop interface later, without
    putting game-playing logic in the UI.
-   Keep the project understandable and demonstrable as a software
    portfolio project.

Bingo is intentionally a simple domain for experimenting with
publish/subscribe messaging. The project does not depend on being a
sophisticated or feature-rich Bingo product.

## High-level architecture

``` text
                         +----------------------+
                         |   Spring Boot server |
                         |                      |
                         | Game API and rules   |
                         | Game state           |
                         | WebSocket endpoint   |
                         +----------+-----------+
                                    |
                         Kafka events / consumers
                                    |
                         +----------v-----------+
                         |        Kafka         |
                         |                      |
                         | Game events          |
                         +----------------------+

              Shared client API and live-event interface
                    /              |              \
                   /               |               \
          +-------v------+  +------v------+  +------v------+
          | React client |  |  lanceBot   |  | JavaFX UI   |
          |              |  | Java core   |  | (optional)  |
          +--------------+  +-------------+  +-------------+
```

The diagram is conceptual. The initial backend can be a **modular
monolith**: one Spring Boot application with clear internal boundaries.
Kafka provides the event pipeline; it does not need to imply multiple
backend services from day one.

### Responsibilities

**Backend (Java / Spring Boot)**

-   Own game creation, joining, lifecycle, and authoritative state.
-   Generate valid Bingo cards and call numbers without repeats.
-   Publish meaningful domain events to Kafka.
-   Consume events where needed and broadcast relevant updates to
    connected clients.
-   Validate player actions and Bingo claims on the server.
-   Provide a way for clients to retrieve the current game state,
    including after reconnecting.

**Kafka**

-   Carry backend game events such as `GameStarted`, `NumberCalled`,
    `PlayerJoined`, `BingoClaimed`, and `GameWon`.
-   Provide a concrete setting for learning producers, consumers,
    topics, keys, consumer groups, delivery behavior, and event payload
    design.
-   Keep event publication behind the backend rather than requiring
    browser clients to connect directly to Kafka.

**React client**

-   Let a person find or join a game.
-   Display the player's card, called numbers, and game status.
-   Receive live updates from the backend.
-   Submit player actions, including a Bingo claim, through the same
    public interface available to other clients.

**lanceBot (future Java application)**

-   Be a separate client application, not code embedded in the game
    server.
-   Join a game through the same API used by React.
-   Receive the same live game events through the client-facing
    real-time interface.
-   Maintain its own local view of its card and called numbers.
-   Detect winning patterns and submit claims through the normal player
    API.
-   Recover after disconnecting by fetching the current game state
    rather than assuming it received every event.

**JavaFX interface (optional future addition)**

-   Present lanceBot's card, called numbers, connection status, and
    decisions.
-   Provide controls to connect, join, start, or stop the bot.
-   Keep the bot's game logic independent of JavaFX so it can also run
    headlessly.

## Client communication

The intended client contract is:

-   **HTTP/REST** for commands and request/response operations, such as
    listing games, joining a game, retrieving state, and submitting a
    claim.
-   **WebSockets** for live server-to-client game updates. This is a
    natural fit for a multiplayer game and leaves room for client
    messages if needed.
-   **Kafka** for backend event distribution.

Server-sent events (SSE) could also support one-way live updates, but
WebSockets are the current preferred direction.

The React client and lanceBot should use the same client-facing API and
event format. They should not need to know how Kafka is implemented
internally.

## Important design principles

1.  **The server is authoritative.** Clients may track state locally for
    responsiveness, but the backend decides which numbers have been
    called and whether a claim is valid.
2.  **One event, many interested clients.** Every player in a game needs
    to see each number called for that game. Kafka consumer-group
    behavior must be designed accordingly; separate player clients
    should not accidentally compete for a single copy of each event.
3.  **Scope events to a game.** Every game event should identify its
    game, and events for one game must not leak into another. A game ID
    is a natural Kafka message key and routing identifier.
4.  **Support reconnects.** A client should be able to retrieve a
    current snapshot of the game and continue after losing its
    connection.
5.  **Keep clients independent.** React, lanceBot, and a possible JavaFX
    UI should all use the same public client contract.
6.  **Keep the first version small.** Avoid premature microservices,
    elaborate event sourcing, Kubernetes, and unnecessary
    infrastructure. Add complexity when it creates a useful learning
    opportunity.
7.  **Use clear event contracts.** Define event names, payloads, IDs,
    timestamps, and versioning deliberately. Avoid making clients depend
    on internal Java classes.

## Repository layout

Each application is an independent Git repository. Locally, all three
are kept side by side in one working directory:

``` text
bingo/
├── bingo-backend/            # Java / Spring Boot / Kafka application; holds this document
├── bingo-frontend/           # React browser application (created with Vite)
└── bingo-lancebot/           # Future Java bot client; JavaFX may be added here
```

Each application should have its own README with its setup and run
instructions.

## Suggested development milestones

### 1. Define the game and build a local UI

-   Use standard US 75-ball Bingo rules.
-   Generate a 5 × 5 card with B, I, N, G, O column ranges and a free
    center square.
-   Support marking a square and detecting a local winning pattern.
-   Use temporary/sample called numbers; no backend is required yet.

### 2. Build the backend game API

-   Create and manage games.
-   Allow players to join.
-   Generate and assign cards.
-   Implement a number caller that never calls the same number twice in
    one game.
-   Expose a current-state endpoint.
-   Test game rules independently of the UI.

### 3. Add Kafka

-   Publish domain events, beginning with `GameStarted` and
    `NumberCalled`.
-   Consume those events in the backend.
-   Use a backend WebSocket handler to broadcast relevant updates to
    players.
-   Ensure events are associated with the correct game.
-   Experiment with consumer groups and delivery/recovery behavior.

### 4. Connect the React client

-   Join a game through the API.
-   Fetch the initial game snapshot.
-   Listen for live updates.
-   Display and mark the player's card.
-   Submit Bingo claims to the server.

### 5. Build lanceBot as a separate client

-   Reuse the same API and WebSocket contract as React.
-   Implement the bot's card tracking and winning-pattern detection.
-   Submit claims just as a human player would.
-   Test with multiple bot instances and human clients.
-   Add reconnect and state-recovery behavior.

### 6. Add optional JavaFX presentation

-   Build a desktop view around the existing bot core.
-   Show the card, called numbers, and bot status.
-   Keep the core usable without a graphical interface.

### 7. Polish and deploy

-   Add persistence if needed for games and player state.
-   Add automated tests, useful logs, and health checks.
-   Containerize the applications and local infrastructure.
-   Deploy a demo that lets multiple people join a game.

## Initial scope and non-goals

For the first playable version:

-   One game mode: standard 75-ball Bingo.
-   Multiple players in a game.
-   One authoritative number caller per game.
-   One card per player.
-   Basic winning patterns and server-side claim validation.
-   Live number updates to all players.

Defer user accounts, chat, payments, tournaments, elaborate statistics,
mobile apps, production-scale clustering, and sophisticated bot
strategies until the core game works.

## Early technical notes

-   A 75-ball card has five columns: B = 1--15, I = 16--30, N = 31--45,
    G = 46--60, and O = 61--75. The center square is free.
-   A number must not be called more than once in a game.
-   A player's local card state is not proof of a win. The server should
    validate a claim against the assigned card and authoritative
    called-number history.
-   A late-joining or reconnecting client needs a state snapshot before
    it can reliably process new live events.
-   If Kafka is partitioned by game ID, events for a given game can be
    kept ordered within a partition. The design should still account for
    consumer behavior and duplicate delivery.
-   Do not expose Kafka credentials or direct Kafka access to browser
    clients.

## Open design questions

-   **Reconnects and missed events.** Consider a per-game `sequence`
    number on every event and on the snapshot. A client would subscribe
    first, fetch the snapshot, and drop any event at or below the
    snapshot's sequence.
-   **State and events disagreeing.** If game state is updated and the
    Kafka publish then fails, the two no longer match. Decide whether
    that is acceptable for this project and what the server does when it
    happens.
-   **More than one backend instance.** Players are WebSocket clients,
    not Kafka consumers. A single instance needs no special handling,
    but instances sharing a consumer group would each receive only some
    events, so players connected to one would miss numbers. Each
    instance would need its own group.

## Working style

Build this incrementally and understand each layer before adding the
next. AI coding tools can help explain, review, and suggest code, but
the goal is to learn the architecture and be able to explain the design
decisions---not simply generate a finished application.

## Future Deployment Plan (Tentative)

The initial goal is to develop and test all applications locally. Once the core game is functional, the next milestone will be deploying a publicly accessible demonstration.

The current preferred deployment approach is:

| Component        | Potential Hosting                 | Notes                                                                                          |
| ---------------- | --------------------------------- | ---------------------------------------------------------------------------------------------- |
| `bingo-frontend` | Cloudflare Pages                  | Host the React/Vite static frontend with HTTPS and GitHub-based deployments.                   |
| `bingo-backend`  | Oracle Cloud Infrastructure (OCI) | Run the Spring Boot application on an Always Free VM, subject to availability and eligibility. |
| Apache Kafka     | OCI VM                            | Initially run Kafka alongside the backend using Docker Compose.                                |
| `bingo-lancebot` | Local machine / OCI               | Run as a separate client application. Cloud deployment is optional.                            |

### Proposed Architecture

* **Cloudflare Pages:** Publicly serve the React application.
* **Oracle Cloud:** Host the backend and Kafka on a Linux VM, potentially using Docker Compose.
* **WebSockets:** Provide real-time communication between the backend and connected players.
* **REST API:** Provide the common interface for React and lanceBot clients.
* **GitHub:** Maintain independent repositories and support automated build and deployment workflows.

### Flexibility and Future Considerations

This is an initial deployment concept, not a fixed infrastructure requirement. Hosting providers, service placement, and deployment architecture may change as the project evolves.

Potential alternatives include managed Kafka services, other cloud providers, self-hosted development infrastructure, or different hosting arrangements for individual applications.

The public demonstration should prioritize reliability and availability so prospective users and employers can access it without depending on local development machines or home infrastructure.

Self-hosting (such as elane-server) remains an option for development, experimentation, and testing, but is not the preferred location for the primary public demonstration.

The deployment strategy should prioritize:

* Staying within free-tier allowances where practical.
* Keeping hosting costs low and avoiding unnecessary paid services.
* Maintaining independent deployment of the frontend, backend, and bot.
* Using Docker to make local development and cloud deployment more consistent.
* Avoiding unnecessary dependence on any particular cloud provider.
* Allowing infrastructure to evolve without requiring significant application changes.

Cloud deployment is a later milestone. Local development and a working multiplayer game take priority.

