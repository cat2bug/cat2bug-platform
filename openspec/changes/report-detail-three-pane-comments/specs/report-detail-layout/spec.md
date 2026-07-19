## ADDED Requirements

### Requirement: Report detail uses a three-pane shell
The report detail view (`ViewReport`) SHALL present a three-pane layout with a left table of contents, a center document body, and a right comments panel. The report template editor SHALL NOT gain this layout or annotation features as part of this capability.

#### Scenario: Open saved report detail
- **WHEN** a user opens a saved report from the report list (including deep link with `reportId`)
- **THEN** the detail UI shows left TOC, center markdown body, and right comments regions

#### Scenario: Template editor unchanged
- **WHEN** a user opens the report template editor
- **THEN** the editor does not show the report-detail three-pane shell or report annotation panel

### Requirement: Table of contents navigates to heading anchors
The system SHALL build a TOC from heading elements (`h1`–`h6`) in the rendered report body, assign stable anchor ids, and scroll the center pane to the target when a TOC item is activated. The TOC SHALL highlight the section corresponding to the current scroll position when headings exist.

#### Scenario: Click TOC item
- **WHEN** the rendered body contains headings and the user clicks a TOC entry
- **THEN** the center pane scrolls so the matching heading is visible

#### Scenario: No headings
- **WHEN** the rendered body has no headings
- **THEN** the TOC shows an empty state and does not error

#### Scenario: Scroll spy
- **WHEN** the user scrolls the center pane across multiple headings
- **THEN** the TOC highlights the heading currently in view

### Requirement: Side panes can collapse on narrow layouts
The system SHALL allow the TOC and comments panes to be collapsed or overlaid so the center document remains usable on narrow widths inside the detail drawer.

#### Scenario: Collapse comments pane
- **WHEN** the user collapses the comments pane
- **THEN** the center document expands into the freed space and content remains readable

### Requirement: Export uses document body only
Word and PDF export SHALL serialize only the center document content and SHALL NOT include TOC chrome, comments UI, or annotation floating controls as exported body content.

#### Scenario: Export Word or PDF
- **WHEN** the user exports the open report as Word or PDF
- **THEN** the export is based on the center report body without the side-pane chrome
