## ADDED Requirements

### Requirement: Backend runs on a Quarkus LTS release compatible with the MCP extension

The backend SHALL be built on a Quarkus LTS platform version (3.27.x) that satisfies the minimum Quarkus version required by `quarkus-mcp-server-http` 1.9.x and by the `quarkus-mybatis` extension in use.

#### Scenario: Platform version meets extension requirements

- **WHEN** the root `pom.xml` is inspected after the upgrade
- **THEN** `quarkus.platform.version` is a 3.27.x release and the declared `quarkus-mybatis` version lists a Quarkus baseline no newer than the minor line in use

#### Scenario: MCP extension can be added

- **WHEN** `quarkus-mcp-server-http` 1.9.x is temporarily added with one trivial tool
- **THEN** the JVM build starts successfully and the Native build completes without reflection or build-time errors

### Requirement: Upgrade keeps external behavior unchanged

The upgrade MUST NOT change REST paths, response structure (`AjaxResult` / `TableDataInfo`), authentication behavior, database schema, Flyway history, or the setup wizard flow.

#### Scenario: Full test suite passes

- **WHEN** the full Maven test suite is run after the upgrade
- **THEN** all tests that passed before the upgrade still pass

#### Scenario: Error responses keep their format

- **WHEN** an unauthenticated request hits a protected `/system/**` endpoint
- **THEN** the response is HTTP 200 with JSON `code` 401, exactly as before the upgrade

### Requirement: Existing installations upgrade by replacing the binary

An instance installed with the 3.15.7-based release SHALL start with the upgraded binary using its existing `config/install/` files, database, Git repositories, and uploaded files, without manual migration.

#### Scenario: Old runtime configuration is still accepted

- **WHEN** the upgraded application starts with a `quarkus-runtime.properties` file written by the previous release
- **THEN** it connects to the configured `runtime-h2` or `runtime-mysql` datasource and users can log in

### Requirement: All delivery forms are rebuilt and smoke-tested

JVM single JAR, Docker Compose, Native (with UPX) and rpm packaging SHALL be rebuilt on the upgraded platform, and the Native binary SHALL pass both smoke modes defined by ADR-004.

#### Scenario: Native smoke passes in both modes

- **WHEN** `smoke-native-docker.sh arm64` runs against the rebuilt Native binary, once with existing data and once with `FRESH=true`
- **THEN** both runs report success

#### Scenario: Native size is recorded

- **WHEN** the Native build finishes
- **THEN** the size before and after UPX is recorded, and any growth above 10% compared with the 3.15.7 build is reported for review instead of being silently accepted
