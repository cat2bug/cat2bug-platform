## ADDED Requirements

### Requirement: Public Smart HTTP path is namespaced

In addition to existing Smart HTTP behavior (JGit, Basic auth, role matrix), the system SHALL serve Git Smart HTTP primarily at `/{teamKey}/{projectKey}/code/{slug}.git/**` resolved via team and project path keys.

#### Scenario: info/refs on namespaced URL

- **WHEN** a Git client requests `info/refs` on `/{teamKey}/{projectKey}/code/{slug}.git` with valid credentials and pull permission
- **THEN** the server responds with refs advertisement for that repository
