## ADDED Requirements

### Requirement: JVM build and run precede Native work
The change MUST deliver a Quarkus JVM package that starts and serves the SPA/API before any Native volume optimization is treated as done. Native packaging MAY be prepared as scripts, but MUST NOT block the JVM human acceptance gate.

#### Scenario: Package Quarkus JVM without Native profile
- **WHEN** a developer runs the documented Quarkus JVM package command with tests or agreed skip policy
- **THEN** `cat2bug-platform-quarkus-admin` produces a runnable Quarkus artifact without requiring `-Dnative`

### Requirement: Automated smoke before human review
Before requesting human UI review, the implementer MUST run an automated smoke checklist covering at least: process starts, health or root SPA reachable, login API succeeds with demo/admin credentials on a known dataset (or fresh setup path documented).

#### Scenario: Smoke login after start
- **WHEN** Quarkus JVM is started with a documented profile and a valid user exists
- **THEN** a login request returns success and a subsequent authenticated call returns non-401

### Requirement: Human acceptance gate before Native
Native binary size and Graal packaging work MUST wait until the product owner has manually reviewed JVM feature/UI parity and confirmed readiness. Feedback after Gate A MUST be possible without Native being finished.

#### Scenario: Report Gate A without Native
- **WHEN** JVM parity smoke passes and human review is requested
- **THEN** delivery notes state that Native (~20MB) is deferred to Gate B and is not required for the review build
