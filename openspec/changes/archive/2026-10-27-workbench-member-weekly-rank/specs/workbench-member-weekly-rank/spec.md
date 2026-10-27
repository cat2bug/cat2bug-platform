## ADDED Requirements

### Requirement: Weekly member contribution widget presentation

The workbench SHALL provide a catalog widget with stable id `member-weekly-rank` in group `member`. The widget MUST NOT present ranking positions (no ordinal badges or podium styling). Each row MUST show the member avatar on the left and, on the right, the member display name above three progress bars for defect-handled count, code-commit count, and document-handled count in that order. Each progress bar MUST use a distinct visual status or color. The widget MUST display the current week date range consistent with other workbench weekly widgets.

#### Scenario: Row layout shows three metrics

- **WHEN** a member opens a workbench surface that includes `member-weekly-rank` and the API returns at least one member
- **THEN** each row shows avatar, display name, and three labeled progress bars with numeric counts for defect, commit, and document metrics, and MUST NOT show a rank number

#### Scenario: Empty week

- **WHEN** the API returns an empty members list for the current week
- **THEN** the widget shows an empty state and MUST NOT fabricate rows

### Requirement: List order and progress percentages (no ranking)

The system MUST NOT order members by descending total for ranking. Members MUST be ordered by display name (case-insensitive), then `userId` for stability. Progress-bar percentages MUST be computed per metric against that metric’s maximum among returned members for the same week (zero when the maximum is zero). The API MAY still return a raw sum field for each member; the UI MUST NOT treat or label it as a competitive score.

#### Scenario: Name order not total order

- **WHEN** member Zoe has weekly totals (defects=2, commits=1, documents=1) and member Amy has (defects=1, commits=1, documents=0)
- **THEN** Amy appears above Zoe because display names sort alphabetically, regardless of 4 > 2

#### Scenario: Progress relative to weekly max

- **WHEN** the highest defect count among returned members is 10 and a member has 5 defects
- **THEN** that member’s defect progress bar percentage is 50

### Requirement: Metric definitions and week window

The week window MUST be the same half-open ISO week used by workbench week-ops: Monday 00:00 through next Monday 00:00. Defect-handled count MUST be the number of distinct project defects with a `sys_defect_log` entry created by the member inside the week. Code-commit count MUST be the number of Git commits in the project’s code repositories whose author time falls in the week and whose author matches the member by the same rules as repository contributor matching. Document-handled count MUST be the number of distinct project documents for which the member is `create_by_id` with `create_time` in the week or `update_by_id` with `update_time` in the week. Members with a total of zero MUST be omitted from the list.

#### Scenario: Week boundary shared with week-ops

- **WHEN** the client requests member weekly contribution for a project
- **THEN** the response weekStart and weekEnd describe the same current week window as `week-ops` for that server clock

#### Scenario: Zero-total members omitted

- **WHEN** a project member has zero defects, commits, and documents in the week
- **THEN** that member is not included in the members list

### Requirement: Aggregated dashboard API

The system SHALL expose an authenticated project dashboard query that returns the current week range and the ordered member list with the three counts and total for each member. The endpoint MUST require `system:dashboard:query`. Git or document subsystem failures for a repository or document query MUST NOT fail the entire response; affected counts MAY degrade to zero with other metrics still returned.

#### Scenario: Authorized query succeeds

- **WHEN** a user with `system:dashboard:query` requests member weekly contribution for a project they can access
- **THEN** the system returns weekStart, weekEnd, and a members array sorted by display name ascending

#### Scenario: Partial Git degradation

- **WHEN** one repository’s commit walk fails while defect and document aggregates succeed
- **THEN** the response still returns 2xx with members ordered by display name on available counts and commit counts treated as zero for the failed repository’s contribution
