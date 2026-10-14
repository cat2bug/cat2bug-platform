# quarkus-backend-parity Specification

## Purpose

Quarkus admin is the default Cat2Bug backend and must behave equivalently to the Spring mainline HTTP API, setup, dual database and Excel features.

## Requirements

### Requirement: Quarkus is the default product backend
The documented and scripted default backend for Cat2Bug Platform MUST be Quarkus `cat2bug-platform-quarkus-admin` (profile `-Pquarkus`). Spring Boot admin MAY remain in the repository for comparison or emergency rollback but MUST NOT be presented as the preferred release path.

#### Scenario: Contributor reads backend quickstart
- **WHEN** a contributor follows root `README.md` / `CLAUDE.md` backend run instructions after this change
- **THEN** the primary commands start or package Quarkus admin, not Spring Boot admin as the recommended path

### Requirement: HTTP API behavioral parity with Spring mainline
For capabilities already exposed by the current Spring mainline UI, Quarkus MUST expose equivalent HTTP APIs such that the embedded SPA can complete the same user flows without Quarkus-specific frontend forks. Response envelopes (`code` / `msg` / `data` or project-standard equivalents) and auth failure semantics MUST match Spring for the same operations.

#### Scenario: Login and open a project
- **WHEN** a user logs in through the SPA against Quarkus JVM and opens an existing project
- **THEN** the same screens load as against Spring for that project without missing menu entries that Spring provides for the same role

#### Scenario: Unauthorized protected API
- **WHEN** a protected system API is called without a valid token on Quarkus
- **THEN** the client receives the same class of rejection (e.g. 401 / project-standard auth error body) as Spring for that API

### Requirement: Setup and dual database support
Quarkus MUST support the install/setup flows for H2 and MySQL (including Flyway migrations triggered by the product’s setup semantics) so a fresh deployment can reach a login-capable state comparable to Spring.

#### Scenario: Fresh H2 install completes
- **WHEN** an uninstalled Quarkus instance completes H2 setup through the product wizard
- **THEN** the operator can restart (or continue per product rules) and log in with the created admin account

### Requirement: Excel features are complete on Quarkus
Every Excel import/export capability available on the Spring mainline (including template download and user-visible validation/error feedback for supported sheets) MUST be available on Quarkus with equivalent results. The implementation MAY choose Apache POI, FastExcel, or another library, but MUST NOT omit Spring-available Excel features for packaging or Native size reasons.

#### Scenario: Export and re-import a supported sheet
- **WHEN** a user exports a supported business sheet on Quarkus and imports a valid file of the same type
- **THEN** the operation succeeds with the same class of outcome as Spring, without a Quarkus-only UI path

#### Scenario: No Excel feature dropped vs Spring
- **WHEN** a maintainer compares the Spring Excel entry points used by the SPA with Quarkus
- **THEN** each Spring-backed Excel action has a working Quarkus counterpart
