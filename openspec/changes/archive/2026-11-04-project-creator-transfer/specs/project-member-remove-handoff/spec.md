## ADDED Requirements

### Requirement: Project creator cannot be removed until transferred

The system MUST reject removing a project member who currently holds `project.create-by`. Preview and delete APIs SHALL leave membership, defects, and repository ACL unchanged when the target is the project creator. Callers MUST transfer the creator role before the former creator can be removed via the existing handoff flow.

#### Scenario: Delete creator rejected

- **WHEN** an authorized user attempts to remove the member who holds `project.create-by`
- **THEN** the system rejects the request
- **THEN** the user remains a project member

#### Scenario: Former creator removable after transfer

- **WHEN** the previous creator no longer holds `project.create-by` after a successful transfer
- **THEN** that user MAY be removed via the existing preview/handoff delete flow subject to normal rules
