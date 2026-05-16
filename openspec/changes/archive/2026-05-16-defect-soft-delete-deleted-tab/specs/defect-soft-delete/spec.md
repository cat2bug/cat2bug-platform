## ADDED Requirements

### Requirement: Defect records use del_flag for soft delete

The system SHALL store deletion state on `sys_defect` using column `del_flag` char(1) NOT NULL DEFAULT `'0'`, where `'0'` means present and `'2'` means soft-deleted. The system MUST NOT use value `'1'` for `del_flag`.

#### Scenario: New defect defaults to not deleted

- **WHEN** a defect is inserted without specifying `del_flag`
- **THEN** the stored `del_flag` is `'0'`

#### Scenario: Legacy rows after migration

- **WHEN** database migration adds `del_flag` to existing `sys_defect` rows
- **THEN** all existing rows have `del_flag = '0'`

### Requirement: Delete defect performs soft delete with log

The system SHALL implement defect deletion as an update setting `del_flag = '2'` instead of removing the row. The system MUST insert a `sys_defect_log` entry with type `DELETE` for each soft-deleted defect. The system MUST NOT add `delete_time`, `delete_by`, or `delete_by_id` columns to `sys_defect`.

#### Scenario: User deletes one defect

- **WHEN** an authorized client calls delete API for a defect with `del_flag = '0'`
- **THEN** the defect row remains in `sys_defect` with `del_flag = '2'` and a log entry with type `DELETE` exists

#### Scenario: Delete API path unchanged

- **WHEN** client uses existing `DELETE /system/defect/{defectIds}` endpoint
- **THEN** behavior is soft delete, not physical delete

### Requirement: Restore defect clears soft delete with log

The system SHALL provide an API to restore a soft-deleted defect by setting `del_flag = '0'`. The system MUST insert a `sys_defect_log` entry with type `RESTORE`.

#### Scenario: User restores deleted defect

- **WHEN** an authorized client restores a defect with `del_flag = '2'`
- **THEN** `del_flag` becomes `'0'` and a log entry with type `RESTORE` exists

### Requirement: Defect log enum includes DELETE and RESTORE

The system SHALL append `DELETE` and `RESTORE` to the end of `SysDefectLogStateEnum` (after `IMPORT`) so persisted `defect_log_type` ordinals remain stable for existing values.

#### Scenario: Delete log type ordinal

- **WHEN** a soft-delete log is written
- **THEN** `defect_log_type` corresponds to the `DELETE` enum ordinal immediately after `IMPORT`

### Requirement: Defect list filters by del_flag

The system SHALL filter defect list queries by `del_flag`. When query parameter `params.delFlag` is absent or `'0'`, the system MUST return only rows with `del_flag = '0'`. When `params.delFlag` is `'2'`, the system MUST return only rows with `del_flag = '2'`.

#### Scenario: Default list excludes deleted

- **WHEN** client requests defect list without `params.delFlag` or with `params.delFlag = '0'`
- **THEN** no row with `del_flag = '2'` is returned

#### Scenario: Deleted-only list

- **WHEN** client requests defect list with `params.delFlag = '2'`
- **THEN** only rows with `del_flag = '2'` are returned

### Requirement: Mutations blocked on soft-deleted defects

The system SHALL reject edit and workflow mutations (update, assign, repair, etc.) when target defect has `del_flag = '2'`. The system MAY allow read-by-id for soft-deleted defects for audit and recycle-bin views.

#### Scenario: Edit rejected for deleted defect

- **WHEN** client attempts to update a defect with `del_flag = '2'`
- **THEN** the request fails with an appropriate error and data is unchanged

### Requirement: Statistics and ancillary queries exclude deleted by default

The system SHALL apply `del_flag = '0'` to defect counts and list queries used in statistics, plan defect lists, version lists, and Open API defect lists unless explicitly querying deleted-only context.

#### Scenario: Dashboard defect count

- **WHEN** statistics aggregate defects for a project
- **THEN** soft-deleted defects are not included in counts
