## MODIFIED Requirements

### Requirement: Empty or dashboard module opens project dashboard

Within the project namespace, the path `/{teamKey}/{projectKey}` and `/{teamKey}/{projectKey}/dashboard` SHALL open the project **workbench** for an authorized member, after resolving path keys into the current project context. The UI label for this module MUST present as workbench (工作台); the path segment MAY remain `dashboard`.

#### Scenario: Dashboard via module segment

- **WHEN** a project member visits `/{teamKey}/{projectKey}/dashboard`
- **THEN** the project workbench is shown and the current project context matches those keys

#### Scenario: Menu label is workbench

- **WHEN** a project member views the project module navigation
- **THEN** the entry that routes to the dashboard module is labeled as workbench (工作台) in the active locale
