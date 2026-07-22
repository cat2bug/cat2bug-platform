# git-repo-settings Specification

## Purpose

Repository settings shell with nested routes for general info, danger zone, members management, and WebHooks for users with effective repository manage permission.

## Requirements

### Requirement: Members navigation is enabled in settings

The system SHALL treat the repository settings sidebar item for Members（仓库成员管理） as an enabled navigation target to `.../settings/members` for users with effective repository manage permission, and MUST NOT present it only as a disabled “coming soon” placeholder.

#### Scenario: Sidebar members navigates

- **WHEN** a manage user selects Members in the repository settings sidebar
- **THEN** the app opens the members settings page for that repository

### Requirement: Settings sidebar includes WebHooks

The system SHALL present an enabled WebHooks navigation item in the repository settings sidebar under the access-control group for users with effective repository manage permission, routing to the repository webhooks settings pages.

#### Scenario: Manage user opens WebHooks from sidebar

- **WHEN** a manage user selects WebHooks in the access-control group of the repository settings sidebar
- **THEN** the app navigates to the webhooks settings route for that repository
