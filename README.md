# bingo-backend

Spring Boot backend for Real-Time Bingo. It owns the game rules and
authoritative game state, and publishes game events to Kafka. For
design, plans, and project history, see [DEVELOPMENT.md](DEVELOPMENT.md).
For the REST API that clients use, see [API.md](API.md); for the game
events published to Kafka, see [EVENTS.md](EVENTS.md).

## Requirements

-   Java 25+
-   Docker with Compose (the app starts Kafka from `compose.yaml`)

## Run

``` bash
./mvnw spring-boot:run
```

Once it is running:

-   Swagger UI (browse and try the REST API): <http://localhost:8080/swagger-ui.html>
-   OpenAPI spec: <http://localhost:8080/v3/api-docs>
-   Kafka UI: <http://localhost:8081>

## Test

``` bash
./mvnw test
```

Tests use an embedded Kafka broker, so they do not need Docker.
