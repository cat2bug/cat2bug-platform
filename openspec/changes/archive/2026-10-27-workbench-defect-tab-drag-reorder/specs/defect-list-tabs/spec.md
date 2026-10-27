## ADDED Requirements

### Requirement: Drag reorder custom defect tabs

The defect list page SHALL allow dragging custom tabs to change their order among custom tabs only. Built-in tabs「全部」and「已删除」and the add control MUST NOT be draggable. Dropping a custom tab onto or past a built-in tab or the add control MUST NOT reorder those fixed panes. Persisted order MUST use `tab_sort` (higher first among the member's custom tabs for the project) via `PUT /system/tabs/sort`. After reload, custom tabs MUST appear in the persisted order before「全部」and「已删除」.

#### Scenario: Reorder two custom tabs

- **WHEN** the member has at least two custom defect tabs and drags one past the other and drops
- **THEN** the custom tab order updates in the tab bar
- **THEN** after a full page reload the same custom order is restored
- **THEN**「全部」and「已删除」remain after all custom tabs

#### Scenario: Fixed panes not draggable

- **WHEN** the member attempts to drag「全部」or「已删除」or the add control
- **THEN** those panes do not reorder

#### Scenario: More menu does not start drag

- **WHEN** the member presses on the custom tab's more (⋯) menu trigger and moves the pointer
- **THEN** a tab drag is not started
- **THEN** the dropdown can still open on click
