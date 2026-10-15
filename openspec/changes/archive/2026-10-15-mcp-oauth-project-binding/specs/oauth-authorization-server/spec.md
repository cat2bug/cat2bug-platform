## MODIFIED Requirements

### Requirement: Authorization code flow with mandatory PKCE

`GET /oauth/authorize` SHALL require `response_type=code`, a registered `client_id`, an exactly matching registered `redirect_uri`, and a `code_challenge` with method `S256`. Valid requests MUST be sent to a consent page that requires login with account and password; invalid client or redirect URI MUST show an error page and MUST NOT redirect to the supplied URI. On the consent page the user MUST select a team and project (see `mcp-project-binding`) before approving. Authorization codes MUST be single-use, expire within 10 minutes, and be bound to client, redirect URI, user, selected project, code challenge, and the `resource` parameter.

#### Scenario: User approves access

- **WHEN** a logged-in user selects a project and approves a valid authorization request
- **THEN** the browser is redirected to the registered redirect URI with `code` and the original `state`

#### Scenario: User denies access

- **WHEN** the user clicks "deny"
- **THEN** the browser is redirected with `error=access_denied` and the original `state`

#### Scenario: Missing PKCE is rejected

- **WHEN** an authorization request has no `code_challenge`
- **THEN** the request is rejected with `invalid_request`

#### Scenario: Not logged in

- **WHEN** a user who is not logged in opens a valid authorization request
- **THEN** the login page is shown first and, after logging in with account and password, the consent page for the same request is shown

#### Scenario: Approve without project

- **WHEN** a decision request approves without a project
- **THEN** no authorization code is issued and the request fails

### Requirement: OAuth tokens are managed like personal tokens

`POST /oauth/revoke` SHALL revoke a given access or refresh token; revoking a refresh token MUST revoke every token of that authorization. Disabling or deleting a user MUST make their OAuth tokens stop working.

#### Scenario: Client logs out

- **WHEN** a client posts its refresh token to `/oauth/revoke`
- **THEN** both the access token and the refresh token of that authorization stop working

#### Scenario: Disabled user's token stops working

- **WHEN** an administrator disables a user who has an authorized MCP client
- **THEN** the next MCP request with that client's access token receives HTTP 401

## ADDED Requirements

### Requirement: OAuth access tokens authenticate only on MCP endpoints

OAuth access tokens SHALL be accepted only on MCP endpoints via `Authorization: Bearer <token>`. Web APIs such as `/system/**` MUST continue to accept only the web session JWT. A token that does not exist, is expired, is revoked, has no bound project, or belongs to a disabled or deleted user MUST be rejected with HTTP 401 without revealing which condition failed. Tokens MUST be stored only as SHA-256 hashes, and each successful authentication SHALL update the token's last-used time and client IP.

#### Scenario: Token on web API is rejected

- **WHEN** a request with a valid OAuth access token calls `/system/defect/list`
- **THEN** the request is treated as unauthenticated (JSON `code` 401)

#### Scenario: Personal token prefix is rejected

- **WHEN** a request to `/mcp` carries a bearer value starting with `c2b_pat_`
- **THEN** it receives HTTP 401
