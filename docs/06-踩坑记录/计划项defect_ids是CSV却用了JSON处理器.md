# 计划项 defect_ids 是 CSV 却用了 JSON 处理器

记录测试计划关联缺陷列表 500、只显示一条、ID 误命中三个连环问题。

- 日期：2026-10-14
- 涉及：Quarkus 后端 / 数据库

## 现象

1. 关联过缺陷的测试计划，打开「计划缺陷」列表直接 500。
2. 修好 500 后，一个计划项关联多个缺陷时只显示最后一个。
3. 统计里缺陷 ID 有前缀误命中，例如查缺陷 1 会把 11、12 也算进来。

## 根因

- 计划项 `defect_ids` 在 Spring 里一直存逗号分隔字符串（如 `1,2` / `,12,34`），Quarkus 移植时误配了 JSON TypeHandler，读到 CSV 解析失败 → 500；同时新写入的是 JSON，造成两种格式混存。
- 计划缺陷列表的 JOIN / LIKE 条件只匹配到末尾缺陷。
- `like concat('%', id, '%')` 没有逗号边界，ID 是字符串前缀即命中（Spring 侧同样存在）。

## 解决办法

提交 `59f6a23e`：

- 新增 `LongCsvTypeHandler`（`cat2bug-platform-quarkus-system/.../mybatis/LongCsvTypeHandler.java`），读写 CSV，并兼容已写入的 JSON 值；`SysPlanMapper.xml` 改用它。
- 匹配条件统一为两边加逗号：`concat(',', defect_ids, ',') like concat('%,', id, ',%')`（见 `SysPlanMapper.xml`、`SysDashboardMapper.xml`）。
- 补 `LongCsvTypeHandlerTest`。

## 以后怎么避免

- 移植 Mapper 时先查 Spring 侧真实存储格式，不要凭字段名猜 JSON。
- 任何「CSV 字段里找 ID」的 LIKE，必须加逗号边界。
- Spring 线如继续维护，同样的 LIKE 前缀问题待同步修复（见 `openspec/changes/archive/2026-10-14-restore-quarkus-full-delivery/tasks.md` 7.12）。已不适用：Spring 线于 2026-10-14 删除；`LongCsvTypeHandler` 现位于 `cat2bug-platform-system/src/main/java/com/cat2bug/system/mybatis/`。
