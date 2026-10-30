## MODIFIED Requirements

### Requirement: Project has an editable work calendar

The system SHALL provide exactly one work calendar per project. The calendar MUST include a configurable work week (which weekdays are working days; default Monday–Friday) and a set of date exceptions. Each exception MUST be either `holiday` (non-working) or `workday` (makeup working day that overrides the work week). Project creators and project admins (`project.create-by` / `project.admin`) MUST be able to edit the calendar; other project members MUST be able to read it. Write APIs MUST reject non-admin editors. Absence of a stored calendar row MUST behave as the default work week with no exceptions.

#### Scenario: Default calendar when unset

- **WHEN** a project has no stored work calendar row
- **THEN** workday checks use Monday–Friday as working days and no holiday or makeup exceptions

#### Scenario: Admin edits work week

- **WHEN** a project admin sets the work week to Monday–Saturday
- **THEN** subsequent workday checks treat Sunday as non-working unless a makeup `workday` exception applies that day

#### Scenario: Member cannot edit

- **WHEN** a non-admin project member opens the calendar editor without project admin or creator role
- **THEN** the calendar is read-only or save is rejected

#### Scenario: Non-admin PUT rejected

- **WHEN** a non-admin member calls the work-calendar write API
- **THEN** the server rejects the write and stored calendar data is unchanged

### Requirement: Calendar editor surface

The system SHALL provide an OmniPlan-like editor dialog for the project work calendar that can toggle work-week days and browse a month grid to set or clear exceptions. The dialog MUST be reachable from project settings and MUST offer a shortcut from the defect Gantt view for users who can open project settings or at least view the calendar. Non-admin users MAY open the dialog read-only from the Gantt shortcut. The editor MUST NOT require a dedicated application route page.

#### Scenario: Open from project settings

- **WHEN** a project admin opens the work-calendar entry on project settings
- **THEN** the calendar dialog opens and they can edit the work week and exceptions and save

#### Scenario: Gantt shortcut

- **WHEN** a user on the Gantt view activates the work-calendar shortcut
- **THEN** the calendar editor or viewer dialog opens for the current project

#### Scenario: Gantt shortcut read-only for member

- **WHEN** a non-admin member opens the work calendar from the Gantt shortcut
- **THEN** they can view the calendar but cannot save changes
