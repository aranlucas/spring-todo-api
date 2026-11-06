# Correctness and reliability audit — 2026-11-06

## Inventory before implementation

Baseline: current remote default `844e304`; isolated worktree clean. Read all tracked
application, tests, migrations, build, CI, deployment and documentation files and
AGENTS.md. No local tests or builds run; CI is the sole execution environment.

Public interfaces: OIDC browser login/logout, CSRF-protected `/todos` CRUD and
pagination, public OpenAPI and health probes. HTTP DTOs hide owner email. The
controller obtains ownership from OIDC email, service transactions use Spring Data
JPA, and single-record reads/creates/deletes use Spring Cache with Redis and a
10-minute TTL. Flyway V1 and Hibernate validation govern PostgreSQL schema.
Nixpacks builds/starts the Java 21 application; Gradle CI checks/packages and runs
HTTP contracts against H2 and disposable PostgreSQL/Redis. No JavaScript or
TypeScript subproject exists: tester-army/e2e is inapplicable; retain meaningful
JUnit HTTP protocol regressions rather than introducing a UI or npm runtime.

## Findings recorded before edits

1. **Missing ownership claim reaches persistence (high confidence, medium impact).**
   `TodosController` passes `principal.getEmail()` directly on every path. OIDC email
   is optional even when the email scope is requested. A valid login with no email
   can reach POST, which attempts a NULL insert into `todos.email NOT NULL` and
   produces a server failure. Blank emails are accepted as a shared ownership key.
   Expected: deny todo access when no usable ownership key exists, without writes.
   Preserve established email ownership semantics; do not migrate to subject IDs.
2. **Cache publication is not coordinated with commit (high confidence, medium impact).**
   `CacheConfig` enables caching but the auto-configured Redis manager is not
   transaction-aware. A service call inside a larger transaction can publish a
   created todo immediately, then roll back the database transaction; a subsequent
   lookup returns the nonexistent todo for up to ten minutes. Expected: rolled-back
   changes never enter Redis. Use the existing Spring Cache transaction support.

## Other paths and rejected hypotheses

- Owner predicates cover lists, reads and deletes; owner and ID compose cache keys.
  Existing tests cover another owner's cached record and attempted deletion.
- Missing deletes are deliberately 404; no update or concurrent-edit API exists.
  POST is not documented as idempotent; adding an idempotency key is a new feature.
- Bean Validation bounds todo content; DTOs prevent mass assignment of IDs/owners.
- CSRF and unauthenticated access already have meaningful regressions. Public health
  and docs and email-based identity are documented policy, not audit defects.
- Flyway baseline adoption and readiness limitations are explicitly documented;
  changing deployment policy without deployment evidence is outside this audit.
- No custom retries or unmanaged resources exist. Database/cache outages surface
  errors; distributed read/delete cache races and post-commit Redis outages remain
  limitations of this cache architecture, not claimed fixed by deferred publication.

## Validation plan

Add HTTP regressions denying unusable owner claims, with no rows saved, and a
transaction rollback regression proving a created record cannot survive in cache.
Keep all existing tests. CI runs regressions against H2 and real PostgreSQL/Redis,
and separately replays the regressions against baseline application sources to
prove they catch the defects. Independent review must challenge findings and fixes.

## Independent review and additional finding

Independent whole-repository static review confirmed both findings and identified
one additional concrete request-validation defect before its fix: `GET /todos?sort=doesNotExist`
passes a nonexistent entity property into Spring Data, producing a
`PropertyReferenceException` and HTTP 500 instead of a client error. Map that
specific exception to HTTP 400 within the todo controller and preserve valid sort
behavior with an HTTP regression. Reviewer found no further concrete defects.

## CI follow-up

First CI run (42307023657) rejected an outdated exception import at compile time.
Corrected it to Spring Data 4's `org.springframework.data.core.PropertyReferenceException`
and adjusted regression formatting. Tests/build have not passed yet; all execution
remains in CI. Independent reviewer completed two static passes with no remaining
concrete correctness findings.
