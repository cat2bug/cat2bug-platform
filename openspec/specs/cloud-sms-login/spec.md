# cloud-sms-login Specification

## Requirements

### Requirement: Send a one-time SMS code

When cloud SMS login is enabled, the system MUST send a one-time verification code to a mainland China mobile number through Huawei SMS over TLS-verified HTTPS. The code MUST expire, MUST be single-use, and MUST be rate-limited per phone number and per client IP. The system MUST NOT disable TLS certificate verification.

#### Scenario: Code is sent

- **WHEN** a client requests a code for a valid mobile number and is within the rate limit
- **THEN** Huawei SMS is called with the configured template and the code can be used once before it expires

#### Scenario: Rate limit

- **WHEN** a client requests codes for the same number faster than the limit
- **THEN** the system rejects the request and does not send another SMS

### Requirement: SMS login issues a platform token

The system MUST accept `POST /sms/login` with phone and code. On a matching unexpired code it MUST issue the same kind of login token as username-password login and invalidate the code. On a wrong or expired code it MUST reject the login and MUST NOT issue a token.

#### Scenario: Correct code

- **WHEN** a client submits the phone number and the current code
- **THEN** the system returns a login token and the same code cannot be used again

#### Scenario: Wrong code

- **WHEN** a client submits a wrong or expired code
- **THEN** the system rejects the login and does not set a session

### Requirement: Login page offers SMS

The cloud login page MUST show an SMS login entry that requests a code and submits it to `POST /sms/login`. A file named with a `.temp` suffix MUST NOT be the shipped entry.

#### Scenario: User completes SMS login from the page

- **WHEN** a user opens the cloud login page, chooses SMS, receives a code, and submits it
- **THEN** the UI stores the returned token and enters the app as a logged-in user
