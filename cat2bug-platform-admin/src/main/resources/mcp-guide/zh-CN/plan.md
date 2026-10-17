## 测试计划是什么

测试计划是一轮测试：选定一批用例，每个用例成为计划里的一个**子项**。子项执行状态：

- `unexecuted`：未执行
- `pass`：通过
- `not_pass`：不通过（应关联一个缺陷）

计划列表会给出通过 / 不通过 / 未执行数量与缺陷数。

## 常用操作

| 目的 | 工具 |
|---|---|
| 查计划 | `list_plans` |
| 看子项 | `list_plan_items`（可按 `state` 过滤，如只看 `unexecuted`） |
| 记录结果 | `update_plan_item`（`state`，不通过时传 `defect_id` 关联缺陷） |
| 新建 / 修改计划 | 专用工具未提供，用 `list_operations(module=plan)` 找到对应接口再 `call_operation` |

执行子项需要「执行测试计划」权限（项目测试默认有；项目管理员默认只有编辑权限，也可以记录结果）。

## 执行测试计划的步骤

1. `list_plans` 找到计划，告诉用户进度（总数、通过、不通过、未执行）。
2. `list_plan_items(state=unexecuted)` 取未执行子项，逐条 `get_case` 读步骤，按步骤执行或请用户执行并反馈结果。
3. 通过：`update_plan_item(state=pass)`。
4. 不通过：先 `create_defect`（标题写清现象，描述里写用例编号、步骤、实际结果，交付物与用例一致），再 `update_plan_item(state=not_pass, defect_id=新缺陷 ID)`。
5. 每条完成后汇报「子项 X：通过 / 不通过（已提缺陷 #N 给 Y）」，并说剩余多少条。
6. 全部执行完后汇总：通过率、提了哪些缺陷，下一步建议「等开发修复后验证缺陷」或「继续执行其它测试计划」。
