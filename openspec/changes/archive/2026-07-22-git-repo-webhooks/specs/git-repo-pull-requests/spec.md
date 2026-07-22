## ADDED Requirements

### Requirement: Pull request lifecycle triggers webhooks

The system SHALL enqueue outbound webhook deliveries for active repository webhooks subscribed to `pull_request` when a pull request is created, updated in a way that corresponds to synchronize, merged, or closed. Payloads MUST use GitHub-style `pull_request` event shape including an `action` field. Delivery failures MUST NOT fail the PR API operation.

#### Scenario: Creating a PR notifies subscribers

- **WHEN** a user successfully creates a pull request
- **THEN** active webhooks subscribed to `pull_request` are enqueued with action `opened`

#### Scenario: Merging a PR notifies subscribers

- **WHEN** a user successfully merges a pull request
- **THEN** active webhooks subscribed to `pull_request` are enqueued with a closed/merged style action payload
