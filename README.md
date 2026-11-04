# bingo-backend

Spring Boot backend for Real-Time Bingo. It owns the game rules and
authoritative game state, and publishes game events to Kafka. For
design, plans, and project history, see [DEVELOPMENT.md](DEVELOPMENT.md).

## Requirements

-   Java 25+
-   Docker with Compose (the app starts Kafka from `compose.yaml`)

## Run

``` bash
./mvnw spring-boot:run
```

Kafka UI is available at <http://localhost:8081>.

## Test

``` bash
./mvnw test
```

Tests use an embedded Kafka broker, so they do not need Docker.
