# MarketFlow Saga

Backend portfolio project that simulates a supermarket sale flow with event-driven microservices and choreography-based SAGA.

## Current Status

The `inventory-service` is implemented as the first service slice:

- Administrative REST endpoints for products and stock adjustments.
- Stock reservation, confirmation and release compensation rules.
- SNS event publication with the standard MarketFlow event envelope.
- SQS listener with idempotency through `processed_events`.
- PostgreSQL schema managed by Flyway.
- Floci init script for SNS, SQS, DLQ and subscription filter policy.
- Unit tests for reservation success, failure, compensation and duplicate event handling.

## Tech Stack

- Java 21
- Spring Boot 3.3.2
- Spring Data JPA
- PostgreSQL
- Flyway
- AWS SDK v2 for SNS/SQS
- Floci for local AWS emulation
- JUnit 5 and Mockito

## Inventory Service

```bash
cd services/inventory-service
mvn test
mvn spring-boot:run
```

By default the service expects PostgreSQL at `jdbc:postgresql://localhost:5432/inventory_db` and Floci at `http://localhost:4566`.

Key endpoints:

- `POST /products`
- `GET /products`
- `GET /products/{id}`
- `POST /products/{id}/stock-adjustments`
