# defect-tab-list-fetch Specification

## Purpose
TBD - created by archiving change defect-tab-single-list-on-restore. Update Purpose after archive.
## Requirements
### Requirement: Local cache stores only defect tab id

The defect page SHALL persist only the active tab identifier (tabId or built-in names such as `all-tab`) in local storage, keyed per project when a project id is available. The page MUST NOT persist the full tab query config or `queryParams` snapshot for the purpose of restore.

#### Scenario: Refresh keeps tab id without caching filters

- **WHEN** the user selects a custom tab such as「我的」and the page writes local cache
- **THEN** the stored value is the tab id (or project-scoped key containing that id), not the tab's filter fields such as `handleBy` or `defectStates`

#### Scenario: Deleted tab is not persisted

- **WHEN** the user selects the「已删除」tab
- **THEN** the page MUST NOT overwrite the last non-deleted tab id in local cache with `deleted-tab`

### Requirement: Restore waits for server tab config before listing

On full page reload of the defect page, the system SHALL read the cached tab id for UI selection, SHALL fetch defect tab configuration from the server, and SHALL apply that tab's server-side config to the query before issuing the defect list request. The system MUST NOT issue a defect list request with default/empty filters solely to "warm" the list before tab config is applied.

#### Scenario: Cached「我的」tab restores filters from config then lists once

- **WHEN** local cache holds the tab id for「我的」and the user refreshes the defect page
- **THEN** after `configDefect` (or equivalent) returns tabs including that id, the page applies that tab's config and issues exactly one defect list request for that restore path

### Requirement: Programmatic tab apply does not echo into extra list requests

When the defect page programmatically applies a tab's query (restore from cache, user-driven tab change, or reset to「全部」/「已删除」), form controls bound to the query MUST NOT trigger additional defect list requests via change/input handlers. The page SHALL load list data through a single scheduled load for that apply.

#### Scenario: Switching to「我的」causes one list request

- **WHEN** the user clicks from「全部缺陷」to「我的」and the tab config sets `handleBy` and/or `defectStates`
- **THEN** the network shows one defect list request for that switch (excluding unrelated APIs such as statistics)

#### Scenario: Manual query change still lists

- **WHEN** the user manually changes a query control after the page is idle (not during programmatic tab apply)
- **THEN** the page still issues a defect list request according to existing query UX

