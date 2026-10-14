# quarkus-git-repositories Specification

## Purpose

Project Git repository features (Smart HTTP, browse, branches, PRs, releases, members, webhooks, import) are available on the Quarkus backend with SPA-compatible API contracts.

## Requirements

### Requirement: Project Git repository features available on Quarkus
Quarkus MUST provide project-scoped Git/source repository capabilities that the current Spring mainline SPA already depends on (repository list, browse, branches, and other wired UI flows under project code routes), so users do not lose source-management features when switching the default backend to Quarkus.

#### Scenario: Open project source repositories page
- **WHEN** a project member opens the project source/Git management UI against Quarkus JVM
- **THEN** repository list (or empty state) loads successfully without a backend 404 for the SPA’s primary list API

#### Scenario: Browse an existing repository
- **WHEN** a repository that exists in the Spring-compatible data model is opened in the browse UI against Quarkus
- **THEN** the user can navigate tree/file views through Quarkus APIs without requiring Spring admin to be running

### Requirement: Git API contracts stay SPA-compatible
New or restored Quarkus Git endpoints MUST preserve path and payload contracts expected by the existing Vue routes under project `code/{slug}` (and related settings), unless a coordinated frontend change is included in the same delivery. Prefer backend adaptation over frontend forks.

#### Scenario: SPA uses existing Git client modules
- **WHEN** the embedded SPA calls its existing Git API modules against Quarkus
- **THEN** list/browse happy paths succeed without Quarkus-only client code paths
