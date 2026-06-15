## ADDED Requirements

### Requirement: System detects first-run installation state

The system SHALL expose whether initial setup is complete. The system MUST treat the instance as installed when `cat2bug.install.completed` is `true` in the disk install file at `cat2bug.install.config-path`, or when install skip is enabled via `cat2bug.install.skip` or environment variable `CAT2BUG_INSTALL_SKIP=true`.

The system MUST treat the instance as not installed when the disk install file is absent, or when `cat2bug.install.completed` is absent or not `true`.

The system MUST NOT treat mere presence of the disk install file as installed.

When legacy data exists without a completed disk install file and upgrade is required, the system MUST NOT mark the instance as installed until the upgrade wizard completes successfully.

#### Scenario: Fresh deployment is not installed

- **WHEN** the application starts with no disk install file and install skip is false
- **THEN** `GET /setup/status` returns `installed: false`

#### Scenario: Completed installation is installed

- **WHEN** the disk install file contains `cat2bug.install.completed: true`
- **THEN** `GET /setup/status` returns `installed: true`

#### Scenario: Disk install file without completed flag is not installed

- **WHEN** a disk install file exists but `cat2bug.install.completed` is not `true`
- **THEN** `GET /setup/status` returns `installed: false`

#### Scenario: Legacy data with upgrade pending is not installed

- **WHEN** legacy data exists, no completed disk install file exists, upgrade skip is false, and upgrade is pending
- **THEN** `GET /setup/status` returns `installed: false` and the UI routes to `/upgrade`

#### Scenario: Developer first run uses wizard

- **WHEN** a developer starts the application from a fresh clone without a disk install file and without legacy data
- **THEN** the UI navigates to `/setup` and install file is created only after successful submit

### Requirement: Uninstalled instance routes users to setup

The system SHALL redirect unauthenticated clients to the setup wizard when not installed and upgrade is not required. When upgrade is required, the system MUST redirect to the upgrade wizard instead. API requests outside setup or upgrade endpoints MUST be rejected or redirected except for setup, upgrade, health, and static assets.

When no completed disk install file exists, the system MUST route users to `/setup` regardless of legacy data presence in the database. The system MUST NOT redirect such users to `/upgrade` solely because legacy admin or Flyway history exists.

#### Scenario: User opens application root before install

- **WHEN** `installed` is `false`, upgrade is not required, and the user navigates to `/`
- **THEN** the UI navigates to `/setup`

#### Scenario: Legacy database without install file shows setup

- **WHEN** legacy data exists, no completed install file exists, and upgrade skip is false
- **THEN** the user is redirected to `/setup` and not to `/upgrade`

#### Scenario: Setup API is accessible without login before install

- **WHEN** `installed` is `false`, upgrade is not required, and the client calls `POST /setup/submit` with valid payload
- **THEN** the request is accepted without authentication

#### Scenario: Setup API blocked after install

- **WHEN** `installed` is `true` and the client calls `POST /setup/submit`
- **THEN** the request is rejected with an appropriate error

#### Scenario: Setup API blocked during upgrade pending

- **WHEN** `upgradeRequired` is `true` and the client calls `POST /setup/submit`
- **THEN** the request is rejected with an appropriate error

### Requirement: Setup wizard is shown when installation is not completed

The system SHALL redirect unauthenticated and authenticated users to `/setup` when `cat2bug.install.completed` is not `true` and install skip is not enabled.

#### Scenario: Fresh empty database shows setup

- **WHEN** the database is empty and no install file exists
- **THEN** the user is redirected to `/setup`

#### Scenario: Install skip bypasses setup

- **WHEN** `CAT2BUG_INSTALL_SKIP=true` or `cat2bug.install.skip=true`
- **THEN** the setup wizard is not required

### Requirement: Database configuration step validates connectivity

The setup wizard database step SHALL collect database type, connection parameters, and database name. The user MUST successfully test database connectivity before proceeding. The test result MUST display whether the database is `new` or `existing`.

#### Scenario: User cannot proceed without successful database test

