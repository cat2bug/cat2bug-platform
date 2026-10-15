# mcp-project-binding Specification

## Requirements
### Requirement: User selects a team and project when authorizing an MCP client

The OAuth consent page SHALL require the logged-in user to pick exactly one project, listed grouped by team, before approving. The project list MUST contain only projects where the user is a member and MCP is enabled; a system administrator MAY select any project with MCP enabled, consistent with the web UI. Approval MUST be rejected by the server when the submitted project is not in that list. Projects without MCP enabled MUST NOT be selectable, and the page MUST explain that a project administrator has to enable MCP first when no project is selectable.

#### Scenario: Approve with a selected project

- **WHEN** a user logs in, selects team "team-12" and project "project-58" on the consent page, and clicks allow
- **THEN** the client is redirected back with an authorization code bound to project "project-58"

#### Scenario: No selectable project

- **WHEN** the user belongs to no project that has MCP enabled
- **THEN** the consent page shows no selectable project, the allow button is disabled, and a hint tells the user to ask a project administrator to enable MCP

#### Scenario: Tampered project is rejected

- **WHEN** a decision request submits a project the user is not a member of, or whose MCP is disabled
- **THEN** no authorization code is issued and the request fails

### Requirement: Tokens are bound to the authorized project

Authorization codes, access tokens, and refresh tokens SHALL record the project selected at consent time. Refreshing MUST keep the same project. An OAuth token without a bound project MUST be rejected on the MCP endpoint with HTTP 401.

#### Scenario: Refresh keeps the project

- **WHEN** a client refreshes its tokens
- **THEN** the new access token is bound to the same project as the old one

#### Scenario: Legacy token without project

- **WHEN** an OAuth access token issued before this change (no bound project) is used on `/mcp`
- **THEN** the request receives HTTP 401 and the client starts a new OAuth login

### Requirement: MCP calls are limited to the bound project

Every MCP tool and generic operation SHALL act only on the project bound to the access token. Tools MUST NOT accept a project argument. If the bound project later has MCP disabled, or the user is no longer a member, calls MUST be refused with a message telling the user to ask an administrator or re-authorize.

#### Scenario: Tool uses the bound project

- **WHEN** a client authorized for project "project-58" calls `search_defects` without any project argument
- **THEN** only defects of project "project-58" are returned

#### Scenario: Removed from project

- **WHEN** the user is removed from the bound project and then calls a tool
- **THEN** the call is refused and no data is returned

#### Scenario: Switching project requires re-authorization

- **WHEN** the user wants the AI client to work on another project
- **THEN** they have to log in and authorize again in the client, selecting the other project
