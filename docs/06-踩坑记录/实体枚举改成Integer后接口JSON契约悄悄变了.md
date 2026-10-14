# 实体枚举改成 Integer 后接口 JSON 契约悄悄变了

移植实体时把 Java 枚举换成 `Integer`，编译和后端测试都能过，但前端按旧契约收发的字段已经对不上。

- 日期：2026-10-15
- 涉及：后台 / 前端

## 现象

- 缺陷列表状态显示「未知」，类型图标空白，详情里修复 / 驳回 / 通过等按钮全部不出现。
- 网页上新建缺陷失败：请求体 `defectType: "BUG"`，后端返回字段反序列化错误。
- 列表按类型筛选（`defectType=BUG`）报错，状态多选（`params[defectStates]`）不生效。

## 根因

Spring 线 `SysDefect.defectType` / `defectState` 是枚举，Jackson 自动做名字 ↔ 序号转换，另有同列映射出的 `defectTypeName` / `defectStateName` 给前端展示。Quarkus 线把它们改成 `Integer`，没有补名字字段，也没处理输入：

- 输出少了 `defectTypeName`、`defectStateName`，`defectType` 从名字变成数字；
- 输入的名字无法转成 `Integer`；
- 后端测试全部用数字收发，前端也没有接口契约测试，所以一直没暴露。

## 解决办法

变更 `fix-defect-enum-names`：`SysDefect` 字段仍是 `Integer`（MyBatis、流转校验、统计都按序号用），用 Jackson 注解在接口边界转换（`DefectEnumJson`）：类型输出名字、输入名字或数字都收；新增只读 `defectTypeName` / `defectStateName`；查询参数改为字符串并用 `DefectConstants.parseType` / `parseState` 解析。

## 以后怎么避免

- 移植或重构实体时，若改变了字段的 Java 类型，先对照旧版 JSON 输出（字段名、类型、派生字段）再改；可用旧 tag（如 `spring-final-1.1.0`）的实体与 TypeHandler 对照。
- 接口测试要按前端真实请求的写法发参数（名字而不是数字），并断言前端读的派生字段。
