## ADDED Requirements

### Requirement: Delete and restore authorization for creator or remove permission

The system SHALL allow defect soft-delete and restore when the caller has permission `system:defect:remove`, OR when the caller's user id equals the defect's `create_by_id` and the caller lacks `system:defect:remove`. The system MUST reject delete and restore when the caller lacks `system:defect:remove` and is not the defect creator. Holders of `system:defect:remove` MUST be able to delete and restore any defect in scope regardless of creator.

#### Scenario: Creator deletes own defect without remove permission

- **WHEN** a user without `system:defect:remove` calls delete API for a defect they created (`create_by_id` equals current user id) with `del_flag = '0'`
- **THEN** the defect is soft-deleted with `del_flag = '2'` and a `DELETE` log entry is written

#### Scenario: Creator restores own deleted defect without remove permission

- **WHEN** a user without `system:defect:remove` calls restore API for a defect they created with `del_flag = '2'`
- **THEN** `del_flag` becomes `'0'` and a `RESTORE` log entry is written

#### Scenario: Non-creator without remove permission is rejected

- **WHEN** a user without `system:defect:remove` calls delete or restore API for a defect created by another user
- **THEN** the request fails with a permission error and the defect state is unchanged

#### Scenario: User with remove permission deletes any defect

- **WHEN** a user with `system:defect:remove` calls delete API for any defect in scope with `del_flag = '0'`
- **THEN** the defect is soft-deleted regardless of `create_by_id`

#### Scenario: Batch delete rejects if any defect is unauthorized

- **WHEN** a user without `system:defect:remove` calls batch delete with multiple defect ids including at least one not created by the caller
- **THEN** the request fails with a permission error and no defect in the batch is soft-deleted

### Requirement: Excel bulk delete aligns with creator or remove permission

The defect Excel view SHALL apply the same delete authorization as defect detail/list tools: allow bulk delete when the user has `system:defect:remove`, or when every selected persisted row has `create_by_id` equal to the current user id.

#### Scenario: Excel bulk delete by creator only own rows

- **WHEN** a user without `system:defect:remove` selects only defects they created in Excel view and confirms bulk delete
- **THEN** those defects are soft-deleted via the delete API

#### Scenario: Excel bulk delete blocked for mixed selection

- **WHEN** a user without `system:defect:remove` selects rows including at least one defect not created by them
- **THEN** the client shows a permission error and does not call the delete API
