# Operations guide

## Deployment checklist

1. Provision PostgreSQL and Redis in Railway.
2. Set the database, Redis, and Auth0 variables documented in the root [README](../README.md#configuration).
3. Confirm `NIXPACKS_JDK_VERSION=21` is present in the Railway environment if the platform does not import the repository variable automatically.
4. Deploy from the repository root. Nixpacks builds the Gradle project and starts `build/libs/spring-todo-api.jar`.
5. Check `/actuator/health`, then `/actuator/health/readiness`.
6. Complete the Auth0 login flow and verify the todo endpoints with an authenticated session.

## Health endpoints

- `/actuator/health` reports an overall status without exposing component details.
- `/actuator/health/liveness` answers whether the process is alive.
- `/actuator/health/readiness` includes dependency readiness and should be used for traffic routing.

Only `health` and `info` are exposed over HTTP. Do not expose the full Actuator surface on a public deployment without adding explicit access controls.

## Configuration failures

| Symptom | Likely cause | Check |
| --- | --- | --- |
| Application fails during startup with a datasource error | Missing or malformed JDBC URL/credentials | `SPRING_DATASOURCE_URL`, username, and password |
| Readiness is `DOWN` for Redis | Redis URL or service reference is missing | `REDIS_URL` or `REDIS_DATASOURCE_URL` |
| OAuth2 login does not start | Auth0 issuer or client settings are wrong | `AUTH0_ISSUER_URI`, `AUTH0_CLIENT_ID`, and `AUTH0_CLIENT_SECRET` |
| Flyway validation fails | Existing schema differs from the mapped entity | Compare the database schema with `V1__create_todos.sql` before changing `ddl-auto` |
| Railway cannot find the JAR | Build/start command or artifact name changed | `build/libs/spring-todo-api.jar` |

## Database changes

Add one forward-only migration under `src/main/resources/db/migration` using the `V2__description.sql` naming convention. Run `./gradlew check` before deployment. For destructive or large changes, use an expand/contract rollout and take a database backup before applying the migration.

Do not restore `spring.jpa.hibernate.ddl-auto: update` as a workaround. Schema changes belong in reviewed, repeatable migration files.

## Rollback

For an application-only regression, redeploy the previous known-good application revision. Do not automatically roll back a database migration: database changes are forward-only unless a reviewed compensating migration exists. If a migration fails, keep the application revision visible, inspect the Flyway error, and repair the migration or schema deliberately.

## Logs and diagnostics

The default log level is `INFO`. Avoid enabling verbose security or SQL logging in production unless it is time-bounded and contains no secrets or personal data. The most useful first checks are:

```shell
curl https://your-service.example/actuator/health
curl https://your-service.example/actuator/health/readiness
```

For a local dependency and build diagnosis:

```shell
./gradlew dependencies
./gradlew test --info
./gradlew bootJar
```
