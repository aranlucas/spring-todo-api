# Spring Todo API

An Auth0/OIDC-authenticated REST API for personal todos, backed by PostgreSQL and Redis.
See the [API contract](docs/api.md), [operations guide](docs/operations.md), and [feature roadmap](ROADMAP.md).

## Run locally

Use Java 21, PostgreSQL, Redis, and [Portless](https://github.com/vercel-labs/portless).
Copy the configuration and fill in the database, Redis, and Auth0 values before starting:

```shell
cp dev.properties.example dev.properties
make dev
```

`make dev` runs the Gradle `bootRun` task through Portless. Register
`https://spring-todo-api.localhost/login/oauth2/code/auth0` as the Auth0 callback.
The API is at `https://spring-todo-api.localhost`; sign in via `/oauth2/authorization/auth0`.
Without Portless, run `./gradlew bootRun` and register the callback at
`http://localhost:8080/login/oauth2/code/auth0` instead.

## Configuration

[dev.properties.example](dev.properties.example) lists the required database, Redis,
and Auth0 settings. The app imports `dev.properties` from the working directory;
keep it ignored and store deployment credentials in the hosting platform's secret store.

## Verify

```shell
./gradlew check bootJar
```

The default HTTP integration tests use H2 in PostgreSQL mode, the production Flyway
migration, an in-memory cache, and test OIDC principals; they do not contact Auth0.
CI also runs the HTTP suite against disposable PostgreSQL and Redis services,
including the Redis serialization and TTL test. To check the HTTP contracts
against disposable local PostgreSQL and Redis instances:

```shell
TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/todo_test \
TEST_DATABASE_USERNAME=todo_test \
TEST_DATABASE_PASSWORD=local-test-only \
TEST_CACHE_TYPE=redis \
TEST_REDIS_URL=redis://localhost:6379/15 \
./gradlew test --tests '*TodosHttpIntegrationTests' --rerun-tasks
```

Use an empty, dedicated test database and Redis database: the suite migrates the
database, deletes todo rows, and clears the `todos` cache between tests. With Redis
enabled, it also checks serialization, owner-scoped cache keys, and the ten-minute TTL.

Licensed under the [MIT License](LICENSE).
