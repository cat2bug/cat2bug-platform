## MODIFIED Requirements

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
