## REMOVED Requirements

### Requirement: Spring Boot admin is the default backend delivery
**Reason**: Product direction restored **Quarkus as the default backend delivery** (JVM first, then Native) for performance and slim-binary goals.
**Migration**: Follow `quarkus-backend-parity` and updated root `README.md` / `CLAUDE.md`; Spring admin remains **optional comparison / emergency rollback only** (commands kept, not recommended).

### Requirement: Quarkus delivery artifacts are retired
**Reason**: Quarkus packaging scripts and modules are restored as the **supported product path**.
**Migration**: Use `deploy/build-*-quarkus.sh`, `deploy/run-quarkus-jar.sh`, and Maven `-Pquarkus`; see `quarkus-jvm-acceptance-gate` / `quarkus-native-slim-binary` and `GATE-NOTES.md`.

### Requirement: No Quarkus module as supported product surface
**Reason**: Quarkus modules are again a supported surface under `-Pquarkus`.
**Migration**: Build with `-Pquarkus` to include `cat2bug-platform-quarkus-*` modules.

## ADDED Requirements

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
