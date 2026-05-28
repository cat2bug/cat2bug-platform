## ADDED Requirements

### Requirement: System detects first-run installation state

The system SHALL expose whether initial setup is complete. When `cat2bug.install.completed` in `sys_config` is absent or not `true`, and no user with username `admin` exists in `sys_user`, the system MUST treat the instance as not installed.

#### Scenario: Fresh deployment is not installed

- **WHEN** the application starts with an empty database and no install completion flag
- **THEN** `GET /setup/status` returns `installed: false`

#### Scenario: Completed installation is installed

- **WHEN** `cat2bug.install.completed` is `true` in `sys_config`
- **THEN** `GET /setup/status` returns `installed: true`

#### Scenario: Legacy existing deployment auto-marked

- **WHEN** an existing database has a row in `sys_user` with username `admin` (as seeded by SQL migrations)
- **THEN** the system MAY set `cat2bug.install.completed` to `true` on startup without showing the wizard

#### Scenario: Empty database without admin user is not installed

- **WHEN** `sys_user` has no row with username `admin` and `cat2bug.install.completed` is not `true`
- **THEN** `GET /setup/status` returns `installed: false`

### Requirement: Uninstalled instance routes users to setup

The system SHALL redirect unauthenticated clients to the setup wizard when not installed. API requests outside setup endpoints MUST be rejected or redirected except for setup, health, and static setup assets.

#### Scenario: User opens application root before install

- **WHEN** `installed` is `false` and the user navigates to `/`
- **THEN** the UI navigates to `/setup`

#### Scenario: Setup API is accessible without login before install

- **WHEN** `installed` is `false` and the client calls `POST /setup/submit` with valid payload
- **THEN** the request is accepted without authentication

#### Scenario: Setup API blocked after install

- **WHEN** `installed` is `true` and the client calls `POST /setup/submit`
- **THEN** the request is rejected with an appropriate error

### Requirement: Setup wizard collects infrastructure configuration

The setup wizard SHALL allow configuring: database mode (H2 embedded or MySQL), cache mode (local Caffeine-only or Redis L2, **independent of database choice**), file storage base path, log directory path, Ollama AI enablement and host URL, administrator username and password (defaults **`admin`** / **`cat2bug`**), public registration toggle, and login captcha toggle.

#### Scenario: Administrator defaults pre-filled

- **WHEN** the user opens the administrator step in the setup wizard
- **THEN** the username field defaults to `admin` and the password field defaults to `cat2bug`

#### Scenario: Administrator may keep default password

- **WHEN** the user submits setup without changing the default password `cat2bug`
- **THEN** the installation succeeds and login with `admin`/`cat2bug` works

#### Scenario: Administrator sets file storage path

- **WHEN** the user submits a writable directory for file storage
- **THEN** the install configuration persists `cat2bug.profile` to that path

#### Scenario: Administrator disables AI

- **WHEN** the user leaves AI disabled in the wizard
- **THEN** persisted configuration sets `cat2bug.ai.enabled` to `false`

#### Scenario: Local cache selected without Redis

- **WHEN** the user selects cache mode `local` regardless of database type
- **THEN** the wizard does not require Redis connection settings

#### Scenario: Redis cache requires connection test

- **WHEN** the user selects cache mode `redis`
- **THEN** the wizard requires Redis host/port and a successful connection test before submit

#### Scenario: Database and cache are independently combinable

- **WHEN** the user selects H2 database with Redis cache, or MySQL database with local cache
- **THEN** the wizard accepts the combination and persists both `spring.database-type` and `cat2bug.cache.type`

### Requirement: Setup performs connection tests before submit

The system SHALL provide APIs to test database connectivity, Redis connectivity (when cache mode is `redis`), Ollama reachability (when AI enabled), and filesystem writability for storage and log paths. Redis connectivity test MUST be invocable independently before setup submit, analogous to the database test API.

#### Scenario: Redis test succeeds

- **WHEN** cache mode is `redis` and the user invokes `POST /setup/test/redis` with valid host, port, and credentials
- **THEN** the API returns success without persisting install completion

#### Scenario: Redis test fails

- **WHEN** the user invokes `POST /setup/test/redis` with unreachable host or wrong password
- **THEN** the API returns failure with an error message and setup submit remains blocked until a successful test

#### Scenario: Redis test skipped for local cache

- **WHEN** cache mode is `local`
- **THEN** `POST /setup/test/redis` is not required and the wizard does not display Redis connection fields

#### Scenario: Database test succeeds

- **WHEN** the user runs database test with valid credentials
- **THEN** the API returns success without persisting install completion

#### Scenario: Database test fails

- **WHEN** the user runs database test with invalid credentials
- **THEN** the API returns failure with an error message and install is not completed

#### Scenario: Ollama test when AI enabled

- **WHEN** AI is enabled and the user runs Ollama test against a reachable host
- **THEN** the API returns success based on HTTP response from Ollama tags endpoint

### Requirement: Setup creates administrator and persists configuration

On successful submit, the system SHALL write `config/install/application-install.yml` (or configured path), run schema initialization via Flyway when needed, create the administrator account with the user-provided or default credentials, write `sys.account.registerUser` and `sys.account.captchaEnabled`, set `cat2bug.install.completed` to `true`. The system MUST NOT reject password `cat2bug`.

#### Scenario: Administrator account created on install

- **WHEN** setup submit succeeds with username `admin` and password `cat2bug`
- **THEN** login with `admin`/`cat2bug` succeeds

#### Scenario: Custom administrator credentials

- **WHEN** setup submit succeeds with a custom username and password
- **THEN** login with those credentials succeeds

#### Scenario: Install requires restart for infrastructure

- **WHEN** setup submit persists database type or paths requiring restart
- **THEN** the response instructs the user to restart the application before normal use

#### Scenario: Init password not in wizard

- **WHEN** the user completes setup
- **THEN** the wizard has not collected or changed `sys.member.initPassword`

### Requirement: Setup defaults login captcha to disabled

The system SHALL default `sys.account.captchaEnabled` to `false` when created by the setup wizard unless the user explicitly enables login captcha.

#### Scenario: Default captcha off after install

- **WHEN** the user completes setup without enabling login captcha
- **THEN** `sys.account.captchaEnabled` is `false` and login does not require a captcha code

#### Scenario: User enables login captcha in wizard

- **WHEN** the user enables login captcha in setup and submits
- **THEN** `sys.account.captchaEnabled` is `true`

### Requirement: Docker may skip setup when preconfigured

When environment variable `CAT2BUG_INSTALL_SKIP` is `true`, the system SHALL skip forcing the setup wizard if datasource and install completion are already configured.

#### Scenario: Docker compose with skip flag

- **WHEN** the container starts with `CAT2BUG_INSTALL_SKIP=true` and valid env-based datasource
- **THEN** the application does not redirect to `/setup`
