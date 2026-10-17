## MODIFIED Requirements

### Requirement: Delivery assets target the unified admin
Embedded frontend build, Docker Compose backend image and rpm packaging MUST consume artifacts of the unified `cat2bug-platform-admin`. The Docker Compose backend MUST persist the whole `cat2bug.profile` directory (including the `git` subdirectory) on a mounted volume. The build MUST NOT depend on a report-preset sync script or on classpath resources under `报告/模版/`.

#### Scenario: Compose deploy keeps uploads and repositories
- **WHEN** an operator runs the documented Docker Compose stack, completes setup, uploads an attachment and creates a project Git repository, then recreates the backend container
- **THEN** the attachment and the repository are still readable after the container is recreated
