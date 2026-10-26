# Feature roadmap

Planning baseline: `d6c76de1c43d` on `origin/main`, 2026-10-26. This remains a small owner-scoped API; the roadmap does not assume a public consumer application.

## Outcome

Support the complete lifecycle of a personal task. The [current API](docs/api.md) creates, lists, reads and deletes todos through an OIDC session. It already has PostgreSQL persistence, pagination and owner-scoped caching.

## 1. Complete and reopen a task

Add a completion state and an owner-scoped update operation. Return the updated resource so a client can display the confirmed state.

Acceptance:

- A user can complete and reopen their own task without deleting its text.
- Another user's ID remains indistinguishable from a missing resource.
- Repeating the same state update has a predictable result.
- A cached read after an update returns the current state.
- Existing records migrate to an explicit incomplete state without data loss.

Validate through the existing HTTP integration tests and a simple authenticated API walkthrough. No new frontend is required to prove the contract.

## 2. Find the tasks that matter now

Add a completion filter and a defined default ordering to the existing paginated list. Introduce due dates only with an explicit date/timezone contract.

Acceptance: filters and pagination remain owner-scoped; repeated queries have stable ordering, and completion does not make tasks disappear without a selectable view.

## 3. Edit without silently losing another edit

Allow text correction with an explicit version or conditional-update contract. A stale client receives a recoverable conflict and can reload the current task.

## Boundaries and evidence

Keep the modular monolith, Spring Data, Bean Validation, Flyway and Spring Cache. Do not add collaboration, notifications, an MCP server or a mobile client without a real consumer need. Verify security filters, database migration and cache invalidation through the existing native tests.

The first value signal is that a consumer can use the task lifecycle without implementing delete-and-recreate workarounds. This is a bounded learning/service project, with no measured adoption or revenue claim.
