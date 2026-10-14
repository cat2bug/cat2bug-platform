## Why

缺陷列表的状态显示「未知」、类型图标空白：前端按 Spring 线的约定读 `defectStateName` / `defectTypeName`（枚举名，如 `PROCESSING`、`BUG`），Quarkus 后端的 `SysDefect` 把类型和状态都存成整数，接口里根本没有这两个字段。

排查中发现同一个根因还造成更严重的问题：前端新建 / 编辑缺陷时提交的 `defectType` 是 `"BUG"` 这样的名字，后端字段是 `Integer`，反序列化直接失败，**网页上新建缺陷会报错**；列表按类型筛选（`defectType=BUG`）和状态多选（`params[defectStates]`）也不生效。

## What Changes

- 缺陷 JSON 输出补回 `defectStateName`、`defectTypeName`（枚举名），与 Spring 线一致。
- `defectType` 输出改回枚举名（Spring 线为字符串），`defectState` 仍输出数字（Spring 线就是数字）。
- `defectType`、`defectState` 输入同时接受枚举名和数字，非法值返回明确错误。
- 缺陷列表（`/system/defect/list`）与计划内缺陷列表的 `defectType` / `defectState` 查询参数接受名字或数字；支持状态多选 `params[defectStates]`（逗号分隔的状态码）。
- 不改数据库（无 Flyway 脚本），库里仍存整数。

不在本次范围：缺陷列表与 Spring 线的其它查询条件差距（回收站、收藏、关键字、创建人 / 处理人多选、页签、日历、自定义字段筛选等），单独开变更。

## Capabilities

### New Capabilities
- `defect-enum-fields`: 缺陷类型 / 状态在接口上的表示（输出名字字段、输入兼容名字与数字、列表按类型和状态筛选）

### Modified Capabilities
（无）

## Impact

- 后端：`cat2bug-platform-common` 的 `SysDefect`、`DefectConstants`；`DefectResource.list`、`PlanResource` 缺陷列表参数；`SysDefectMapper.xml` 列表条件。
- 受益的前端页面（不改前端代码）：缺陷列表 / 详情 / 日历 / 分享卡片 / 操作按钮（按 `defectStateName` 判断能否修复、驳回等）、类型图标、报告模版 `api.defect.list` 表格里的类型和状态列。
- MCP：`McpJson` 已兼容数字和名字两种输入，行为不变。
- Open API（`ApiDefect`）自带枚举，不受影响。
