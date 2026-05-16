## ADDED Requirements

### Requirement: Built-in deleted defects tab

The defect list page SHALL display a built-in tab labeled via i18n key `defect.deleted-defect` immediately to the right of the built-in `defect.all-defect` tab. This tab MUST NOT be closable. Activating it MUST query defects with `params.delFlag = '2'`.

#### Scenario: User opens deleted tab

- **WHEN** user selects the built-in deleted defects tab
- **THEN** the list shows only soft-deleted defects for the current project

#### Scenario: All defects tab excludes deleted

- **WHEN** user selects the built-in all defects tab
- **THEN** the list shows only defects with `del_flag = '0'`

### Requirement: Tab icons differentiate tab kinds

The defect list tabs SHALL show icons: a dedicated icon for all defects, a dedicated icon for deleted defects, and a single shared icon for all user-defined custom tabs.

#### Scenario: Custom tab icon

- **WHEN** user views a custom defect tab in the tab bar
- **THEN** the tab label includes the shared custom-tab icon

### Requirement: Custom tab creation includes deleted switch

The create defect tab dialog SHALL include an `el-switch` at the bottom of the form (above action buttons) bound to `form.config.params.delFlag` with `active-value="2"` and `inactive-value="0"`, defaulting to `0` when the dialog opens.

#### Scenario: New custom tab defaults to not deleted filter

- **WHEN** user opens create defect tab dialog
- **THEN** the deleted switch is off and saved config uses `params.delFlag = '0'` unless user enables it

#### Scenario: Custom tab with deleted switch on

- **WHEN** user creates a tab with deleted switch on
- **THEN** activating that tab queries with `params.delFlag = '2'` combined with other saved filters

### Requirement: DefectTools shows restore and view for deleted rows in table

When `defect.delFlag` is `'2'`, the `DefectTools` component in `list/table.vue` SHALL hide all action buttons except **restore** and **view** (including share, collect, assign, repair, reject, pass, open, close, edit, delete). Restore SHALL use the restore API and emit refresh events on success. View SHALL open the defect detail/read flow (same as active rows, e.g. `@view` / `handleClickTableRow`) without enabling edit or workflow actions on deleted records.

#### Scenario: Table row tools for deleted defect

- **WHEN** defect list table renders a row with `delFlag = '2'`
- **THEN** only the restore and view buttons are visible in `DefectTools`

#### Scenario: User views deleted defect from table

- **WHEN** user clicks view on a deleted defect row in `table.vue`
- **THEN** the system opens defect detail in read-only mode (no edit/workflow mutations)

#### Scenario: Table row tools for active defect

- **WHEN** defect list table renders a row with `delFlag = '0'`
- **THEN** restore is hidden and existing tools follow prior visibility rules

### Requirement: Excel rows for deleted defects are not editable

In defect Excel view, any row representing a defect with `delFlag = '2'` MUST NOT accept cell edits or attachment/image removals. Save handlers MUST ignore change buffers for such rows.

#### Scenario: User attempts to edit deleted row in Excel

- **WHEN** user changes a cell on a row with `delFlag = '2'`
- **THEN** no update API is called and the change is not persisted

### Requirement: i18n for new UI strings

The system SHALL add i18n entries for new user-visible strings (including `defect.deleted-defect`, `defect.show-deleted`, `defect.restore`) in all supported locale files under `cat2bug-platform-ui/src/utils/i18n/`.

#### Scenario: Chinese UI label

- **WHEN** UI locale is zh-CN
- **THEN** built-in deleted tab displays「已删除缺陷」from i18n
