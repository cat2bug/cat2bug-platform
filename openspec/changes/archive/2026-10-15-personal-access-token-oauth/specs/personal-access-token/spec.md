## ADDED Requirements

### Requirement: Users manage their own personal access tokens

A logged-in user SHALL be able to create, list, and revoke their own personal access tokens from the profile page. Creation MUST accept a name and an optional expiry. The plaintext token MUST be returned only once at creation; afterwards only the name, prefix, source, creation time, expiry, and last-used information are visible.

#### Scenario: Create a token

- **WHEN** a user creates a token named "Cursor" with a 90-day expiry
- **THEN** the response contains a token starting with `c2b_pat_`, and the token list afterwards shows "Cursor" with its prefix and expiry but not the full token

#### Scenario: Users cannot see others' tokens

- **WHEN** user A requests the token list
- **THEN** only tokens owned by user A are returned, including tokens issued through OAuth to user A

#### Scenario: Revoke a token

- **WHEN** a user revokes one of their tokens
- **THEN** subsequent requests using that token are rejected with HTTP 401

### Requirement: Tokens are stored as hashes

The system MUST store only a SHA-256 hash and a short display prefix of each token. The plaintext token MUST NOT be stored, logged, or returned by any list or detail API.

#### Scenario: Database contains no plaintext

- **WHEN** a token is created and the token table is inspected
- **THEN** no column contains the plaintext token

### Requirement: Tokens authenticate only on MCP endpoints

Personal access tokens and OAuth access tokens SHALL be accepted only on MCP endpoints via `Authorization: Bearer <token>`. Web APIs such as `/system/**` MUST continue to accept only the web session JWT.

#### Scenario: Token on MCP endpoint

- **WHEN** a request with a valid personal access token reaches the MCP endpoint
- **THEN** the request is authenticated as the token owner

#### Scenario: Token on web API is rejected

- **WHEN** a request with a valid personal access token calls `/system/defect/list`
- **THEN** the request is treated as unauthenticated (JSON `code` 401)

### Requirement: Invalid tokens are rejected uniformly

A token that does not exist, is expired, is revoked, or belongs to a disabled or deleted user MUST be rejected with HTTP 401 on MCP endpoints, without revealing which condition failed.

#### Scenario: Disabled user's token stops working

- **WHEN** an administrator disables a user who owns a valid token
- **THEN** the next MCP request with that token receives HTTP 401

### Requirement: Token usage is recorded

Each successful authentication with a token SHALL update the token's last-used time and client IP.

#### Scenario: Last used time is shown

- **WHEN** a token is used to call the MCP endpoint and the owner opens the token list
- **THEN** the token shows the time and IP of that call
