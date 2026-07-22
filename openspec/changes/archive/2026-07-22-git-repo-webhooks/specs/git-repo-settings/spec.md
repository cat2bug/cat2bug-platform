## ADDED Requirements

### Requirement: Settings sidebar includes WebHooks

The system SHALL present an enabled WebHooks navigation item in the repository settings sidebar under the access-control group for users with effective repository manage permission, routing to the repository webhooks settings pages.

#### Scenario: Manage user opens WebHooks from sidebar

- **WHEN** a manage user selects WebHooks in the access-control group of the repository settings sidebar
- **THEN** the app navigates to the webhooks settings route for that repository
