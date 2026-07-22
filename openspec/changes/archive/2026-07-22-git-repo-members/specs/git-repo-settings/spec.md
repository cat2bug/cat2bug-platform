## ADDED Requirements

### Requirement: Members navigation is enabled in settings

The system SHALL treat the repository settings sidebar item for Members（仓库成员管理） as an enabled navigation target to `.../settings/members` for users with effective repository manage permission, and MUST NOT present it only as a disabled “coming soon” placeholder.

#### Scenario: Sidebar members navigates

- **WHEN** a manage user selects Members in the repository settings sidebar
- **THEN** the app opens the members settings page for that repository
