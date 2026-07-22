# git-repo-webhooks Specification

## Purpose

Repository-scoped outbound webhooks with GitHub-compatible signatures and payloads for push, tag, and pull_request events, including one-time secret reveal and delivery logs.

## Requirements

### Requirement: Manage repository webhooks

The system SHALL allow users with effective repository manage permission to create, list, update, activate/deactivate, and delete webhooks for that repository. Each webhook MUST include a target HTTPS or HTTP URL, optional alias, a non-empty secret, a non-empty set of subscribed events among `push`, `tag`, and `pull_request`, and an active flag. Users without effective manage permission MUST be denied all webhook management APIs and settings UI.

#### Scenario: Manage user creates webhook

- **WHEN** a manage user creates a webhook with URL, secret, and at least one event
- **THEN** the webhook is stored for that repository and appears in the webhook list

#### Scenario: Non-manage user denied

- **WHEN** a user without effective manage permission calls webhook create or list APIs
- **THEN** the system denies the operation

### Requirement: Secret shown once then reset-only

The system SHALL return the webhook secret in plaintext only in the response of create and secret-regenerate operations. List and get webhook responses MUST NOT include the plaintext secret (they MAY indicate that a secret is configured). After create or regenerate, subsequent reads MUST NOT reveal the previous plaintext; regenerating MUST invalidate the old secret for signing.

#### Scenario: Create returns secret once

- **WHEN** a manage user successfully creates a webhook
- **THEN** the create response includes the plaintext secret exactly once for the client to copy

#### Scenario: List hides secret

- **WHEN** a manage user lists webhooks
- **THEN** no plaintext secret field is returned

#### Scenario: Regenerate replaces secret

- **WHEN** a manage user regenerates the secret for a webhook
- **THEN** the response includes the new plaintext secret and later deliveries MUST sign with the new secret only

### Requirement: GitHub-compatible delivery signing and payload

The system SHALL deliver webhook HTTP POST bodies as GitHub-style JSON for the subscribed event types and SHALL sign the raw request body with HMAC-SHA256 using the webhook secret, sending header `X-Hub-Signature-256` with value `sha256=` followed by the lowercase hex digest. The system SHALL also send an event name header compatible with GitHub (`X-GitHub-Event`) for `push`, `create`, `delete`, or `pull_request` as applicable. Push payloads MUST include GitHub-style fields such as `ref`, `before`, `after`, and a `commits` array (MAY truncate with a truncation indicator). Pull request payloads MUST include `action` and a `pull_request` object. Inactive webhooks MUST NOT be delivered to.

#### Scenario: Push delivery is signed

- **WHEN** a subscribed active webhook receives a push event
- **THEN** the POST body is GitHub-style JSON and `X-Hub-Signature-256` verifies with the webhook secret over that exact body

#### Scenario: Inactive webhook skipped

- **WHEN** a matching event occurs but the webhook is inactive
- **THEN** the system does not send an HTTP request for that webhook

### Requirement: Delivery logs

The system SHALL persist a delivery log entry per attempted outbound request including event type, timestamp, success flag, HTTP response status when available, duration, and truncated response or error summary. Manage users SHALL be able to list recent deliveries for a webhook. Git push and pull-request API success MUST NOT depend on webhook delivery success.

#### Scenario: Failed delivery is logged

- **WHEN** an active webhook POST fails or returns a non-success HTTP status
- **THEN** a delivery log entry records the failure and the originating Git or PR operation still succeeds

#### Scenario: Manage user lists deliveries

- **WHEN** a manage user requests deliveries for a webhook
- **THEN** the system returns recent delivery records for that webhook

### Requirement: Webhooks settings UI

The system SHALL provide repository settings UI under the access-control settings group for listing webhooks, creating/editing subscriptions (URL, alias, secret on create, events, active), showing the plaintext secret only in a one-time reveal after create or regenerate, and viewing delivery logs.

#### Scenario: Sidebar opens webhooks

- **WHEN** a manage user selects WebHooks in the access-control group of the repository settings sidebar
- **THEN** the app opens the webhooks settings page for that repository
