# Spring Todo API

An authenticated REST API for managing personal todo items. The application is a small Spring Boot service backed by PostgreSQL and Redis, with Auth0 OpenID Connect login, Actuator health probes, OpenAPI documentation, and Spring Modulith module verification.

## Features

- Auth0 OIDC login for every todo endpoint.
- User-scoped listing, reads, creation, and deletion.
- PostgreSQL persistence with Flyway migrations and Hibernate schema validation.
- Redis-backed caching for individual todo reads.
- Pageable todo listing through Spring Data Web.
- Public health probes for Railway and other platform orchestrators.
- OpenAPI JSON and Swagger UI documentation.
- Spring Modulith verification and generated module diagrams.

## Technology baseline

| Component | Version or choice |
| --- | --- |
| Java | 21 toolchain |
| Spring Boot | 4.1.1 |
| Spring Modulith | 2.1.1 |
| Gradle | 9.7.1 via the Gradle Wrapper |
| OpenAPI | springdoc-openapi 3.0.3 |
| Database | PostgreSQL |
| Cache | Redis |
| Deployment | Railway via Nixpacks |

Java 21 is intentional: Java 25 is the current LTS release, but the Railway Nixpacks Java provider currently documents Java 21 as its newest available JDK. The Gradle toolchain makes the build requirement explicit and leaves a straightforward path to Java 25 when the deployment provider supports it.

## Project structure

```text
src/main/java/com/aranlucas/todo/
├── TodoApplication.java       # Application entry point
├── config/OpenApiConfig.java  # API metadata
├── security/SecurityConfig.java
└── todo/
    ├── CreateTodoRequest.java # Validated API input
    ├── Todo.java              # JPA persistence entity
    ├── TodoRepository.java
    ├── TodoResponse.java      # API output
    ├── TodoService.java       # Transactions, ownership, and caching
    └── TodosController.java
```

See [docs/architecture.md](docs/architecture.md) for the request flow and design decisions.

## API

| Method | Path | Authentication | Purpose |
| --- | --- | --- | --- |
| `GET` | `/todos` | Auth0 login | List the current user's todos |
| `POST` | `/todos` | Auth0 login | Create a todo; returns `201 Created` |
| `GET` | `/todos/{id}` | Auth0 login | Read a current user's todo |
| `DELETE` | `/todos/{id}` | Auth0 login | Delete a current user's todo; returns `204 No Content` |
| `GET` | `/actuator/health` | Public | Application health |
| `GET` | `/actuator/health/liveness` | Public | Process liveness |
| `GET` | `/actuator/health/readiness` | Public | Dependency readiness |
| `GET` | `/swagger-ui.html` | Public | Interactive API documentation |
| `GET` | `/v3/api-docs` | Public | OpenAPI document |

`GET /todos` accepts Spring Data pagination parameters such as `page`, `size`, and `sort`. The default sort is ascending `id` order, the default page size is 20, and the maximum page size is 100.

The create request is:

```json
{
  "content": "Ship the API"
}
```

`content` must be non-blank and at most 255 characters. The authenticated user's email is assigned by the server; clients cannot choose an owner.

## Prerequisites

- JDK 21.
- PostgreSQL.
- Redis.
- An Auth0 application configured for OAuth2 login.

The Gradle Wrapper supplies Gradle 9.7.1, so a separate Gradle installation is not required.

## Local development

Copy the example configuration and fill in the Auth0 values:

```shell
cp dev.properties.example dev.properties
```

Start PostgreSQL and Redis, then run the checks and application:

```shell
./gradlew clean check
./gradlew bootRun
```

The default local port is `8080`. Verify the process and dependency health with:

```shell
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health/readiness
```

`dev.properties` is ignored by Git. Never commit client secrets, database passwords, Redis URLs, or other credentials.

## Configuration

Production configuration is supplied through environment variables. `dev.properties` is an optional local override imported by `application.yml`.

| Variable | Required | Description |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | Yes | JDBC PostgreSQL URL |
| `SPRING_DATASOURCE_USERNAME` | Yes* | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Yes* | Database password |
| `PGUSER` | Fallback | Railway PostgreSQL username |
| `PGPASSWORD` | Fallback | Railway PostgreSQL password |
| `REDIS_URL` | Yes* | Redis connection URL |
| `REDIS_DATASOURCE_URL` | Fallback | Existing Redis variable name |
| `AUTH0_CLIENT_ID` | Yes | Auth0 client ID |
| `AUTH0_CLIENT_SECRET` | Yes | Auth0 client secret |
| `AUTH0_ISSUER_URI` | Yes | Auth0 issuer, including the trailing slash |
| `PORT` | No | HTTP port; defaults to `8080` |

`*` One of the primary or fallback names must be set. Railway normally provides the `PG*` variables and can provide `REDIS_URL` through its Redis service reference.

## Persistence and migrations

Hibernate runs with `ddl-auto: validate`; it never changes production schema. Flyway owns schema changes in `src/main/resources/db/migration`.

The first migration creates the `todos` table and an owner/id index. `baseline-on-migrate` is enabled for the existing pre-Flyway deployment so a database that already contains the table is baselined instead of having the initial migration applied a second time. Review the actual production schema before deploying any future migration that changes existing columns.

## Security model

The application keeps Spring Security's CSRF protection enabled because OAuth2 login is browser-based. Health and documentation routes are public; all todo routes require an authenticated OIDC user.

Ownership is enforced in both read and write paths. A missing todo and another user's todo produce the same `404 Not Found` result, avoiding an ownership-enumeration signal. Deletes use a repository predicate on both `id` and `email`, so authorization does not depend only on a controller-side check.

## Railway deployment

The repository includes `nixpacks.toml` with a stable Spring Boot start command and `NIXPACKS_JDK_VERSION=21`. Configure the variables in the [Configuration](#configuration) table in Railway, connect PostgreSQL and Redis, and deploy from the repository root.

The deployment produces `build/libs/spring-todo-api.jar` and listens on Railway's injected `PORT`. Configure Railway health checks to use `/actuator/health/readiness` after the application has started.

See [docs/operations.md](docs/operations.md) for deployment checks, troubleshooting, and rollback guidance.

## Useful Gradle commands

| Command | Purpose |
| --- | --- |
| `./gradlew clean check` | Format check, compilation, tests, and Modulith verification |
| `./gradlew test` | Run the test suite |
| `./gradlew spotlessApply` | Apply Java and text formatting |
| `./gradlew bootJar` | Build the deployable executable JAR |
| `./gradlew bootRun` | Run the application locally |
| `./gradlew dependencies` | Inspect the resolved dependency graph |

GitHub Actions runs `./gradlew clean check bootJar` for pushes to `master` and pull requests. Dependabot checks both Gradle and GitHub Actions dependencies weekly.

## Reference documentation

- [Spring Boot 4.1.1 reference documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Security OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html)
- [Spring Modulith reference documentation](https://docs.spring.io/spring-modulith/reference/)
- [Gradle 9.7.1 user manual](https://docs.gradle.org/9.7.1/userguide/userguide.html)
- [Flyway documentation](https://documentation.red-gate.com/flyway)
- [Nixpacks Java provider](https://nixpacks.com/docs/providers/java)
- [springdoc-openapi releases](https://github.com/springdoc/springdoc-openapi/releases)
