## ADDED Requirements

### Requirement: Authorization server metadata is published

The system SHALL serve `GET /.well-known/oauth-authorization-server` without authentication, returning the issuer, authorization, token, registration, and revocation endpoint URLs, `code_challenge_methods_supported` containing only `S256`, `grant_types_supported` of `authorization_code` and `refresh_token`, and `token_endpoint_auth_methods_supported` of `none`. The issuer MUST be `cat2bug.public-url` when configured, otherwise derived from the request's forwarded headers or host.

#### Scenario: Metadata uses configured public URL

- **WHEN** `cat2bug.public-url` is `https://bug.example.com` and the metadata endpoint is requested
- **THEN** the issuer is `https://bug.example.com` and all endpoint URLs start with it

### Requirement: Clients register dynamically

The system SHALL accept dynamic client registration at `POST /oauth/register` for public clients only. Each `redirect_uri` MUST be either an `https` URL or an `http` loopback URL (`127.0.0.1` or `localhost`, any port). The response MUST include a generated `client_id`.

#### Scenario: Register a client

- **WHEN** a client registers with name "Claude" and redirect URI `https://claude.ai/api/mcp/auth_callback`
- **THEN** the response contains a new `client_id` and the registered redirect URI

#### Scenario: Reject insecure redirect URI

- **WHEN** a client registers with redirect URI `http://evil.example.com/cb`
- **THEN** registration fails with `invalid_redirect_uri`

### Requirement: Authorization code flow with mandatory PKCE

`GET /oauth/authorize` SHALL require `response_type=code`, a registered `client_id`, an exactly matching registered `redirect_uri`, and a `code_challenge` with method `S256`. Valid requests MUST be sent to a consent page that requires login; invalid client or redirect URI MUST show an error page and MUST NOT redirect to the supplied URI. Authorization codes MUST be single-use, expire within 10 minutes, and be bound to client, redirect URI, user, code challenge, and the `resource` parameter.

#### Scenario: User approves access

- **WHEN** a logged-in user approves a valid authorization request
- **THEN** the browser is redirected to the registered redirect URI with `code` and the original `state`

#### Scenario: User denies access

- **WHEN** the user clicks "deny"
- **THEN** the browser is redirected with `error=access_denied` and the original `state`

#### Scenario: Missing PKCE is rejected

- **WHEN** an authorization request has no `code_challenge`
- **THEN** the request is rejected with `invalid_request`

#### Scenario: Not logged in

- **WHEN** a user who is not logged in opens a valid authorization request
- **THEN** the login page is shown first and, after login, the consent page for the same request is shown

### Requirement: Token endpoint issues and rotates tokens

`POST /oauth/token` SHALL exchange a valid authorization code and matching `code_verifier` for an access token (prefix `c2b_oat_`, 1-hour lifetime) and a refresh token (30-day lifetime). The `refresh_token` grant SHALL issue a new access token and a new refresh token and invalidate the old refresh token. Reuse of an already-rotated refresh token MUST revoke every token in that chain.

#### Scenario: Exchange code

- **WHEN** a client posts a valid code with the correct `code_verifier`
- **THEN** it receives an access token, a refresh token, `token_type` Bearer, and `expires_in` 3600

#### Scenario: Wrong verifier

- **WHEN** a client posts a valid code with a wrong `code_verifier`
- **THEN** the response is `invalid_grant` and the code can no longer be used

#### Scenario: Refresh token reuse is detected

- **WHEN** a refresh token that was already rotated is presented again
- **THEN** the request fails with `invalid_grant` and all tokens issued from that authorization are revoked

### Requirement: OAuth tokens are managed like personal tokens

Tokens issued through OAuth SHALL appear in the owner's token list with source "OAuth" and the client name, and SHALL be revocable there. `POST /oauth/revoke` SHALL revoke a given access or refresh token.

#### Scenario: Revoke from profile page

- **WHEN** a user revokes the "Claude" OAuth entry in their token list
- **THEN** both the access token and the refresh token of that authorization stop working

### Requirement: OAuth endpoints are rate limited

Registration and token endpoints MUST be rate limited per client IP.

#### Scenario: Excessive registrations are throttled

- **WHEN** one IP sends more registration requests than the configured limit within a minute
- **THEN** further requests are rejected with HTTP 429 until the window passes
