## Context

Spring 线的缺陷实体用 Java 枚举表示类型和状态：

| 字段 | Spring 线 JSON 输出 | 说明 |
|---|---|---|
| `defectType` | `"BUG"`（`@JsonFormat(STRING)`） | 枚举名 |
| `defectTypeName` | `"BUG"` | 同一列再映射一次，专供展示 |
| `defectState` | `0`～`4`（`@JsonFormat(NUMBER)`） | 枚举序号 |
| `defectStateName` | `"PROCESSING"` | 枚举名，前端按它判断状态 |

数据库里两列都存序号（`SysDefectTypeEnumTypeHandler` / `SysDefectStateEnumTypeHandler` 读写 `ordinal()`）。Jackson 反序列化枚举时名字和序号都接受。

Quarkus 线把 `SysDefect.defectType` / `defectState` 改成了 `Integer`，没有名字字段，也没有对输入做兼容。前端（Vue 3 版仍按 Spring 约定写）因此：

- 列表 / 详情 / 日历 / 分享卡片读 `defectStateName`、`defectTypeName` 得到 `undefined`，状态显示「未知」，类型图标空白；
- 操作按钮（修复、驳回、通过、关闭、重新打开）按 `defectStateName` 判断是否显示，全部不显示；
- 新建 / 编辑缺陷提交 `defectType: "BUG"`，后端 `Integer` 反序列化失败，请求被拒；
- 列表类型筛选发 `defectType=BUG`，状态多选发 `params[defectStates]=0,1`，前者参数转换失败，后者后端没读。

`DefectConstants.DEFECT_TYPES` / `DEFECT_STATES` 已按 Spring 枚举顺序定义了名字数组，MCP 的 `McpJson` 也已兼容名字与数字两种写法。

## Goals / Non-Goals

**Goals:**

- 缺陷 JSON 与 Spring 线一致：输出 `defectTypeName`、`defectStateName`，`defectType` 输出名字，`defectState` 输出数字。
- 输入兼容：请求体与查询参数里的类型、状态都接受名字（不区分大小写）或数字。
- 列表按类型筛选、按状态多选可用。

**Non-Goals:**

- 不改数据库存储（仍存序号），不加 Flyway 脚本。
- 不恢复 Java 枚举类型：内部代码大量按 `Integer` 使用（状态流转校验、统计、Excel、MCP），改动面过大且没有收益。
- 不补缺陷列表其它查询条件（回收站、收藏、关键字、创建人 / 处理人多选、页签、日历、自定义字段等），另开变更。
- 不改前端。

## Decisions

### D1：在 `SysDefect` 上用 Jackson 注解做名字 ↔ 序号转换，Java 字段保持 `Integer`

- `defectType` 字段加 `@JsonSerialize(using = DefectEnumJson.TypeSerializer.class)`、`@JsonDeserialize(using = DefectEnumJson.TypeDeserializer.class)`：输出名字，输入接受名字或数字。
- `defectState` 只加反序列化器（输出保持数字，与 Spring 一致）。
- 新增只读 getter `getDefectTypeName()`、`getDefectStateName()`，由 `DefectConstants` 把序号转名字；类上 `@JsonIgnoreProperties(value = {"defectTypeName", "defectStateName"}, allowGetters = true)`，前端把整条缺陷回传时这两个字段被忽略。
- 非法名字或越界数字抛 `InvalidFormatException`，由现有异常映射返回 400。

备选：恢复 Java 枚举（与 Spring 完全一致）。放弃原因见 Non-Goals。备选：只在 Resource 层手工转换。放弃：缺陷 JSON 出口很多（列表、详情、分享、计划内缺陷、报告数据、回收站），漏一个就再出一次「未知」。

MyBatis 只走 Java getter / setter，不受 Jackson 注解影响；Excel 导入导出用 `defectTypeImportName` / `defectStateImportName`，也不受影响。

### D2：查询参数改为字符串并统一解析

`DefectResource.list`、`DefectResource` 导出表单、`PlanResource.listDefectOfPlan` 的 `defectType` / `defectState` 参数类型改为 `String`，用 `DefectConstants.parseType` / `parseState`（名字或数字 → 序号，空串 → `null`，非法 → 400）解析。

### D3：状态多选

`DefectResource.list` 读取 `params[defectStates]`（逗号分隔，元素可为名字或数字），解析为序号列表放进 `params.defectStates`；Mapper 用 `IN` 过滤。只有一项时与 `defectState` 等价。

空列表在 Java 侧就不放进 `params`，XML 只判 `!= null`，不在 OGNL 里调 `size()`。

### D4：Native 下注册 OGNL 会反射调用的 JDK 集合类

浏览器验证时发现：Native 镜像里 OGNL 调 `list.size()` 抛 `NoSuchMethodException: java.util.ArrayList.size()`（JVM 正常）。`SysCaseMapper`、`SysPlanMapper`、`SysPlanItemMapper` 早有同样写法，属既有隐患。新增 `nativeimage/MyBatisOgnlReflectionConfig`，用 `@RegisterForReflection` 注册常见 `List` / `Set` / `Map` 实现（含 `Arrays$ArrayList`、`List.of` 的内部类）。

## Risks / Trade-offs

- [`defectType` 输出由数字变名字，依赖数字的调用方会受影响] → 后端内部只有 MCP `McpJson` 读 JSON，已兼容；前端按 Spring 约定本就期待名字；Open API 用独立的 `ApiDefect`，不受影响。
- [Jackson 注解在 Native 下需要反射] → Quarkus 会自动注册 `@JsonSerialize` / `@JsonDeserialize` 引用的类，无需手工注册；已在 Native 下验证列表与新建缺陷。

## Migration Plan

无数据迁移。回滚即回退代码。

## Open Questions

（无）

## 相关长期文档

- `docs/01-产品需求/术语表.md`（缺陷类型 / 状态枚举）
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`
- `docs/05-接口文档/接口约定.md`
- `docs/06-踩坑记录/缺陷更新接口缺字段会重置状态并清空自定义字段.md`
- `docs/06-踩坑记录/实体枚举改成Integer后接口JSON契约悄悄变了.md`
- `docs/06-踩坑记录/Native下MyBatis-OGNL调用集合size报NoSuchMethod.md`
