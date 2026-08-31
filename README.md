# Spring Todo API

An authenticated Spring Boot REST API for personal todos, backed by PostgreSQL and Redis.

## Run locally

Copy the example configuration, provide PostgreSQL, Redis, and Auth0 values, then start the app:

```shell
cp dev.properties.example dev.properties
./gradlew bootRun
```

The API is available at `http://localhost:8080`; its readiness endpoint is `/actuator/health/readiness` and Swagger UI is at `/swagger-ui.html`.

## Verify

```shell
./gradlew clean check bootJar
```

See [API usage](docs/api.md), [architecture](docs/architecture.md), and [operations](docs/operations.md) for details. Keep credentials in the ignored `dev.properties` file or platform-managed environment variables.
