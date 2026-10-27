# workbench-widget-catalog Specification

## Requirements

### Requirement: Catalog grouped by product domain

The workbench widget catalog in edit mode SHALL present catalog-visible widgets grouped by product business domain (stable group ids such as overview, defect, case, document, git, plan, member, deliverable, notice). Each widget definition exposed in the catalog MUST declare a group. Group headers MUST be localized. Groups MUST be visually separated and MAY collapse and expand. Widgets that differ only by personal vs team scope MUST still share the same product-domain group when they belong to that domain.

#### Scenario: Open catalog shows domain sections

- **WHEN** a member opens workbench edit mode
- **THEN** the left catalog lists widgets under domain group headers rather than a single flat chip list
- **THEN** every catalog-visible widget appears under exactly one group

#### Scenario: Collapse a group

- **WHEN** a member collapses a domain group in the catalog
- **THEN** that group's widget entries are hidden until the group is expanded again

### Requirement: Live widget preview scaled by width

Each catalog entry MUST preview the **actual widget component** (same component and binding used on the canvas). The preview MUST render the widget at a fixed design width suitable for its layout, then **uniformly scale** it to the catalog card width so proportions match the canvas (for example a four-column KPI row stays a horizontal row, not squeezed vertical strips). Preview frame height MUST be the scaled content height and MUST NOT be forced equal across all cards. Static illustrative/mock images MUST NOT be used as the primary catalog preview. Collapsed groups SHOULD unmount their preview instances so widgets are not all fetched at once.

#### Scenario: Preview matches canvas widget type

- **WHEN** a member expands a catalog group containing `member-defect-line`
- **THEN** the catalog card for that type renders the same chart host/widget used on the workbench canvas (not a generic line-drawing mock)

#### Scenario: KPI preview keeps horizontal proportions

- **WHEN** the catalog shows `my-kpi` in a narrow sidebar
- **THEN** the four KPI tiles remain arranged horizontally in the preview (scaled down)
- **THEN** text and numbers are not compressed into vertical strips

#### Scenario: Preview heights may differ by type

- **WHEN** a catalog group shows both a KPI-style widget and a chart widget
- **THEN** their preview card heights MAY differ according to each scaled widget's content
- **THEN** the catalog MUST NOT stretch shorter widgets to match taller ones

### Requirement: Used-on-page marker is advisory

The catalog SHALL mark a widget type as used when at least one instance of that `type` exists anywhere on the surface currently being edited (all rows and columns of that surface, after normalizing legacy singular `widget` into `widgets`). The mark MUST be visual only (for example a badge or “已添加” label). Marked types MUST remain draggable from the catalog onto columns that do not already contain that `type`. The catalog MUST NOT enforce whole-surface uniqueness of `type`.

#### Scenario: Mark when type exists on surface

- **WHEN** the edited surface already contains one `my-defects` instance in any column
- **THEN** the catalog entry for `my-defects` shows the used-on-page mark

#### Scenario: Mark clears after last instance removed

- **WHEN** the member removes the last instance of a type from the edited surface
- **THEN** that type's catalog entry no longer shows the used-on-page mark

#### Scenario: Marked type still placeable in another column

- **WHEN** a type is marked used and the member drags it onto a different column that does not already contain that type
- **THEN** placement succeeds per existing column uniqueness rules
- **THEN** the used-on-page mark remains visible

### Requirement: Shared catalog component for editors

The grouped catalog with previews and used-on-page marks MUST be implemented as a reusable UI component that accepts the set of used types (or enough layout data to derive them) and exposes the same catalog drag payload contract used by the row layout editor. Any workbench edit surface that presents a widget catalog MUST use this component rather than a one-off flat list.

#### Scenario: Row editor hosts shared catalog

- **WHEN** a member edits a personal workbench or custom tab via the row layout editor
- **THEN** the left-side catalog is the shared catalog component

#### Scenario: Drag payload unchanged for drop handlers

- **WHEN** a member starts dragging a catalog entry from the shared catalog
- **THEN** the drag data identifies the widget type in the same catalog contract the row editor drop handler already expects

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
