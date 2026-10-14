## ADDED Requirements

### Requirement: Repository contains a single backend line
The repository MUST contain exactly one backend implementation consisting of the modules `cat2bug-platform-admin`, `cat2bug-platform-system`, `cat2bug-platform-framework` and `cat2bug-platform-common`. Spring Boot backend modules, the Quarkus spike module and Spring-only delivery assets MUST NOT remain in the working tree of the mainline branch.

#### Scenario: No legacy backend modules
- **WHEN** a maintainer lists top-level directories tracked by git on `core`
- **THEN** no `cat2bug-platform-{api,ai,im,quartz,generator}`, no `cat2bug-platform-quarkus-*` directory and no root `META-INF/` exist

#### Scenario: No Spring-only delivery scripts
- **WHEN** a maintainer searches tracked files under `deploy/` and `readme/` (excluding `openspec/changes/archive/**`)
- **THEN** no script or CI workflow builds Spring Boot fat JARs or Spring Native images

### Requirement: Spring line is preserved by tag
Before Spring modules are deleted, an annotated or lightweight git tag `spring-final-1.1.0` MUST point at the last commit that still contains the Spring backend, so the rollback source remains reachable without the modules in the tree.

#### Scenario: Recover Spring sources
- **WHEN** a maintainer checks out `spring-final-1.1.0`
- **THEN** the Spring modules (`cat2bug-platform-admin` Spring Boot application and its sibling modules) are present and buildable with the commands documented at that commit

### Requirement: Default Maven build is the product build
Running `mvn package` (or `mvn install`) at the repository root without any profile MUST build all backend modules required by the product; no `quarkus` profile SHALL be required.

#### Scenario: Plain package builds admin
- **WHEN** a developer runs `mvn -DskipTests package` at the repository root
- **THEN** `cat2bug-platform-admin` is packaged as the runnable product artifact

### Requirement: Module identifiers carry no stack suffix
Module directory names, Maven artifactIds and Java package names of the backend MUST NOT contain the `quarkus` technology suffix. Directories MUST be `cat2bug-platform-<name>`, artifactIds `cat2bug-<name>`, and Java packages `com.cat2bug.<name>…`. Third-party identifiers (e.g. `io.quarkus`, `quarkus.*` configuration keys, Quarkus Maven plugins) are exempt.

#### Scenario: No stack-suffixed identifiers remain
- **WHEN** a maintainer greps tracked files outside `openspec/changes/archive/**` for `com.cat2bug.quarkus`, `cat2bug-platform-quarkus-` and `cat2bug-quarkus-`
- **THEN** there are no matches

#### Scenario: Renamed backend passes verification
- **WHEN** the renamed modules are built, fully tested, packaged as Native and smoke-tested (regular and fresh-install)
- **THEN** all tests pass and both smoke runs succeed with the same checks as before the rename

### Requirement: Delivery assets target the unified admin
Embedded frontend build, report preset sync scripts, Docker Compose backend image and rpm packaging MUST consume artifacts of the unified `cat2bug-platform-admin`. The Docker Compose backend MUST persist the whole `cat2bug.profile` directory (including the `git` subdirectory) on a mounted volume.

#### Scenario: Compose deploy keeps uploads and repositories
- **WHEN** an operator runs the documented Docker Compose stack, completes setup, uploads an attachment and creates a project Git repository, then recreates the backend container
- **THEN** the attachment and the repository are still readable after the container is recreated

#### Scenario: Report presets synced to admin
- **WHEN** a maintainer runs `scripts/sync-report-preset-templates.sh`
- **THEN** preset files are written under `cat2bug-platform-admin/src/main/resources/报告/模版/`
