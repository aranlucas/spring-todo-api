# Untangle decision log — 2026-11-06

## Scope and baseline inventory (before edits)

Read all tracked application, test, build, CI, deployment and documentation files,
including AGENTS.md. Starting commit: d6aa1f6; worktree clean. No earlier refactor
findings or edits were present. Both comment and test cleanup are selected.
Tests and builds run only in CI per the user's instruction; no local baseline
execution was attempted. Recent GitHub CI runs were successful.

The application is small: one colocated todo feature (HTTP DTOs/controller,
transactional cached service, JPA entity/repository), security configuration,
OpenAPI/cache configuration, one Flyway migration, repository/HTTP/architecture
tests, Gradle, Nixpacks and concise operational/API documentation. Keep that
layout; introducing more layers would make routine changes harder.

## Decisions and problems addressed

- Make the service own todo creation; keep the controller focused on HTTP input/output.
  - Hidden link: the controller constructs persistence entities while the service
    is a pass-through save wrapper.
  - Considered centralizing missing-resource handling in the service, then rejected
    it during review: changing a cached Optional return to a required Todo would
    turn previously cached misses into nulls and cause HTTP 500s during rollout.
    Keep Optional lookup and controller 404 mapping for compatibility.
- Validate actual persistence/cache integrations in the existing CI job with
  disposable PostgreSQL and Redis services, after the default H2 checks.
  - Hidden link: production requires PostgreSQL/Redis but CI used only substitutes.
  - Special case: the Redis serialization/TTL test is enabled by an environment
    variable that CI never set. Retain its conditional for the H2 run and execute
    it in the production-integration run.
- Remove unused JVM option configuration; Nixpacks remains the runtime authority.
  - Surplus: jvm.options duplicates Nixpacks JAVA_OPTS; no repository consumer
    references it and Java does not automatically load that file.
- Keep architecture verification, remove diagram generation from its test.
  - Needless link: a dependency-boundary assertion also writes undocumented,
    unconsumed PlantUML output. Keep the meaningful verify() assertion.

## Intentional special cases and retained boundaries

- OIDC email is the established ownership key; migrating to subject identifiers
  would change ownership semantics and needs a separate data migration.
- Owner-scoped deletes distinguish zero affected rows to implement documented 404.
- Flyway baseline V1 supports existing databases and is documented operationally.
- Public health, login and OpenAPI paths are deliberate security policy.
- Dependency version overrides document external security advisories; keep them.
- H2/Redis test profile switches provide a fast default and real-service CI checks.
- Keep Spring Cache annotations, Bean Validation and Spring Data rather than custom
  cache, validation or persistence machinery. No new libraries or scripts needed.
- Retain all behavioral tests: owner isolation, cached creation/read/deletion,
  pagination, validation boundaries, login/CSRF, schema startup, and Redis TTL.
  They catch credible regressions; none merely repeats implementation logic.
- No redundant implementation comments were found in application/test sources.
  Remove generated Dependabot setup comments and inline descriptions of YAML keys.

## Validation and review

Round 1 static review found the cached-null rollout risk above and redundant
Dependabot comments. Both addressed; no other worthwhile structural issues found.
Behavioral tests are preserved. Round 2 independently re-reviewed the repository
and resolved findings: no worthwhile issues remain. CI pending on the PR.
No local tests or builds run.
