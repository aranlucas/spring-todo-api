# Operations

## Deployment

Provision PostgreSQL, Redis, and an Auth0 OIDC client, then set the variables in
[dev.properties.example](../dev.properties.example) through the host's secret store.
Register the deployment's `/login/oauth2/code/auth0` URL as an Auth0 callback.

[nixpacks.toml](../nixpacks.toml) selects Java 21 and starts
`build/libs/spring-todo-api.jar` on the host-provided `PORT`; without it the app
uses port 8080. Verify that the deployment uses this configuration and builds the
JAR before startup. The application trusts forwarded headers, so the ingress must
set them correctly for HTTPS login redirects and prevent client-supplied spoofing.

## Health

Health endpoints are public and hide component details. `/actuator/health` reports
aggregate health, including configured database and Redis checks.
`/actuator/health/liveness` and `/actuator/health/readiness` expose Spring Boot's
process-state probes. The current configuration does **not** explicitly include
database or Redis health in the readiness group; readiness alone does not prove
dependency availability. Confirm login and an authenticated todo read after rollout.

Only `health` and `info` are exposed through Actuator, and `info` still requires
authentication. Retain access controls when exposing additional endpoints.

## Schema changes and recovery

Add versioned migrations under `src/main/resources/db/migration` (next:
`V2__description.sql`). Hibernate validates the schema; do not switch to automatic
schema updates to bypass a migration failure. Run `./gradlew check` before deployment.

`baseline-on-migrate` is enabled at version 1: a nonempty database without Flyway
history can be marked as V1 without running the initial migration. Before adopting
an existing database, compare it with
[V1__create_todos.sql](../src/main/resources/db/migration/V1__create_todos.sql).

Back up data before destructive migrations and use an expand/contract rollout when
old and new application revisions must coexist. For an application-only regression,
redeploy the last known-good revision only if it is compatible with the current
schema. Database rollback requires a reviewed compensating migration or a deliberate
backup restore; redeploying an older JAR does not reverse migrations.

On startup failure, distinguish Flyway history/checksum errors from Hibernate schema
validation errors. Inspect the failure and current schema before changing migration
history. Avoid verbose security or SQL logs containing credentials or personal data.
