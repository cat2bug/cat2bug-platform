# mcp-endpoint Specification

## Requirements
### Requirement: Remote MCP endpoint over Streamable HTTP

The system SHALL expose an MCP endpoint at `/mcp` using the Streamable HTTP transport, authenticated only by OAuth access tokens sent as `Authorization: Bearer <token>`. Clients MUST NOT need a local proxy, and users MUST NOT need to copy any token: the client discovers the authorization server through the 401 challenge and opens the browser for login.

#### Scenario: Client connects through OAuth login

- **WHEN** an MCP client is configured with only the `/mcp` URL and connects for the first time
- **THEN** it receives HTTP 401 with `resource_metadata`, completes OAuth login in the browser, and afterwards initialization succeeds and the tool list is returned

### Requirement: Protected resource metadata and 401 challenge

The system SHALL serve `GET /.well-known/oauth-protected-resource` with `resource` set to the public MCP URL and `authorization_servers` pointing to the platform issuer. Requests to `/mcp` without a valid token MUST receive HTTP 401 with a `WWW-Authenticate` header containing `resource_metadata` pointing to that document.

#### Scenario: Unauthenticated request triggers OAuth discovery

- **WHEN** a client calls `/mcp` without a token
- **THEN** it receives HTTP 401 and a `WWW-Authenticate` header whose `resource_metadata` URL returns the protected resource metadata

#### Scenario: Token for another resource is rejected

- **WHEN** an OAuth access token issued with a different `resource` value is used on `/mcp`
- **THEN** the request receives HTTP 401

### Requirement: Effective permission is role permission intersected with project MCP modules

For every project-scoped tool or operation, the system SHALL compute the user's permissions in the target project from their project and team roles, without changing the user's current project in the web UI, and intersect them with the permission prefixes of modules selected in that project's MCP settings. A call MUST be refused when the project has MCP disabled, the user is not a member, the module is not selected, or the role lacks the required permission.

#### Scenario: Role allows but module is unchecked

- **WHEN** a developer who can edit defects calls a defect write tool on a project where the "defect" module is unchecked
- **THEN** the call fails with a message that the module is not enabled for MCP

#### Scenario: Module is checked but role lacks permission

- **WHEN** an outsider role without defect pass permission calls the pass-defect tool on a project with all modules checked
- **THEN** the call fails with a permission error, the same as on the web

#### Scenario: Web current project is untouched

- **WHEN** a user is viewing project A in the browser and their AI client operates on project B through MCP
- **THEN** the browser session's current project remains A

### Requirement: MCP operations are audited with channel and client

Every write made through MCP SHALL be recorded in the operation log and, for defects, in the defect log with channel `mcp` and the client name reported at MCP initialization. The web UI MUST show these entries as made "via MCP" together with the client name.

#### Scenario: Defect repaired through MCP

- **WHEN** a user repairs a defect using Claude Code
- **THEN** the defect log entry shows the user, the repair action, and "via MCP (Claude Code)"

### Requirement: MCP calls are rate limited per user

The system MUST limit MCP calls per user within a time window, with the limit configurable.

#### Scenario: Excessive calls are throttled

- **WHEN** a user exceeds the configured MCP call limit within one minute
- **THEN** further calls fail with a rate-limit error until the window passes
