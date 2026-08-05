# boutique-checkoutservice-java

Orchestrates cart, inventory, order and payment services during checkout.

## Overview

- **Type:** Spring Boot service
- **Stack:** Java 21, Spring Boot, Maven, Actuator, Docker
- **Port:** `8086`

## Flow

```text
Client / service → Controller → Business logic → Database / events / downstream services
```

## Configuration

```text
CART_SERVICE_URL
DB_CONNECTION_TIMEOUT_MS
DB_MAX_LIFETIME_MS
DB_POOL_MAX_SIZE
DB_POOL_MIN_IDLE
DB_VALIDATION_TIMEOUT_MS
DEPLOYMENT_ENVIRONMENT
MAVEN_USER_HOME
```

## Run

```bash
./mvnw spring-boot:run
./mvnw clean verify
```

## Docker

```bash
docker build -t boutique-checkoutservice-java:local .
```

## Health

```bash
curl http://localhost:8086/actuator/health
```

## CI/CD

This repository is built and deployed independently through its own GitHub Actions workflow.
