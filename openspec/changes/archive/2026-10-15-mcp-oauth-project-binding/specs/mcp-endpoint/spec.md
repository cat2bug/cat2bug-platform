## MODIFIED Requirements

### Requirement: Remote MCP endpoint over Streamable HTTP

The system SHALL expose an MCP endpoint at `/mcp` using the Streamable HTTP transport, authenticated only by OAuth access tokens sent as `Authorization: Bearer <token>`. Clients MUST NOT need a local proxy, and users MUST NOT need to copy any token: the client discovers the authorization server through the 401 challenge and opens the browser for login.

#### Scenario: Client connects through OAuth login

- **WHEN** an MCP client is configured with only the `/mcp` URL and connects for the first time
- **THEN** it receives HTTP 401 with `resource_metadata`, completes OAuth login in the browser, and afterwards initialization succeeds and the tool list is returned
