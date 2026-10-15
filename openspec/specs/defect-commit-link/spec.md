# defect-commit-link Specification

## Requirements
### Requirement: Defect repair can link commits

Repairing a defect, through MCP or the web, SHALL accept an optional list of commits. Each linked commit MUST be stored with the defect, the repair log entry it belongs to, the commit SHA, and either the hosted repository or an external URL.

#### Scenario: Repair with a hosted commit

- **WHEN** a user repairs a defect and links commit `abc1234` from a repository hosted in the same project
- **THEN** the defect log entry for the repair shows the commit, and opening it shows the commit's changes in the repository browser

#### Scenario: Repair with an external commit

- **WHEN** a user repairs a defect and links a commit SHA with a GitHub URL
- **THEN** the commit is stored with the URL and displayed as an external link

### Requirement: Hosted commits are verified

For repositories hosted on the platform, the system MUST verify that the commit exists in a repository of the defect's project and that the user has pull permission on that repository. If verification fails, the whole repair MUST be rejected and nothing is changed.

#### Scenario: Unknown commit is rejected

- **WHEN** a repair links a SHA that does not exist in the project's hosted repository
- **THEN** the repair fails with a "commit not found" message and the defect state is unchanged

#### Scenario: External SHA format is checked

- **WHEN** an external commit with a malformed SHA is linked
- **THEN** the repair fails with an invalid-commit message

### Requirement: Linked commits are visible on the defect

Defect detail, both on the web and in `get_defect`, SHALL include linked commits with SHA, message, author time (for hosted commits), and repository or URL.

#### Scenario: AI reads linked commits

- **WHEN** a client calls `get_defect` for a defect repaired with linked commits
- **THEN** the result lists those commits under the corresponding repair log entry
