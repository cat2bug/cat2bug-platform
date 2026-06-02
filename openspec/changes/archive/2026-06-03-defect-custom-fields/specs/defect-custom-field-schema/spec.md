## ADDED Requirements

### Requirement: 项目缺陷字段定义表

系统 SHALL 提供表 `sys_project_defect_field`，按 `project_id` 存储字段定义，且每条记录 MUST 包含：`field_key`、`field_label`、`field_type`、`max_length`、`required`、`nullable`、`enabled`、`sort_order`、`type_config`。

`field_key` 在同一 `project_id` 下 MUST 唯一，且创建后 MUST NOT 被更新。

单项目启用中的字段定义数量 MUST NOT 超过 50（`enabled=1`）。

#### Scenario: 创建字段定义

- **WHEN** 项目管理员提交合法 `field_key` 与 `field_type`
- **THEN** 系统持久化新记录并返回 `field_id`
- **THEN** 同项目下不得存在重复 `field_key`

#### Scenario: 禁止修改 field_key

- **WHEN** 客户端尝试更新已有记录的 `field_key`
- **THEN** 请求被拒绝并返回明确错误信息

### Requirement: 支持的字段类型

系统 MUST 支持以下 `field_type`：`string`、`number`、`boolean`、`datetime`、`enum`、`object`、`image`、`file`、`array`。

`enum` 类型的 `type_config` MUST 包含 `options` 数组，每项含 `key`、`label`、`color`（color 为可选 CSS 色值）。

`array` 类型的 `type_config` MUST 包含 `itemType`，且 `itemType` MUST 为上述类型之一（含嵌套 `array` 时 MVP 仅允许一层嵌套，即 `itemType` 不得为 `array`）。

#### Scenario: 枚举配置校验

- **WHEN** 创建 `enum` 字段且 `options` 为空或存在重复 `key`
- **THEN** 保存失败并提示校验错误

### Requirement: 字段定义 CRUD API

系统 SHALL 提供按项目维度的 REST API：列表、仅启用列表、创建、更新、软删除（`enabled=0`）。

仅具有项目设置权限的用户 MUST 可调用管理类接口。

#### Scenario: 获取启用字段供表单使用

- **WHEN** 缺陷编辑页请求 `.../defect-fields/enabled`
- **THEN** 仅返回 `enabled=1` 的记录，按 `sort_order` 升序

#### Scenario: 软删除字段

- **WHEN** 管理员删除某字段定义
- **THEN** 该记录 `enabled` 置为 0
- **THEN** 历史缺陷 `custom_fields` 中对应键仍可存在但不参与展示与必填校验

### Requirement: 禁止在有数据时修改字段类型

当项目中存在至少一条缺陷且其 `custom_fields` 中某 `field_key` 值非空时，系统 MUST NOT 允许将该字段定义的 `field_type` 修改为其它类型。

#### Scenario: 有历史数据时改类型

- **WHEN** 字段 `env` 已在缺陷中使用且管理员尝试将类型从 `string` 改为 `number`
- **THEN** 更新被拒绝

### Requirement: 项目设置入口

缺陷项目设置页（`project/option`）MUST 提供「缺陷设置」卡片，含进入自定义字段管理页的链接。

管理页 MUST 支持字段定义的增删改查及本 spec 所列属性编辑。

#### Scenario: 从项目设置进入管理页

- **WHEN** 用户点击「自定义字段」链接
- **THEN** 打开项目级字段定义管理界面并加载当前项目定义列表
