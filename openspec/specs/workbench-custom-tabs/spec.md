# workbench-custom-tabs Specification

## Requirements

### Requirement: Fixed work tab plus custom tabs and add control

The project workbench shell SHALL present a fixed tab **工作台** (id `work`), zero or more custom tabs, and a trailing add control modeled after the defect-list add tab (placeholder pane that MUST NOT become the selected tab). Display order MUST be: fixed work → project-shared custom tabs → private custom tabs → add control.

#### Scenario: Open workbench

- **WHEN** a project member opens the project landing or dashboard module
- **THEN** the fixed **工作台** tab is present
- **THEN** the add control is present at the end of the tab bar
- **THEN** no **看板** tab is presented

#### Scenario: Order shared before private

- **WHEN** the member has at least one project-shared custom tab and one private custom tab
- **THEN** all shared custom tabs appear after **工作台** and before any private custom tabs

### Requirement: Drag reorder within visibility group

The system SHALL allow dragging custom tabs to change order **within the same visibility group** (project-shared among shared; private among the member's private). The fixed **工作台** tab and the add control MUST NOT be draggable. Dragging a tab across shared/private groups MUST be rejected. Reorder of shared tabs MUST require `project.create-by` or `project.admin`; reorder of private tabs MUST require ownership. Persisted order MUST use `tab_sort` (higher first within the group) via `PUT /system/workbench/{projectId}/tabs/sort`.

#### Scenario: Reorder private tabs

- **WHEN** a member drags one of their private tabs past another private tab and drops
- **THEN** the private group order updates and is restored after reload
- **THEN** shared tabs remain before all private tabs

#### Scenario: Reject cross-group drag

- **WHEN** a member attempts to drop a private tab among shared tabs (or the reverse)
- **THEN** the move is not applied

#### Scenario: Non-admin cannot reorder shared

- **WHEN** a non-admin member attempts to drag a project-shared tab
- **THEN** the tab is not draggable
- **THEN** shared order is unchanged

### Requirement: Create custom tab via add dialog

The system SHALL allow creating a custom tab through a dialog opened from the add control. The dialog MUST collect: tab name (required, max **12** characters), preset (`manager` | `tester` | `developer` | `document`), and visibility (`private` | `project`). Visibility controls MUST be shown only to members with project role `project.create-by` or `project.admin`; other members MUST create private tabs only (visibility field hidden; server MUST force `private`). On successful create, the client MUST select the new tab, persist that selection locally, and enter layout edit mode for that tab.

#### Scenario: Admin creates shared tab

- **WHEN** a project admin opens the add dialog, enters a name ≤12 chars, chooses preset `document`, chooses visibility `project`, and confirms
- **THEN** a project-shared tab is created with layout seeded from the document preset
- **THEN** the new tab becomes active and layout edit mode is on

#### Scenario: Non-admin cannot choose shared

- **WHEN** a member without create-by/admin roles opens the add dialog
- **THEN** visibility controls are not shown
- **THEN** the created tab is private to that member

#### Scenario: Reject oversized name

- **WHEN** a member attempts to submit a tab name longer than 12 characters
- **THEN** the client or server rejects the create
- **THEN** no tab row is persisted

### Requirement: Shared tab limit of twenty-four

The system SHALL allow at most **24** project-shared custom tabs per project. Private tabs MUST NOT be subject to this limit. Creating a shared tab that would exceed 24 MUST fail with a clear error; the client SHOULD disable or block choosing `project` visibility when the limit is already reached.

#### Scenario: Twenty-fifth shared rejected

- **WHEN** a project already has 24 shared custom tabs and an admin attempts to create another with visibility `project`
- **THEN** the server rejects the create
- **THEN** the shared count remains 24

#### Scenario: Private unlimited relative to shared cap

- **WHEN** a project already has 24 shared tabs and a member creates a private tab
- **THEN** the private tab is created successfully

### Requirement: Visibility immutable after create

After a custom tab is created, the system MUST NOT allow changing its visibility between `private` and `project`.

#### Scenario: No visibility edit

- **WHEN** a member opens rename or edit flows for an existing custom tab
- **THEN** no control is offered to change visibility
- **THEN** updating the tab does not alter its stored visibility

### Requirement: Permissions for edit rename and delete

The **工作台** tab remains editable as the member's personal layout and MAY be removed from the tab bar when more than one visible tab exists (removal hides it locally; personal layout data is retained). Custom tab rename/delete follow ownership: private → owning user; project-shared → `project.create-by` / `project.admin` only; other members MUST view shared layout read-only and the client MUST **hide** edit/rename/delete. When only **one** visible tab remains (工作台 and/or custom), delete controls MUST NOT be offered for that tab.

#### Scenario: Owner renames private tab

- **WHEN** the owning user renames their private tab to a valid name ≤12 chars
- **THEN** the new name is persisted and shown on the tab bar

#### Scenario: Admin deletes shared tab

- **WHEN** a project admin confirms deletion of a project-shared tab and at least one other tab remains visible
- **THEN** the tab is removed for all project members
- **THEN** members whose local selection pointed at that tab fall back to another visible tab (工作台 if shown, else another custom)

#### Scenario: Non-admin on shared tab

- **WHEN** a non-admin member selects a project-shared tab
- **THEN** edit, rename, and delete controls are not rendered
- **THEN** the shared layout is still displayed

#### Scenario: Last visible tab cannot be deleted

- **WHEN** only one tab is visible on the bar
- **THEN** no delete control is offered for that tab

#### Scenario: Hide work tab when others exist

- **WHEN** a member has at least one custom tab and confirms deleting **工作台**
- **THEN** **工作台** is hidden from the tab bar for that member
- **THEN** personal layout data remains and can be shown again if no other tabs remain

### Requirement: Local persistence of selected tab

The client SHALL persist the selected workbench tab id per project in local storage (key pattern `workbench-tab:{projectId}` or equivalent). On next visit, the client MUST restore that selection when the tab still exists and is visible to the user; otherwise it MUST select **工作台**. Restoring selection MUST NOT automatically re-enter edit mode.

#### Scenario: Restore after reload

- **WHEN** a member selects a private tab, then reloads the workbench
- **THEN** that private tab is selected again
- **THEN** the shell is not in edit mode solely due to restore

#### Scenario: Fallback when tab gone

- **WHEN** the locally cached tab id refers to a deleted or no-longer-visible tab
- **THEN** the client selects **工作台**
- **THEN** the cache is updated accordingly

### Requirement: Persist custom tab layouts separately from personal workbench

Custom tab layouts MUST be persisted independently from the per-user personal **工作台** config (`sys_user_workbench`). Each custom tab MUST store its own v3 row layout payload. Listing custom tabs for a member MUST return all project-shared tabs for the project plus that member's private tabs.

#### Scenario: Save shared layout visible to peer

- **WHEN** an admin saves layout changes on a shared tab
- **THEN** another project member opening that tab sees the updated rows

#### Scenario: Private layout not visible to peer

- **WHEN** a member saves layout on their private tab
- **THEN** another member does not see that private tab in their tab bar
