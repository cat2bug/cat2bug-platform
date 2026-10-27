## ADDED Requirements

### Requirement: Cloud site is same-origin at the apex host

The cloud UI MUST be served at `https://cloud.cat2bug.com/` with History routing base `/`. API calls MUST use the same host and the prefix `/prod-api`. WebSocket MUST use `/ws/` on the same host. The application MUST NOT require a `/cloud` path prefix.

#### Scenario: Login page and API share the host

- **WHEN** a browser opens `https://cloud.cat2bug.com/login`
- **THEN** the page loads from that origin and its API requests go to `https://cloud.cat2bug.com/prod-api/`

#### Scenario: Nginx strips the API prefix

- **WHEN** a request arrives at `/prod-api/login` on the cloud vhost
- **THEN** the backend receives the path `/login`

### Requirement: OAuth and MCP paths stay unprefixed

Paths under `/oauth/`, `/.well-known/`, and `/mcp` MUST be proxied to the backend without the `/prod-api` prefix, matching the open-source nginx vhost.

#### Scenario: OAuth token endpoint

- **WHEN** a client calls `https://cloud.cat2bug.com/oauth/token`
- **THEN** the backend receives `/oauth/token`

### Requirement: Secrets are not in the repository

DingTalk, WeChat open platform, Huawei SMS, and Huawei OBS credentials MUST be read from environment variables. The cloud configuration committed to Git MUST NOT contain those secret values.

#### Scenario: Repository config has no secrets

- **WHEN** the cloud profile configuration in Git is read
- **THEN** client secrets, SMS secrets, and OBS access keys are placeholders or environment references, not literal credentials
