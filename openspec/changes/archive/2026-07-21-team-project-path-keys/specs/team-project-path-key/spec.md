## ADDED Requirements

### Requirement: Team has a globally unique path key

The system SHALL require a path key (`teamKey`) when creating a team. The key MUST be globally unique among teams, MUST match the allowed slug character rules (letters, digits, `.`, `_`, `-`; must start and end with a letter or digit), and MUST NOT equal a reserved system path segment.

#### Scenario: Create team with valid key

- **WHEN** a user creates a team with display name and a valid unused `teamKey`
- **THEN** the team is persisted with that key

#### Scenario: Duplicate team key rejected

- **WHEN** a user creates a team with a `teamKey` already used by another team
- **THEN** the system rejects the create with a uniqueness error

#### Scenario: Reserved team key rejected

- **WHEN** a user attempts to use a reserved segment (e.g. `login`, `setup`) as `teamKey`
- **THEN** the system rejects the value

### Requirement: Project has a team-scoped path key

The system SHALL require a path key (`projectKey`) when creating a project under a team. The key MUST be unique within that team, MUST follow the same slug rules as `teamKey`, and the project MUST belong to a team (path namespace is always `/{teamKey}/{projectKey}`).

#### Scenario: Create project with valid key

- **WHEN** a user creates a project in a team with a valid `projectKey` unused in that team
- **THEN** the project is persisted with that key

#### Scenario: Same project key in another team allowed

- **WHEN** team A and team B each create a project with `projectKey=platform`
- **THEN** both creates succeed

#### Scenario: Duplicate project key in same team rejected

- **WHEN** a user creates a second project in the same team with an existing `projectKey`
- **THEN** the system rejects the create

### Requirement: Path keys may be changed with warning

The system SHALL allow authorized users to change `teamKey` or `projectKey`, and MUST warn that existing share links and namespaced clone URLs based on the old keys become invalid. Alias redirects are not required in this change.

#### Scenario: Rename project key warns

- **WHEN** an authorized user changes a project's `projectKey`
- **THEN** the system accepts the change after confirmation UX and subsequent share/clone URLs use the new key
