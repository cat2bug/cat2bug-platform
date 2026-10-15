## ADDED Requirements

### Requirement: PR commit list shows author avatar

The PR detail commits tab SHALL display each commit author's avatar using the same project-member matching rules as repository browse and commit detail (match by author name / nickname / email to a project member; if unmatched, show a textual placeholder avatar). The author display name MUST remain visible next to the avatar.

#### Scenario: Matched member shows avatar

- **WHEN** a user with pull permission opens a PR whose commit author email matches a project member
- **THEN** the commits list shows that member's avatar beside the commit message metadata

#### Scenario: Unmatched author still readable

- **WHEN** a commit author does not match any project member
- **THEN** the commits list still shows the author name with a placeholder avatar and does not hide the commit

### Requirement: PR description rendered as sanitized Markdown

The PR detail description SHALL be rendered as sanitized Markdown (not a raw monospace dump of the entire body), consistent with PR discussion comments and ADR-012. Empty body MAY hide the description block.

#### Scenario: Markdown body renders

- **WHEN** a PR body contains Markdown headings or lists
- **THEN** the detail view renders them as Markdown after sanitization
