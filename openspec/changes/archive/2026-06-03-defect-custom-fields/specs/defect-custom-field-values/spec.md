## ADDED Requirements

### Requirement: 缺陷 custom_fields 存储

表 `sys_defect` MUST 新增列 `custom_fields`（JSON），用于存储键值对；键 MUST 为项目字段定义的 `field_key`。

缺陷创建、更新、查询接口 MUST 读写该字段（与 `SysDefect` 实体一并序列化）。

#### Scenario: 保存自定义字段值

- **WHEN** 用户创建缺陷并提交 `customFields: { "env": "prod" }`
- **THEN** 持久化至 `sys_defect.custom_fields`
- **THEN** 查询详情时返回相同结构

### Requirement: 服务端按定义校验

保存缺陷时，系统 MUST 根据该项目**已启用**字段定义校验 `custom_fields`：

- `required=1` 的字段 MUST 有非空值（`nullable` 不豁免必填）
- `string` MUST 满足 `max_length`（若配置）
- 各类型 MUST 符合 `design.md` 存储形态
- 未知或已停用字段键 MUST 被忽略或剥离（实现二选一并在代码注释固定；MVP 推荐：**剥离未定义键**）

#### Scenario: 必填字段缺失

- **WHEN** 字段 `severity` 标记必填且请求未包含该键
- **THEN** 保存失败并返回字段级错误信息

#### Scenario: 枚举值非法

- **WHEN** 提交枚举键 `severity: "INVALID"` 且不在 `type_config.options` 的 key 集合中
- **THEN** 保存失败

### Requirement: 新增缺陷表单展示

`AddDefect` MUST 在加载项目启用字段后，按 `sort_order` 渲染自定义字段表单项，并绑定 `form.customFields`。

#### Scenario: 无自定义字段的项目

- **WHEN** 项目无启用字段定义
- **THEN** 新增表单不展示自定义字段区域

### Requirement: 编辑缺陷表单展示

`EditDefectDialog` MUST 与新增一致支持编辑；打开时 MUST 将缺陷已有 `customFields` 填入表单。

#### Scenario: 编辑回显枚举

- **WHEN** 缺陷 `custom_fields.severity` 为 `"P0"` 且枚举配置含 P0
- **THEN** 编辑框选中 P0 对应项

### Requirement: 查看缺陷只读展示

`HandleDefect` MUST 在详情区域展示已启用字段的标签与格式化值（只读）；停用字段若有历史值 MVP 可折叠为「已停用字段」或不展示（实现时选「不展示」）。

枚举 MUST 以 label + color 样式展示（与 `DefectStateFlag` 类似色标）。

图片/文件 MUST 复用现有预览/下载交互（缩略图或链接列表）。

#### Scenario: 查看对象类型

- **WHEN** 字段类型为 `object` 且值为 JSON 对象
- **THEN** 详情以格式化 JSON 或键值只读列表展示

### Requirement: 前端共享组件注册表

前端 MUST 通过 `DefectCustomField/registry` 为各 `field_type` 提供编辑器与只读展示适配器，避免在 Add/Edit/Handle 三处重复 switch。

#### Scenario: 新增数组字段

- **WHEN** 字段类型为 `array` 且 `itemType` 为 `string`
- **THEN** 用户可增删多行字符串项并提交为 JSON 数组
