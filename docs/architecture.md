# Architecture

Spring Todo API is intentionally a small modular monolith. It keeps the deployment model simple while preserving boundaries that can be verified as the application grows.

## Package boundaries

```text
com.aranlucas.todo
├── config      API metadata and cross-cutting configuration
├── security    OAuth2 login and HTTP authorization
└── todo        Todo HTTP, application, and persistence code
```

Spring Modulith verifies the application module arrangement in `TodoApplicationTests`. The same test writes PlantUML module diagrams under `build/spring-modulith-docs/` during a test run. Configuration remains a small cross-cutting package rather than a business module.

## Request flow

```text
HTTP request
    │
    ▼
Spring Security ── unauthenticated ──► OAuth2 login
    │ authenticated OidcUser
    ▼
TodosController
    │ validated request + principal email
    ▼
TodoService
    │ transaction + ownership + cache policy
    ├──────────────► Redis cache
    ▼
TodoRepository
    ▼
PostgreSQL
```

The controller translates between API records and the JPA entity. `CreateTodoRequest` prevents clients from supplying an owner, while `TodoResponse` keeps persistence mapping out of the wire contract.

## Ownership boundary

The authenticated email is the ownership key. List queries are filtered by email, single-item reads filter the cached result before returning it, and deletes use `deleteByIdAndEmail` so the database operation itself is scoped to the owner. Missing resources and resources owned by another user intentionally share the same not-found response.

## Persistence

Flyway owns schema creation and future migrations. Hibernate is configured only to validate the mapped schema. `open-in-view` is disabled so database access remains inside explicit service transactions instead of leaking into response serialization.

The Redis cache stores individual todo reads for ten minutes. Saves update the cache with the persisted ID; owner-scoped deletes evict only the affected ID. The application does not cache pageable owner listings, which avoids invalidating an unbounded set of list keys.

## Security boundaries

Health probes and OpenAPI resources are public so an orchestrator and an operator can determine whether the service is alive. Todo resources remain authenticated. CSRF protection stays enabled because the application uses browser OAuth2 login; if the service later becomes a non-browser-only API, revisit that decision as part of the client authentication design rather than disabling it casually.

## Extension guidance

New business capabilities should get their own direct child package under `com.aranlucas.todo`, with HTTP adapters, application services, and persistence details kept inside that module. Cross-module dependencies should be introduced deliberately and covered by the Modulith verification test.
