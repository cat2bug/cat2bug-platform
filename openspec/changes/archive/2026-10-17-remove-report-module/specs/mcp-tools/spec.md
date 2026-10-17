## MODIFIED Requirements

### Requirement: Curated tools cover the defect lifecycle and core project data

The system SHALL provide curated tools for searching and reading defects (including logs, comments and linked commits), creating, updating, assigning, repairing, passing, rejecting, closing, reopening and commenting on defects; searching, reading, creating and updating cases; listing and reading plans, deliverables and documents; and listing repositories, browsing trees, reading files, searching code, and reading commits. Each curated tool MUST apply the same business rules as the corresponding web action. The system MUST NOT provide `list_reports` or `get_report`.

#### Scenario: Pass a defect

- **WHEN** a tester calls the pass-defect tool on a defect in "pending verification" state
- **THEN** the defect moves to the same state as when passed on the web, and the handler is notified the same way

#### Scenario: Invalid state transition

- **WHEN** a client tries to pass a defect that is still in "processing" state
- **THEN** the tool returns the same error the web action would return

#### Scenario: Report tools are absent

- **WHEN** a client lists tools
- **THEN** the list does not contain `list_reports` or `get_report`
