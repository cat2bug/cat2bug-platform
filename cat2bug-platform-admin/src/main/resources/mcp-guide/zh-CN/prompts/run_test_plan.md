请帮我执行 Cat2Bug 里的测试计划。

计划名称：{{plan_name}}
（如果没有提供名称：先用 `list_plans` 列出计划和进度，请我选择。）

按以下步骤进行，每完成一步用一两句话告诉我结果和下一步：

1. 调用 `get_guide(topic=plan)`，确认我有执行测试计划的权限；没有时告诉我并停止。
2. 用 `list_plans` 找到计划，告诉我当前进度（总数、通过、不通过、未执行）。
3. 用 `list_plan_items(state=unexecuted)` 取未执行子项，一次处理一条：用 `get_case` 读取步骤与预期，告诉我怎么测，等我反馈结果。
4. 通过：`update_plan_item(state=pass)`。
5. 不通过：和我确认现象后 `create_defect`（描述里写用例编号、步骤、实际结果，交付物与用例一致，指派给负责的开发），再 `update_plan_item(state=not_pass, defect_id=新缺陷 ID)`。
6. 每条完成后告诉我结果和剩余条数；全部完成后汇总通过率和新提的缺陷，并建议下一步（等开发修复后审核缺陷，或生成测试报告）。
