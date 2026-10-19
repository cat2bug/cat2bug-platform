## 测试用例是什么

用例描述「怎么测、期望什么」，必须归属一个交付物。字段：

- 标题 `name`、交付物 `module_id`（必填，先用 `list_modules` 查）
- 前置条件 `preconditions`、测试数据 `case_data`
- 步骤 `steps`：每步 `describe`（操作）+ `expect`（该步期望）
- 整体预期 `expect`、等级 `level`（1～5，默认 2，数字越小越重要）
- 类型 `case_type_id` 可选，名称来自本项目配置（`list_case_types`）。空着也合法，类型不改变编辑表单
- 评审标记 `reviewStatus`：空为未评审，`in_review` 评审中，`passed` 通过，`changes` 需修改

用例本身没有执行状态；执行结果记在测试计划的子项上（见 `plan` 主题）。评审是可选的勾选批次，不是每条用例的审批流。未评审或需修改的用例仍可加入测试计划。

## 常用操作

| 目的 | 工具 |
|---|---|
| 查用例 | `search_cases`（按标题、交付物、编号、`case_type_id`、`review_status`） |
| 看详情 | `get_case` |
| 新建 | `create_case`（建议传 `idempotency_key`，重试不重复） |
| 修改 | `update_case`（传 `steps` 会整体替换原步骤；`clear_case_type=true` 清空类型） |
| 类型 | `list_case_types` / `create_case_type` / `update_case_type` / `delete_case_type` |
| 评审 | `start_case_review`、`comment_case_review`、`complete_case_review`、`list_case_reviews`、`get_case_review`、`update_case_review_report` |

## 编写用例的步骤

1. 确认交付物：`list_modules` 列出后让用户选；没有合适的交付物时告诉用户先建（见 `deliverable` 主题）。
2. 根据需求拟出用例清单（标题 + 步骤 + 期望），先给用户看，确认后再创建。
3. 逐条 `create_case`，完成后汇报「已在『交付物名』下新建 N 条用例：编号……」。
4. 下一步建议：把这些用例加入测试计划执行（需要网页或 `call_operation` 建计划，见 `plan` 主题）。

## 与缺陷的关系

执行用例发现问题时提缺陷，缺陷的交付物与用例保持一致；在测试计划里把子项标为不通过并关联该缺陷。
