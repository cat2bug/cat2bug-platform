## ADDED Requirements

### Requirement: Project has an editable work calendar

The system SHALL provide exactly one work calendar per project. The calendar MUST include a configurable work week (which weekdays are working days; default Monday–Friday) and a set of date exceptions. Each exception MUST be either `holiday` (non-working) or `workday` (makeup working day that overrides the work week). Project creators and project admins MUST be able to edit the calendar; other project members MUST be able to read it. Absence of a stored calendar row MUST behave as the default work week with no exceptions.

#### Scenario: Default calendar when unset

- **WHEN** a project has no stored work calendar row
- **THEN** workday checks use Monday–Friday as working days and no holiday or makeup exceptions

#### Scenario: Admin edits work week

- **WHEN** a project admin sets the work week to Monday–Saturday
- **THEN** subsequent workday checks treat Sunday as non-working unless a makeup `workday` exception applies that day

#### Scenario: Member cannot edit

- **WHEN** a non-admin project member opens the calendar editor without project admin or creator role
- **THEN** the calendar is read-only or save is rejected

### Requirement: Exceptions support holidays and makeup workdays

The system SHALL allow marking individual calendar dates as `holiday` or `workday` (调休上班). A `workday` exception MUST count as a working day even if that weekday is outside the work week. A `holiday` exception MUST count as non-working even if that weekday is in the work week. Exception lookup MUST take precedence over the work week.

#### Scenario: National holiday midweek

- **WHEN** a Wednesday is marked `holiday` and the work week includes Wednesday
- **THEN** that date is not a working day

#### Scenario: Makeup Sunday

- **WHEN** a Sunday is marked `workday` and the work week excludes Sunday
- **THEN** that date is a working day

### Requirement: Import China statutory pack by year

The system SHALL ship built-in China statutory holiday packs (including makeup workdays) for supported years as offline product resources. Authorized editors MUST be able to import a selected year into the project calendar exceptions without calling an external network holiday API. Import MUST offer a clear mode that does not silently destroy unrelated years’ exceptions. When a year has no pack and no project exceptions, the system MUST still apply the work week and MUST surface a notice that holiday data for that year is missing.

#### Scenario: Import fills year exceptions offline

- **WHEN** an admin imports the built-in China pack for year Y into the project calendar
- **THEN** year Y’s holiday and makeup dates appear as exceptions without requiring outbound holiday API calls

#### Scenario: Missing year degrades to work week

- **WHEN** scheduling uses year Y that has no built-in pack and no project exceptions for Y
- **THEN** only the work week defines non-working days and the UI indicates that holiday data for Y is not configured

### Requirement: Calendar editor surface

The system SHALL provide an OmniPlan-like editor dialog for the project work calendar that can toggle work-week days, browse a month grid to set or clear exceptions, and trigger per-year import. The dialog MUST be reachable from project settings and MUST offer a shortcut from the defect Gantt view for users who can open project settings or at least view the calendar. The editor MUST NOT require a dedicated application route page.

#### Scenario: Open from project settings

- **WHEN** a project admin opens the work-calendar entry on project settings
- **THEN** the calendar dialog opens and they can edit the work week and exceptions and save

#### Scenario: Gantt shortcut

- **WHEN** a user on the Gantt view activates the work-calendar shortcut
- **THEN** the calendar editor or viewer dialog opens for the current project
