# retire-quarkus-delivery Specification

## Purpose

Historical capability: Quarkus delivery was retired in favor of Spring Boot, then the retirement was reversed by `restore-quarkus-full-delivery`. Quarkus admin is now the default backend; Spring admin is optional comparison / emergency rollback.

## Requirements

### Requirement: Quarkus retirement status is reversed
Active specs MUST treat Quarkus admin as the **preferred / default** backend delivery. They MUST NOT forbid Quarkus as the default. Historical archive text under `openspec/changes/archive/**` MAY remain for archaeology only.

#### Scenario: Active specs allow Quarkus default
- **WHEN** a maintainer reads active OpenSpec requirements after this change is archived
- **THEN** requirements document Quarkus admin as the preferred backend, and no active requirement forbids that default

### Requirement: Report presets may ship on Quarkus when it is default
When Quarkus is the default backend, classpath report presets used by the product MUST be available to the Quarkus runtime (copied or shared) so report template selection does not regress versus Spring-only packaging.

#### Scenario: Preset readable on Quarkus
- **WHEN** Quarkus serves the report template picker that depends on classpath presets
- **THEN** preset groups are listable without requiring Spring admin resources exclusively
