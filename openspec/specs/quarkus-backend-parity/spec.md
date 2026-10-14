# quarkus-backend-parity Specification

## Purpose

Quarkus admin is the default Cat2Bug backend and must behave equivalently to the Spring mainline HTTP API, setup, dual database and Excel features.
## Requirements
### Requirement: Quarkus is the default product backend
The documented and scripted backend for Cat2Bug Platform MUST be the Quarkus-based `cat2bug-platform-admin`, built by the default Maven reactor without any profile. Spring Boot backend modules MUST NOT remain in the repository; the Spring line is reachable only through tag `spring-final-1.1.0`.

#### Scenario: Contributor reads backend quickstart
- **WHEN** a contributor follows root `README.md` / `CLAUDE.md` backend run instructions after this change
- **THEN** the primary commands build and start `cat2bug-platform-admin` without `-Pquarkus` and no Spring Boot instructions are presented as runnable from the mainline tree

### Requirement: HTTP API behavioral parity with Spring mainline
For capabilities exposed by the Spring mainline UI at tag `spring-final-1.1.0`, the backend MUST expose equivalent HTTP APIs such that the embedded SPA can complete the same user flows. Response envelopes (`code` / `msg` / `data` or project-standard equivalents) and auth failure semantics MUST match that baseline for the same operations. The code generator (`/tool/gen`) and RuoYi sample (`test/user`) are excluded from the baseline.

#### Scenario: Login and open a project
- **WHEN** a user logs in through the SPA and opens an existing project
- **THEN** the same screens load as at the `spring-final-1.1.0` baseline for that project without missing menu entries for the same role

#### Scenario: Unauthorized protected API
- **WHEN** a protected system API is called without a valid token
- **THEN** the client receives the same class of rejection (e.g. 401 / project-standard auth error body) as the baseline for that API

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

