## ADDED Requirements

### Requirement: Tab more menu does not start drag

On workbench custom tabs that show a more (⋯) menu, pointer interaction that begins on the more menu trigger MUST NOT start a tab drag. Drag reorder of custom tabs within a visibility group (existing requirement) MUST remain possible when the pointer starts on the tab label outside the more menu. The Sortable filter used to decide draggability MUST evaluate the draggable tab item element, not the pointer event object.

#### Scenario: Drag from tab label reorders

- **WHEN** a member who can edit a custom tab starts a drag on that tab's label text (not the more control) and drops within the same visibility group
- **THEN** the tab order updates and is persisted via the existing sort API

#### Scenario: Press on more does not drag

- **WHEN** the member presses on the more menu trigger of a custom tab and moves the pointer
- **THEN** a tab drag is not started
