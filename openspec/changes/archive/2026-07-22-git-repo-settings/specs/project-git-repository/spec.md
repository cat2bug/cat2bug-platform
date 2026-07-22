## ADDED Requirements

### Requirement: Preferred UI for repository metadata edit and delete

The system SHALL treat the repository shell settings pages (`.../code/{slug}/settings/**`) as the preferred UI for editing repository metadata and deleting a repository. Project repository list actions that previously opened an inline settings dialog MUST navigate to those settings pages instead (or equivalent shell entry) for manage users.

#### Scenario: List no longer relies on dialog as primary editor

- **WHEN** a manage user chooses to configure an existing repository from the project repository list
- **THEN** the user is taken to the shell settings experience rather than editing solely inside a list-page dialog as the primary path
