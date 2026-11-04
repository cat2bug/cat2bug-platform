## ADDED Requirements

### Requirement: 批量菜单提供合并入口

缺陷表格「批量操作」下拉 MUST 提供「合并」菜单项。该菜单项 MUST 仅在勾选数量 ≥2、当前为表格视图、且用户具备 `system:defect:merge`、且选中每条均为未删除缺陷时可用。合并可用性 MUST NOT 受选中缺陷状态是否相同或流转资格限制（与指派/驳回等状态机批量规则正交）。选择「合并」MUST 打开合并抽屉（见 `defect-merge-duplicates`），MUST NOT 打开现有批量流转填写弹窗或删除确认框。Excel 视图 MUST NOT 显示批量「合并」（与整条批量工具条一并隐藏即可）。

#### Scenario: 勾选两条有权限时可合并

- **WHEN** 用户在表格视图勾选 2 条未删除缺陷且具备 `system:defect:merge`
- **THEN** 「合并」菜单项可用，点击后打开合并抽屉并载入这两条为右侧列表

#### Scenario: 仅勾选一条不可合并

- **WHEN** 用户只勾选 1 条缺陷
- **THEN** 「合并」菜单项不可用

#### Scenario: 混合状态仍可合并

- **WHEN** 用户勾选同时包含处理中与待审核的多条未删除缺陷，且具备合并权限
- **THEN** 「合并」可用（即使「驳回」「通过」因混合状态不可用）

#### Scenario: 合并不走批量流转弹窗

- **WHEN** 用户点击批量「合并」
- **THEN** 系统打开合并专用抽屉，不打开 `DefectBatchActionDialog` 类流转弹窗
