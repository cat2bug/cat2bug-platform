## ADDED Requirements

### Requirement: Project can be located by key, ID, or Git remote

Every project-scoped tool SHALL accept a `project` argument given as `teamKey/projectKey` or project ID. The system SHALL provide `list_my_projects`, returning the user's projects that have MCP enabled together with their selected modules, and `resolve_project_by_git_remote`, mapping a Git remote URL of a repository hosted on the platform to its project and repository.

#### Scenario: Resolve project from local origin

- **WHEN** a client calls `resolve_project_by_git_remote` with the clone URL of a repository hosted on the platform
- **THEN** the response contains the project key and repository identifier

#### Scenario: Disabled projects are not listed

- **WHEN** a user calls `list_my_projects` and one of their projects has MCP disabled
- **THEN** that project is not included

### Requirement: Curated tools cover the defect lifecycle and core project data

The system SHALL provide curated tools for searching and reading defects (including logs, comments and linked commits), creating, updating, assigning, repairing, passing, rejecting, closing, reopening and commenting on defects; searching, reading, creating and updating cases; listing and reading plans, deliverables, reports and documents; and listing repositories, browsing trees, reading files, searching code, and reading commits. Each curated tool MUST apply the same business rules as the corresponding web action.

#### Scenario: Pass a defect

- **WHEN** a tester calls the pass-defect tool on a defect in "pending verification" state
- **THEN** the defect moves to the same state as when passed on the web, and the handler is notified the same way

#### Scenario: Invalid state transition

- **WHEN** a client tries to pass a defect that is still in "processing" state
- **THEN** the tool returns the same error the web action would return

### Requirement: Tools declare their risk

Each tool MUST declare MCP tool annotations: read-only tools set `readOnlyHint`, tools that modify data set `readOnlyHint` false, and tools that delete or irreversibly change data set `destructiveHint`.

#### Scenario: Client sees annotations

- **WHEN** a client lists tools
- **THEN** `search_defects` is marked read-only and `close_defect` is marked as not read-only

### Requirement: Generic gateway exposes all other project operations

The system SHALL provide `list_operations`, `describe_operation`, and `call_operation`. The operation catalog MUST be generated from the project-scoped web endpoints, excluding file upload/download, export, WebSocket and Git Smart HTTP endpoints. `list_operations` MUST return only operations the user is allowed to perform in the given project and module. `call_operation` MUST execute the original web endpoint as the user, so that permission checks, data scope, pagination, logging and error mapping are identical to the web.

#### Scenario: Operation listing respects permissions

- **WHEN** a project outsider calls `list_operations` for the defect module
- **THEN** operations requiring permissions the outsider does not have are not listed

#### Scenario: Call goes through the web endpoint rules

- **WHEN** a user calls `call_operation` for an endpoint that requires a permission they lack
- **THEN** the result is the same permission error the web endpoint returns, and nothing is changed

#### Scenario: Excluded endpoints are not callable

- **WHEN** a client calls `call_operation` with the ID of an export endpoint
- **THEN** the call fails with "operation not available through MCP"

### Requirement: Create operations support idempotency keys

`create_defect`, `create_case` and `comment_defect` SHALL accept an optional `idempotency_key`. A repeated call with the same user and key within 24 hours MUST return the original result without creating another record.

#### Scenario: Retry does not duplicate a defect

- **WHEN** a client calls `create_defect` twice with the same `idempotency_key`
- **THEN** only one defect exists and both calls return its ID
