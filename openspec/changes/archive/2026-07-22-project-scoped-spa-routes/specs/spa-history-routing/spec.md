## ADDED Requirements

### Requirement: Browser uses History URLs without hash

The product UI SHALL use HTML5 History routing so canonical application paths appear as `https://{host}/{path}` without a `#` fragment segment for route state. Share links and in-app navigation MUST use these hash-free paths for both embedded (Spring-served static) and reverse-proxied (e.g. nginx) deployments.

#### Scenario: Open login without hash

- **WHEN** a user navigates to the login page on an installed instance
- **THEN** the browser location path is `/login` (or the deployment’s equivalent) and MUST NOT require `/#/login` as the canonical form

#### Scenario: Share project path without hash

- **WHEN** the UI builds a copyable project path or absolute share URL
- **THEN** the URL uses History path segments and MUST NOT insert `#` before `/{teamKey}/{projectKey}`

### Requirement: SPA fallback serves index for History deep links

For GET requests that accept HTML, the server (embedded SPA fallback and/or nginx `try_files`) SHALL return the SPA `index.html` for unknown front-end paths so client-side routes resolve after refresh. The fallback MUST NOT intercept Git Smart HTTP paths under `/{teamKey}/{projectKey}/code/{slug}.git/**`, nor static/API prefixes already excluded from SPA handling.

#### Scenario: Refresh business deep link

- **WHEN** an authenticated user refreshes a History URL such as `/{teamKey}/{projectKey}/defect`
- **THEN** the server returns the SPA shell and the client router restores the defect view

#### Scenario: Git Smart HTTP not swallowed by SPA

- **WHEN** a Git client requests `/{teamKey}/{projectKey}/code/{slug}.git/info/refs` (or other Smart HTTP endpoints under that prefix)
- **THEN** the request is handled by Git Smart HTTP and MUST NOT receive the SPA `index.html` as the primary response
