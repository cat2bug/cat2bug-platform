# defect-list-tabs Specification

## Requirements

### Requirement: Custom defect tab dropdown actions

Custom defect tabs on the defect list page SHALL show a single bordered「more」dropdown trigger icon after the tab name only while the tab item is hovered (or while its menu is open / the tab is keyboard-focused for navigation). Clicking the trigger MUST open a menu with edit and delete actions (each with a leading icon). Built-in tabs「全部」and「已删除」MUST NOT show the dropdown. The trailing add control behavior is unchanged. Clicking the dropdown trigger or menu items MUST NOT by itself change the selected tab.

#### Scenario: Hover reveals bordered more trigger

- **WHEN** the user hovers a custom defect tab
- **THEN** a bordered more (three-dot) trigger is visible on that tab
- **THEN** built-in「全部」and「已删除」tabs still show no dropdown

#### Scenario: Open menu on custom tab

- **WHEN** the user clicks the more trigger on a custom defect tab
- **THEN** a menu with edit and delete actions (with leading icons) is shown

### Requirement: Edit custom defect tab via dialog

The system SHALL allow editing a custom defect tab through the same dialog used for create, opened from the tab menu's edit action. The dialog MUST prefill tab name and filter config from the existing tab. On successful save the client MUST call the existing update tabs API (`PUT /system/tabs`), update the in-memory tab list, and if the edited tab is the active tab MUST re-apply that tab's query through the existing single-list apply path.

#### Scenario: Edit active tab filters

- **WHEN** the user opens edit on the active custom tab, changes a filter field, and confirms
- **THEN** the tab config is persisted via update
- **THEN** the defect list is refreshed once with the updated filters

#### Scenario: Edit inactive tab name only

- **WHEN** the user opens edit on a non-active custom tab, changes only the tab name, and confirms
- **THEN** the tab name in the tab bar updates
- **THEN** the active tab selection and its list query are unchanged

#### Scenario: Cancel edit

- **WHEN** the user opens edit and cancels or closes the dialog without saving
- **THEN** no update API call is made
- **THEN** tab name and config remain unchanged

### Requirement: Delete custom defect tab

Deleting a custom defect tab MUST continue to require confirmation and then call the existing delete tabs API. After delete, if the deleted tab was active, the client MUST select another custom tab if any remain, otherwise「全部」, and apply that selection. Custom tabs MAY all be deleted; built-in tabs remain. Clicking edit or delete MUST NOT change the selected tab by itself (pointer events stopped on the action controls).

#### Scenario: Delete active custom tab

- **WHEN** the user confirms delete on the active custom tab and at least one other custom tab exists
- **THEN** the tab is removed
- **THEN** another remaining custom tab (or「全部」if none) becomes active and its query is applied

#### Scenario: Delete all custom tabs

- **WHEN** the user deletes the last custom tab
- **THEN**「全部」and「已删除」remain available
- **THEN** the active selection is「全部」if the deleted tab was active
