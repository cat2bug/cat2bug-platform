# cloud-social-login Specification

## Requirements

### Requirement: WeChat and DingTalk authorization URL

When cloud social login is enabled, the system MUST return an authorization URL for source `wechat` or `dingtalk`. The callback URL MUST be `https://cloud.cat2bug.com/three-login` with query `source` set to that source. The system MUST reject any other source.

#### Scenario: DingTalk authorization URL

- **WHEN** a client requests the authorization URL for `dingtalk`
- **THEN** the response contains an authorization URL whose redirect URI is `https://cloud.cat2bug.com/three-login?source=dingtalk`

#### Scenario: Unknown source

- **WHEN** a client requests the authorization URL for a source other than `wechat` or `dingtalk`
- **THEN** the system rejects the request

### Requirement: Social login issues a token only when bound

The system MUST exchange the provider code for a platform login token only when that provider identity is already bound in `c_auth_user`. When it is not bound, the system MUST NOT create a session and MUST return an unbound result the UI can send to `/three-bind`.

#### Scenario: Bound WeChat user logs in

- **WHEN** a WeChat callback identity matches a row in `c_auth_user`
- **THEN** the system returns the same kind of login token as username-password login

#### Scenario: Unbound DingTalk user

- **WHEN** a DingTalk callback identity has no row in `c_auth_user`
- **THEN** the system does not issue a login token and indicates the identity is unbound

### Requirement: Bind, register-bind, and unbind

An authenticated user MUST be able to bind one unbound WeChat or DingTalk identity to the current account, or register a new account and bind it in one request. A user MUST be able to unbind one of their own `c_auth_user` rows. The system MUST NOT bind the same provider identity to a second user.

#### Scenario: Bind to current account

- **WHEN** an authenticated user submits a valid unbound provider code for `wechat` or `dingtalk`
- **THEN** `c_auth_user` links that identity to the current user

#### Scenario: Identity already bound

- **WHEN** a user tries to bind a provider identity that already belongs to another user
- **THEN** the system rejects the bind and leaves both rows unchanged

### Requirement: WeChat mini program openid binding survives logout

Logging out MUST NOT clear the WeChat mini program openid stored for the user. A later mini program login with the same openid MUST still resolve to that user.

#### Scenario: Logout keeps openid

- **WHEN** a user who has a WeChat mini program openid logs out
- **THEN** the stored openid is unchanged and a later mini program login with that openid authenticates the same user
