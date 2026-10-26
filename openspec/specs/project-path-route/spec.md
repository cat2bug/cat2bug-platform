# project-path-route Specification

## Requirements

### Requirement: Share link opens project dashboard

The system SHALL resolve the browser History path `/{teamKey}/{projectKey}` (without requiring a `#` route fragment) to the corresponding project and, for an authenticated user allowed to enter that project, open the project **workbench** (module path segment MAY remain `dashboard`) as the default landing view. The same workbench MUST also be reachable at `/{teamKey}/{projectKey}/dashboard` when module routes are enabled.

#### Scenario: Member opens share link

- **WHEN** a project member visits `/{teamKey}/{projectKey}` while logged in
- **THEN** the current project context becomes that project
- **THEN** the workbench for that project is shown
- **THEN** the location remains a History path under `/{teamKey}/{projectKey}` without a `#` route fragment

#### Scenario: Unknown path keys

- **WHEN** a user visits a path whose `teamKey` or `projectKey` does not exist
- **THEN** the system shows a not-found (or equivalent) outcome and does not switch project context

#### Scenario: Unauthorized user

- **WHEN** a logged-in user without access to the project visits `/{teamKey}/{projectKey}`
- **THEN** the system denies access (403 or equivalent) without exposing project internals


### Requirement: Project path is copyable

The system SHALL expose the canonical project path `/{teamKey}/{projectKey}` (relative to the deployment origin, History style without `#`) in the product UI so users can share it.

#### Scenario: Copy project path

- **WHEN** an authorized user views project settings or an equivalent surface that shows the project path
- **THEN** the displayed path uses the current `teamKey` and `projectKey` and MUST NOT include a `#` before the path keys
