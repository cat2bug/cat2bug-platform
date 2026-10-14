# 计划项缺陷 ID 的 CSV 存储

说明测试计划项 `sys_plan_item.defect_ids` 以逗号分隔字符串存储关联缺陷 ID 的读写方式与 SQL 匹配片段；凡是「按缺陷找计划项 / 统计计划缺陷」的查询都要复用这里的写法。

## 概述

- `defect_ids` 是 `varchar(10000)`，内容为逗号分隔的 Long 列表，历史上存在多种格式：`,12,34`（SQL 追加产生的前导逗号）、`12,34`（TypeHandler 写入）、`[5,6]` / `"[8]"`（早期 Quarkus 误写的 JSON 数组）。
- Java 侧统一由 `LongCsvTypeHandler` 映射为 `List<Long>`；SQL 侧用 `planItemDefectIdMatch` 片段做「某缺陷 ID 是否在列表中」的精确匹配。

## 关键流程

1. **读取**：resultMap 中 `defect_ids` 列指定 `typeHandler=LongCsvTypeHandler`；`parse` 去掉 `[`、`]`、`"`，按逗号拆分、忽略空段，因此三种历史格式都能读。
2. **整体写入**：`#{defectIds, typeHandler=LongCsvTypeHandler}`，输出 `1,2,3`（无首尾逗号）。
3. **追加 / 移除单个缺陷**（`SysPlanItemMapper.xml` 的 update 与 batch update）：
   - 追加：`params.defectId` → 先 `REPLACE` 去掉已有的 `,{id}` 再在末尾拼 `,{id}`；
   - 移除：`params.deleteDefectId` → `REPLACE(defect_ids, ',{id}', '')`。
4. **匹配片段** `planItemDefectIdMatch`（参数 `planItemAlias`、`defectIdExpr`）：把列首尾各补一个逗号，再 `like '%,{id},%'`，从而对 `12,34`、`,12,34` 两种格式都精确匹配，不会把 `112` 误当成 `12`。用于计划列表统计与仪表盘（JOIN `sys_defect`、按缺陷日志统计）。

## 关键代码位置

- `cat2bug-platform-system/src/main/java/com/cat2bug/system/mybatis/LongCsvTypeHandler.java`
- 测试：`cat2bug-platform-admin/src/test/java/com/cat2bug/system/mybatis/LongCsvTypeHandlerTest.java`
- `cat2bug-platform-admin/src/main/resources/mapper/SysPlanItemMapper.xml`（读写）
- `cat2bug-platform-admin/src/main/resources/mapper/SysPlanMapper.xml`、`SysDashboardMapper.xml`（`<sql id="planItemDefectIdMatch">` 定义与引用）

## 注意事项

- 匹配片段在 `SysPlanMapper.xml` 与 `SysDashboardMapper.xml` 中**各定义了一份**（MyBatis `<sql>` 不跨 namespace 共享），修改时两处同步。
- 原 Spring 线的同名片段用三个 `like` 组合（`%,id,%`、`%id,%`、`%,id%`），会把 `112` 误匹配成 `12`；当前实现已改为首尾补逗号的精确写法，不要照搬 tag `spring-final-1.1.0` 里的旧写法。
- 同文件中的 `defectLogReceiveByMatchUser` 是同类思路：H2 下 `receive_by` 为 JSON，不能直接与 BIGINT 比较，需 `CAST` 成字符串后匹配。
- 片段里 `CAST(... AS CHAR(3000))` 的长度小于列定义 10000，关联缺陷极多时的行为见待确认。
- 新代码不要再写 JSON 数组格式，整体写入统一走 TypeHandler。

## 相关规格

- 无专门 OpenSpec 规格；计划/仪表盘统计口径若沉淀为规格，在此补链接。

## 待确认

- `REPLACE(defect_ids, ',{id}', '')` 是子串替换：移除 `12` 时会把 `,123` 变成 `3`，追加时同理；是否已有数据被污染、是否需改为首尾补逗号后再替换。
- `SysPlanItemMapper.xml` 查询条件 `pi.defect_ids in (#{defectId}...)` 对 CSV 字段做 `IN`，只有字段恰好等于单个 ID 时才会命中，是否符合预期。
- `defect_ids` 超过 3000 字符时 `CAST(... AS CHAR(3000))` 在 H2 / MySQL 下是截断还是报错。
