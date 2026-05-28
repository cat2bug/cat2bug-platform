## ADDED Requirements

### Requirement: Login captcha switch applies only to login

The system SHALL use `sys.account.captchaEnabled` exclusively to control whether login requires captcha validation. Registration MUST NOT require captcha validation based on this flag.

#### Scenario: Captcha disabled login has no captcha

- **WHEN** `sys.account.captchaEnabled` is `false` and the user submits login credentials
- **THEN** login succeeds without captcha code validation

#### Scenario: Captcha enabled login requires captcha

- **WHEN** `sys.account.captchaEnabled` is `true` and the user submits login without a valid captcha
- **THEN** login is rejected

#### Scenario: Captcha enabled does not affect registration

- **WHEN** `sys.account.captchaEnabled` is `true` and the user submits registration with valid required fields but no captcha
- **THEN** registration proceeds without captcha validation

### Requirement: Captcha image endpoint reflects login-only scope

The `/captchaImage` endpoint SHALL report `captchaEnabled` according to `sys.account.captchaEnabled` for login use. The registration page MUST NOT display or require captcha fields.

#### Scenario: Login page loads captcha when enabled

- **WHEN** `sys.account.captchaEnabled` is `true` and the login page loads
- **THEN** the login form displays captcha input and loads an image from `/captchaImage`

#### Scenario: Register page never shows captcha

- **WHEN** the registration page loads regardless of `sys.account.captchaEnabled`
- **THEN** the registration form does not display captcha input

### Requirement: Default captcha state is disabled for new installations

For databases initialized by the setup wizard, the system SHALL set `sys.account.captchaEnabled` to `false` unless the installer explicitly enables it.

#### Scenario: Seed default remains false for migrated empty flag

- **WHEN** `sys.account.captchaEnabled` is missing from `sys_config`
- **THEN** `selectCaptchaEnabled()` returns `false` (changed from previous default-true fallback when empty)

#### Scenario: Setup wizard explicit default

- **WHEN** a new installation completes via setup wizard without toggling captcha
- **THEN** stored value is `false`

### Requirement: Admin parameter label reflects login-only semantics

The system management UI for `sys.account.captchaEnabled` SHALL describe the setting as login captcha (not registration captcha).

#### Scenario: Parameter setting description

- **WHEN** an administrator views system parameters for captcha
- **THEN** the label or remark indicates the switch controls login verification only
