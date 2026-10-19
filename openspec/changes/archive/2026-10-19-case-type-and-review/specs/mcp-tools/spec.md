## MODIFIED Requirements

### Requirement: Curated tools cover the defect lifecycle and core project data

The system SHALL provide curated tools for searching and reading defects (including logs, comments and linked commits), creating, updating, assigning, repairing, passing, rejecting, closing, reopening and commenting on defects; searching, reading, creating and updating cases; listing, creating, updating and deleting project case types; starting, commenting, completing and reading case reviews; listing and reading plans, deliverables and documents; and listing repositories, browsing trees, reading files, searching code, and reading commits. Case search MUST accept optional `case_type_id` and `review_status` filters. Case reads and writes MUST include the optional case type and the review stamp, using the same validation as the web. Each curated tool MUST apply the same business rules as the corresponding web action. The system MUST NOT provide `list_reports` or `get_report`, and MUST NOT treat a case review minute as a report-module record.

#### Scenario: Pass a defect

- **WHEN** a tester calls the pass-defect tool on a defect in "pending verification" state
- **THEN** the defect moves to the same state as when passed on the web, and the handler is notified the same way

#### Scenario: Invalid state transition

- **WHEN** a client tries to pass a defect that is still in "processing" state
- **THEN** the tool returns the same error the web action would return

#### Scenario: Report tools are absent

- **WHEN** a client lists tools
- **THEN** the list does not contain `list_reports` or `get_report`

#### Scenario: Case type is rejected when it belongs to another project

- **WHEN** a client calls `create_case` or `update_case` with a `case_type_id` from a different project
- **THEN** the tool returns the same error the web save would return and does not change the case

#### Scenario: Review filter does not block plan membership

- **WHEN** a client searches cases with `review_status` of `none` and then adds one of those cases to a plan through the same API the web uses
- **THEN** the search returns unreviewed cases and the plan save succeeds

### Requirement: Create operations support idempotency keys

`create_defect`, `create_case`, `comment_defect`, `create_case_type` and `start_case_review` SHALL accept an optional `idempotency_key`. A repeated call with the same user and key within 24 hours MUST return the original result without creating another record.

#### Scenario: Retry does not duplicate a defect

- **WHEN** a client calls `create_defect` twice with the same `idempotency_key`
- **THEN** only one defect exists and both calls return its ID

#### Scenario: Retry does not start a second review

- **WHEN** a client calls `start_case_review` twice with the same `idempotency_key` and the same case ids
- **THEN** only one open review exists and both calls return its id
