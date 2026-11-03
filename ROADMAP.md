# Feature roadmap

Support the complete lifecycle of a personal task without delete-and-recreate workarounds.
Priorities are ordered; no public frontend is required.

## 1. Complete and reopen a task

Add a completion state and an owner-scoped update operation. Return the updated resource.

Acceptance:

- A user can complete and reopen their own task without deleting its text.
- Another user's ID remains indistinguishable from a missing resource.
- Repeating the same state update has a predictable result.
- A cached read after an update returns the current state.
- Existing records migrate to an explicit incomplete state without data loss.

## 2. Find the tasks that matter now

Add a completion filter to the paginated list, preserving its existing stable ordering.
Introduce due dates only with an explicit date/timezone contract.

Acceptance: filters and pagination remain owner-scoped, and completion does not make
tasks disappear without a selectable view.

## 3. Edit without silently losing another edit

Allow text correction with an explicit version or conditional-update contract.
A stale client receives a recoverable conflict and can reload the current task.

## Scope

Keep this a personal-task API. Collaboration, notifications, an MCP server, and a
mobile client need a concrete consumer need before joining the roadmap. Validate
new contracts through the existing HTTP integration tests and an authenticated walkthrough.
