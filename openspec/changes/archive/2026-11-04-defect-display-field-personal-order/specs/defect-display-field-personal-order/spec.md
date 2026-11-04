## ADDED Requirements

### Requirement: Display-field picker supports drag reorder of all configurable fields

The defect table「显示字段」popover MUST allow the user to reorder every configurable field row (including unchecked rows) by dragging. Releasing a drag MUST update the live table column order to match the picker list order (visible columns only affect which columns render, not relative order among all configurable keys). Checkbox clicks MUST NOT start a drag.

#### Scenario: Drag unchecked field between checked fields

- **WHEN** the user opens「显示字段」and drags an unchecked field between two checked fields, then releases
- **THEN** the table column order among all configurable columns matches the new picker order
- **THEN** previously unchecked fields remain hidden until checked

#### Scenario: Drag does not toggle visibility

- **WHEN** the user drags a field by its drag handle without clicking the checkbox
- **THEN** that field's checked/visible state does not change solely because of the drag

### Requirement: Personal column order persists across refresh

The client MUST persist the user's configurable column key order in the user-scoped defect table field-list local cache (same store as visible/fixed/width). After a full page reload, the defect table MUST restore that personal order. The team `orderedEnabledFieldKeys` / field `sortOrder` MUST be used as the default order only when the user has no stored order for existing keys, and as the placement rule for newly introduced team fields.

#### Scenario: Reload keeps personal order

- **WHEN** the user reorders columns (via picker or table header) and reloads the defect list page
- **THEN** the table columns appear in the same personal order as before reload

#### Scenario: No personal order uses team default

- **WHEN** a user has no stored column order entries for the current enabled fields
- **THEN** the table uses the team `orderedEnabledFieldKeys` order

### Requirement: Header drag and picker drag share one personal order

Dragging a column header on the defect `Cat2BugTable` MUST update the「显示字段」list order in the same session and MUST write the same personal order cache that the picker uses. Changing order in the picker MUST be reflected on the table without requiring a reload.

#### Scenario: Header drag updates picker list

- **WHEN** the user reorders columns by dragging table headers
- **THEN** the「显示字段」list order matches the new table order without closing and reopening the page

#### Scenario: Picker drag updates table

- **WHEN** the user reorders fields in「显示字段」and the popover remains open or is reopened
- **THEN** the table columns already follow that order

### Requirement: New team fields insert after personal predecessor

When merging team column defaults with personal cache, any enabled field key present in team defaults but absent from the personal order MUST be inserted immediately after the nearest preceding key in the team default order that already appears in the personal order output; if no such predecessor exists, the new key MUST be appended at the end of the merged order. Keys present only in personal cache and not in defaults MUST be dropped.

#### Scenario: New field after known predecessor

- **WHEN** the team adds enabled field B whose team order is A → B → C, and the user's personal order is A → C
- **THEN** after merge the order is A → B → C

#### Scenario: Removed field dropped

- **WHEN** the user's personal order contains key X but X is no longer in team defaults
- **THEN** X does not appear in the merged column list

### Requirement: Excel shares personal order and supports picker drag

The Excel defect view MUST read and write the same user-scoped field-list cache as the table view for column visibility and personal order. Its「显示字段」panel MUST support the same drag-reorder behavior, and reordering there MUST update Excel data columns and the shared cache so the table view sees the same order after switching views or reloading.

#### Scenario: Excel drag visible on table after switch

- **WHEN** the user reorders fields in Excel「显示字段」then switches to the table view (or reloads)
- **THEN** the table columns follow that personal order

#### Scenario: Legacy excel cache key migrates

- **WHEN** only a legacy non-user-prefixed field-list cache exists
- **THEN** the client migrates preferences into the user-scoped key and subsequent writes use the user-scoped key

### Requirement: Forms and team admin stay on team order

Create/edit defect forms and the team defect-field admin sortOrder MUST continue to follow team column-layout / `sortOrder` only. Personal display-field order MUST NOT change form field order or team-wide defaults for other users.

#### Scenario: Form ignores personal list order

- **WHEN** the user has a personal table column order different from the team default
- **THEN** opening create or edit defect still presents fields in the team layout order

#### Scenario: Another user sees team default

- **WHEN** user A persists a personal column order
- **THEN** user B with no personal order still sees the team default column order on the defect table
