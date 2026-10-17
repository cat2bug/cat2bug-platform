## ADDED Requirements

### Requirement: Inbound push is stored and listed

The cloud deployment MUST accept a project push package from a private deployment, store it, and list it from `c_project_push` with push source, version, size, state, and time. Deleting a push record MUST remove that inbound item.

#### Scenario: Package is listed

- **WHEN** a private deployment uploads a project push package with a valid project API key
- **THEN** a `c_project_push` row exists and appears in the cloud project sync list

### Requirement: Sync imports with member mapping

An authorized cloud user MUST be able to sync a received package into the project by supplying the member mapping. The import MUST run in one transaction. A failed sync MUST leave project data unchanged and MUST record the failed state on the push row.

#### Scenario: Successful sync

- **WHEN** an authorized user syncs a pending push and provides a member mapping
- **THEN** the package contents are imported under the mapped users and the push state becomes successful

#### Scenario: Failed sync rolls back

- **WHEN** sync fails after it has started writing project data
- **THEN** the project data is unchanged and the push row is not left in the success state

### Requirement: Schema exists in migrations

`c_auth_user` and `c_project_push` MUST be created by Flyway. On a database where those tables already exist with the production column set, migration MUST complete without dropping rows.

#### Scenario: Fresh database

- **WHEN** Flyway runs on an empty MySQL database
- **THEN** `c_auth_user` and `c_project_push` exist with the columns the cloud login and sync features use

#### Scenario: Existing production tables

- **WHEN** Flyway runs on a copy of the production database that already contains both tables and rows
- **THEN** migration finishes and the existing rows are still present

### Requirement: Private deployment default cloud URL

The private deployment push client MUST default its cloud base URL to `https://cloud.cat2bug.com/prod-api`.

#### Scenario: Default outbound host

- **WHEN** a private deployment pushes a project and `cat2bug.cloud.host` is not overridden
- **THEN** the client sends the package to `https://cloud.cat2bug.com/prod-api`
