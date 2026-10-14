## MODIFIED Requirements

### Requirement: JVM build and run precede Native work
The change MUST deliver a Quarkus JVM package that starts and serves the SPA/API before any Native volume optimization is treated as done. Native packaging MAY be prepared as scripts, but MUST NOT block the JVM human acceptance gate.

#### Scenario: Package Quarkus JVM without Native profile
- **WHEN** a developer runs the documented Quarkus JVM package command with tests or agreed skip policy
- **THEN** `cat2bug-platform-admin` produces a runnable Quarkus artifact without requiring `-Dnative`
