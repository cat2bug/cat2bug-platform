## ADDED Requirements

### Requirement: Create project can optionally copy from a source project

The system SHALL allow a user creating a project to enable a “copy project configuration” option, select an eligible source project, and choose which assets and settings to copy into the newly created project. Project identity fields (name, path key, introduction, icon) MUST be supplied by the create form and MUST NOT be overwritten by the source project’s “project info” card data.

#### Scenario: Copy switch disabled

- **WHEN** the user creates a project with the copy switch off
- **THEN** the system creates an empty project using the existing create behavior (no copy job)

#### Scenario: Copy switch enabled with selections

- **WHEN** the user enables copy, selects a source project, checks one or more copy items, and submits create
- **THEN** the system creates the new project and starts a copy process that applies only the checked items

### Requirement: Eligible source projects

The system SHALL list as copy sources only projects where the current user is a member and holds the project creator role (`project.create-by`) or project admin role (`project.admin`). Source projects from other teams SHALL be allowed when the user meets the same role criteria.

#### Scenario: Cross-team source listed

- **WHEN** the user is project admin of a project in team A and is creating a project in team B
- **THEN** that team A project SHALL appear in the source project list

#### Scenario: Non-admin member excluded

- **WHEN** the user is only a developer/tester/outsider on a project
- **THEN** that project MUST NOT appear as a copy source

### Requirement: Selectable copy items

The system SHALL support checking the following copy items independently (except dependency rules below), presented as tabs for Git / test cases / deliverables / documents, each with select-all and searchable individual selection:

- Git repositories: multi-select among source repos; copy SHALL include repository metadata and the bare repository disk content
- Test cases: all or selected case IDs
- Deliverables (module) tree: all or selected module IDs (ancestors included as needed)
- Documents: all or selected document IDs including on-disk file content (ancestors included as needed)
- Project settings excluding the project-info card: project members and roles; AI model/account/MCP settings; DingTalk/Feishu/WeCom project configs (work calendar is not project settings and MUST NOT be offered or copied)

The system MUST NOT offer copying of defects, API keys, outbound push configuration, or delete-project actions as part of this flow.

#### Scenario: Cases imply deliverables

- **WHEN** the user selects any test cases (all or partial) without selecting deliverables
- **THEN** the system SHALL also include the needed deliverable tree (all modules, or modules referenced by selected cases plus ancestors)

#### Scenario: Members copied

- **WHEN** the user checks member copy
- **THEN** the new project SHALL receive source members with their project roles, while the creating user remains project creator on the new project

### Requirement: Copy size estimate versus project storage quota

Before completing create-with-copy, the system SHALL show an estimated byte size of the selected copy items against the target team plan’s per-project max storage (default 1 GiB). If the estimate exceeds the limit, the system MUST prevent completing the copy selection as submitted (block submit and/or further selection) and explain the quota.

#### Scenario: Over quota blocked

- **WHEN** selected copy estimate exceeds the target project max storage
- **THEN** the user cannot successfully start that copy until selections fit under the limit

#### Scenario: Under quota allowed

- **WHEN** selected copy estimate is within the per-project max storage
- **THEN** create-with-copy MAY proceed and the estimate display shows used-vs-limit

### Requirement: Asynchronous copy with progress

When copy includes disk-heavy items (Git bare and/or documents) or otherwise non-trivial volume, the system SHALL run the copy asynchronously after project creation and expose progress (or completion/failure status) to the user. Partial failure MUST NOT silently leave the user without status; failed item groups SHALL be visible as failed.

#### Scenario: User sees copy progress after create

- **WHEN** create-with-copy succeeds in creating the project shell
- **THEN** the user can observe copy job progress or final success/failure for the selected items
