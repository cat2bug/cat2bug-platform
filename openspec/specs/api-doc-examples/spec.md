## ADDED Requirements

### Requirement: Example placement per endpoint

Every Open API HTTP endpoint section in `readme/production/api/api-*.md` (excluding purely descriptive sections without method and path) SHALL end with a `::: code-tabs` block placed after Request/Response parameter tables.

#### Scenario: Endpoint section ends with code-tabs

- **WHEN** a reader opens an endpoint section that documents `**方法**` and `**路径**`
- **THEN** the last content block before the next `##` heading or document end SHALL be a `code-tabs` example

### Requirement: Placeholders in all examples

Every code sample in API documentation examples SHALL use `${baseUrl}` for the service root (no trailing slash) and `${apiKey}` for the `CAT2BUG-API-KEY` header value, unless the sample is intentionally illustrating a missing header error.

#### Scenario: Consistent placeholder usage

- **WHEN** any language panel in an API example is read
- **THEN** the request URL SHALL be formed with `${baseUrl}` plus the documented path
- **AND** authenticated requests SHALL include header `CAT2BUG-API-KEY: ${apiKey}`

### Requirement: baseUrl deployment guidance

`readme/production/api/api-intro.md` SHALL document how to choose `${baseUrl}` for common deployments (local default port, Docker-mapped port, reverse proxy / HTTPS) and SHALL NOT mandate a single hardcoded production hostname for all users.

#### Scenario: Reader finds deployment address guidance

- **WHEN** a reader consults API introduction before calling endpoints
- **THEN** they SHALL find explicit guidance for setting `${baseUrl}` according to their deployment

### Requirement: Language implementation stacks

API examples SHALL use the following stacks: cURL CLI; Java `java.net.http.HttpClient` (JDK standard library); Python standard library (`urllib.request`); Node.js global `fetch` with Node 18+ noted in intro; PHP `curl_*` with php-curl noted in intro; C# `System.Net.Http.HttpClient`.

#### Scenario: Java example uses JDK HttpClient

- **WHEN** the Java tab is shown for any endpoint
- **THEN** the sample SHALL use `java.net.http.HttpClient` (or equivalent JDK 11+ standard API) without third-party HTTP client libraries

#### Scenario: Node example uses fetch

- **WHEN** the Node.js tab is shown for any endpoint
- **THEN** the sample SHALL use the global `fetch` API
- **AND** `api-intro.md` SHALL state the Node.js 18+ requirement

### Requirement: Simultaneous six-language delivery

A change that adds or updates an API endpoint's examples SHALL NOT be considered complete until all six language panels are present and accurate for that endpoint.

#### Scenario: Validation rejects incomplete tabs

- **WHEN** a documentation validation script runs on `readme/production/api/`
- **THEN** any endpoint section missing one or more of the six required language fences SHALL cause validation failure

### Requirement: Coverage of documented Open API endpoints

All HTTP operations documented in production API markdown files SHALL have six-language examples, including at minimum: deliverable, case, defect, report, member, file upload, and project endpoints as currently listed in `api-intro.md`.

#### Scenario: Full API surface covered

- **WHEN** validation runs after the change is applied
- **THEN** each endpoint linked from `api-intro.md` interface list SHALL have a complete `code-tabs` block at the end of its section
