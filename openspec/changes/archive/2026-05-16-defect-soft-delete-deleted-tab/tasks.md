## 1. Database migration

- [x] 1.1 Add Flyway migration for H2: `sys_defect.del_flag` char(1) NOT NULL DEFAULT `'0'` with comment
- [x] 1.2 Add Flyway migration for MySQL: same `del_flag` column
- [x] 1.3 Update `h2-schema.sql` and `sql/cat2bug_platform.sql` baseline for new installs

## 2. Backend domain and logging

- [x] 2.1 Add `delFlag` field to `SysDefect` entity and `SysDefectMapper` resultMap
- [x] 2.2 Append `DELETE` and `RESTORE` to end of `SysDefectLogStateEnum`
- [x] 2.3 Change `deleteSysDefectByDefectId(s)` to soft update + write `DELETE` log in `SysDefectServiceImpl`
- [x] 2.4 Implement `restoreSysDefect` service method + `PUT/POST .../restore` controller endpoint with `RESTORE` log
- [x] 2.5 Add `params.delFlag` filtering to `selectSysDefectList` (default `'0'`, `'2'` for deleted-only)
- [x] 2.6 Block edit/workflow endpoints when target defect `del_flag = '2'`
- [x] 2.7 Apply `del_flag = '0'` filter to `SysDefectStatisticMapper`, `getProjectDefectMaxNum`, `selectVersionList`, `SysPlanMapper` defect list, `ApiDefectMapper` (grep `sys_defect`)

## 3. Frontend API and i18n

- [x] 3.1 Add `restoreDefect(defectId)` to `api/system/defect.js`
- [x] 3.2 Add i18n keys: `defect.deleted-defect`, `defect.show-deleted`, `defect.restore` (zh-CN, zh-TW, en-US, ja-JP, ko-KR, ru, ar)

## 4. Defect list tabs (index.vue)

- [x] 4.1 Add `DELETED_TAB_NAME` constant and built-in tab after「全部缺陷」with icon, no close button
- [x] 4.2 Update `selectDefectTabHandle`: `all-tab` → `params.delFlag='0'`; `deleted-tab` → `'2'`; custom tabs use saved `params.delFlag`
- [x] 4.3 Add icons: all-tab, deleted-tab, custom tabs (SVG assets + template slots)
- [x] 4.4 Hide or disable「新建缺陷」/ bulk delete on deleted tab when appropriate

## 5. Create tab dialog

- [x] 5.1 Add bottom `el-switch` for `form.config.params.delFlag` (active `2`, inactive `0`) in `DefectTabDialog.vue`
- [x] 5.2 Default `params.delFlag = '0'` in `reset()`

## 6. DefectTools and table

- [x] 6.1 Add `isDeleted`, `restoreVisible`, `handleRestore` in `DefectTools/index.vue`; when deleted keep **restore + view** only, hide other actions
- [x] 6.2 Wire `@restore` and `@view` in `list/table.vue`; deleted row view opens read-only detail

## 7. Excel view

- [x] 7.1 Map `delFlag` from API into `sheetRows`
- [x] 7.2 Guard `handleSheetUpdate`, plan time save, img/annex remove when `row.delFlag === '2'`
- [x] 7.3 Apply readonly styling to deleted rows after `refreshExcelEditorView`

## 8. Documentation and verification

- [x] 8.1 Update user guide: `defect-tabs.md`, `defect-delete.md` (soft delete + restore + deleted tab)
- [x] 8.2 Manual test: delete → deleted tab → view (read-only) + restore → all tab; custom tab switch; Excel readonly row
