## ADDED Requirements

### Requirement: Catalog search by display name

The workbench widget catalog MUST provide a search field that filters catalog entries by the member's current-locale display name (localized title). Matching MUST be case-insensitive substring match. The catalog MAY also match against the widget type id. When the query is non-empty, the catalog MUST show only groups that contain at least one matching widget, and MUST present those matching groups in an expanded state so results are visible without further clicks. When no widgets match, the catalog MUST show an empty-state message. Clearing the query MUST restore the catalog's non-search accordion behavior (at most one group open at a time). Search MUST NOT change drag payload, used-on-page mark semantics, or preview scaling rules.

#### Scenario: Filter by localized name

- **WHEN** a member types a substring of a widget's localized title into the catalog search field
- **THEN** only widgets whose title (or type id) contains that substring remain listed
- **THEN** groups with no matches are hidden

#### Scenario: Matching groups expand while searching

- **WHEN** the search query matches widgets in more than one domain group
- **THEN** each group that still has matches is shown expanded

#### Scenario: Empty search results

- **WHEN** the search query matches no catalog-visible widget
- **THEN** the catalog shows an empty-state message and no widget cards

#### Scenario: Clear search restores single-open accordion

- **WHEN** the member clears the search query after filtering
- **THEN** the catalog again keeps at most one group open at a time
