## ADDED Requirements

### Requirement: Each team has one defect field catalog

The system SHALL store custom defect fields and builtin field layout overrides per team (`team_id`). Field keys MUST be unique within a team. Project members consuming defect forms and lists MUST receive the enabled field definitions and column layout for the project's owning team.

#### Scenario: Two projects in one team share fields

- **WHEN** team admin adds a custom field `severity` on the team
- **THEN** defect create/edit UIs for every project in that team can show that field when enabled

#### Scenario: Field key unique per team

- **WHEN** a team admin tries to create a second custom field with the same `field_key` in the same team
- **THEN** the create fails and the catalog is unchanged

### Requirement: Team create-by and team admin maintain defect fields

Only team create-by and team admin SHALL add, edit, reorder, enable/disable, delete custom fields, and update builtin field layout for the team. Other roles MUST NOT modify the catalog. Delete of a custom field MUST follow existing safety rules for in-use data as implemented for project-scoped fields (reject or clear per current product rules carried forward).

#### Scenario: Team admin adds custom field

- **WHEN** a team admin submits a valid new custom field for the team
- **THEN** the field appears in the team catalog and in project defect forms for that team's projects

#### Scenario: Project tester cannot edit catalog

- **WHEN** a user with only project tester role attempts to update defect field definitions via the team API
- **THEN** the operation is rejected

### Requirement: Business reads resolve via project to team

Defect list/form APIs that accept `projectId` for enabled fields or column layout MUST resolve `project.team_id` and return that team's catalog. Clients MUST NOT need a separate teamId for ordinary defect workflows.

#### Scenario: List enabled fields by project

- **WHEN** a member with defect list permission requests enabled fields for a project
- **THEN** the response reflects the owning team's enabled custom fields and builtin visibility
