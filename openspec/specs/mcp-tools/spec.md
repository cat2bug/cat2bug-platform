# mcp-tools Specification

## Requirements

### Requirement: Current project is reported by the bound token

The system SHALL provide `get_current_project`, returning the project bound to the access token (ID, team and project keys, names), the modules usable by the user in that project, and the project's code repositories. `resolve_project_by_git_remote` SHALL look up a repository only inside the bound project and MUST fail with a clear message when the URL belongs to another project or is not hosted on the platform. No other tool MUST accept a `project` argument.

#### Scenario: Read current project

- **WHEN** a client authorized for project "project-58" calls `get_current_project`
- **THEN** the response contains project "project-58", its usable modules, and its repositories

#### Scenario: Remote of another project

- **WHEN** a client authorized for project "project-58" calls `resolve_project_by_git_remote` with the clone URL of a repository in another project
- **THEN** the call fails and says the repository is not in the authorized project

#### Scenario: Tools list has no project argument

- **WHEN** a client lists tools
- **THEN** no tool's input schema contains a `project` property

### Requirement: get_current_project 附带仓库确认提示

`get_current_project` SHALL 在返回 JSON 中包含字符串字段 `repositoryConfirmHint`，用简短中文提示：单仓库且本地 origin 能 resolve 到该仓时不要追问仓库；基线默认用 `defaultBranch`，说明或确认基线时列出本地常见分支；多仓或 remote 对不上必须问用户。工具 `description` SHALL 含有指向该规则的简短说明（不必复述全文）。

#### Scenario: 返回含 repositoryConfirmHint
- **WHEN** 已授权用户调用 `get_current_project`
- **THEN** 响应 JSON 含非空字段 `repositoryConfirmHint`，且文案提到单仓库（或「不问」）与基线 / `defaultBranch` 之一

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