- **WHEN** the user has not passed database connectivity test
- **THEN** the wizard does not allow advancing past the database step

#### Scenario: Test result shows attach mode

- **WHEN** database test succeeds for an existing H2 or MySQL database
- **THEN** the UI indicates that setup will attach to an existing database without running migrations during install

### Requirement: Setup submit carries database attach mode

The setup submit API SHALL accept `databaseMode` (`new` or `existing`) alongside database connection fields. The frontend MUST send the mode returned by the latest successful database test for the same connection parameters.

#### Scenario: Submit includes database mode from test

- **WHEN** the user completes setup after a successful database test returning `existing`
- **THEN** the submit payload includes `databaseMode: existing`

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

On successful submit, the system SHALL write `config/install/application-install.yml` (or configured path) with **complete** infrastructure settings merged from classpath templates and wizard input, run schema initialization via Flyway when needed, create the administrator account with the user-provided or default credentials, write `sys.account.registerUser` and `sys.account.captchaEnabled`, set `cat2bug.install.completed` to `true` in the disk install file, and write `cat2bug.install.completed` to `sys_config` for audit. The system MUST NOT reject password `cat2bug`.

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

#### Scenario: Install file is self-contained after submit

- **WHEN** setup submit succeeds with MySQL and local cache
- **THEN** the disk install file contains datasource, `spring.database-type`, and `cat2bug.cache.type` without requiring `application-mysql.yml`

### Requirement: Setup defaults login captcha to disabled

The system SHALL default `sys.account.captchaEnabled` to `false` when created by the setup wizard unless the user explicitly enables login captcha.

#### Scenario: Default captcha off after install

- **WHEN** the user completes setup without enabling login captcha
- **THEN** `sys.account.captchaEnabled` is `false` and login does not require a captcha code

#### Scenario: User enables login captcha in wizard

- **WHEN** the user enables login captcha in setup and submits
- **THEN** `sys.account.captchaEnabled` is `true`

### Requirement: Docker may skip setup when preconfigured

When environment variable `CAT2BUG_INSTALL_SKIP` is `true`, or `cat2bug.install.skip` is `true`, the system SHALL skip forcing the setup wizard. Preconfiguration MUST be supplied via a disk install file with `cat2bug.install.completed: true` and/or environment property overrides for datasource and related settings.

#### Scenario: Docker compose with skip flag and install file

- **WHEN** the container starts with `CAT2BUG_INSTALL_SKIP=true` and a mounted install file with `completed: true`
- **THEN** the application does not redirect to `/setup`

#### Scenario: Skip without completed install still allows bootstrap

- **WHEN** `CAT2BUG_INSTALL_SKIP=true` and no disk install file exists
- **THEN** the application does not force `/setup` (existing skip semantics preserved)

### Requirement: Spring Native 安装向导 parity

首次安装向导的后端实现 MUST 继续由 **Spring MVC** 提供；在 **Spring Native embedded** 为默认 Release 时，对外 URL、步骤数、配置写入路径 `config/install/application-install.yml` 与 **安装后重启** 要求 MUST 与 JVM 版不变。

#### Scenario: Spring Native 提供安装 API

- **WHEN** Spring Native embedded 二进制启动且实例未安装
- **THEN** `GET /setup/status` 返回 `installed: false`
- **AND** 安装向导各步骤 API 可用且写入 install 文件格式与 JVM 版兼容

#### Scenario: Spring Native 安装后重启

- **WHEN** 经 Native 进程完成安装向导 submit
- **THEN** 必须重启 Native 进程后新 datasource/cache 配置生效
- **AND** 重启后 `GET /setup/status` 返回 `installed: true`

#### Scenario: Native 下 Setup 与 SPA 共存

- **WHEN** 未安装且用户访问 `/`
- **THEN** UI 导航至 `/setup` 且静态资源由同一 Native 进程提供

#### Scenario: JVM 安装行为不变（回归）

- **WHEN** 使用 JVM embedded JAR 执行安装流程
- **THEN** 行为与 Native 场景一致
