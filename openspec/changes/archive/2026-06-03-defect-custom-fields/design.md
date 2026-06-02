## Context

- 缺陷实体 `SysDefect` 已有固定列；`handle_by` 已使用 JSON 列（MySQL/H2），证明双库 JSON 可行。
- 列表列显隐与 Excel 共用 `defect-table` + `defect-tabledefect-table-field-list` 本地缓存（`excel.vue` / `Cat2BugTable`）。
- 自定义 Tab 配置存于 `sys_project_defect_tabs.config`（JSON），今日含 `defectType`、`params.defectStates` 等固定筛选项。
- 列表查询扩展机制见已归档变更 `defect-query-extension-params`：`queryParams.params` + `clearExtensionParams()`。

## Goals / Non-Goals

**Goals:**

- 项目级字段定义与缺陷级 JSON 取值分离（Schema / Values）
- 类型全量支持；统一前端 `registry` 适配编辑、展示、列表单元格、Excel、Tab 筛选
- MVP 覆盖：项目设置 CRUD、新增/编辑/查看、table/excel、Tab 筛选、导入导出
- 双库 Flyway 迁移；服务端校验与 `field_key` 稳定键

**Non-Goals:**

- 字段定义跨项目复制、版本历史、审批流
- 团队级（非项目）字段模板
- 分享页、报告模板、Open API 字段暴露（后续变更）
- 管理员强制列布局（仅用户本地缓存）

## Decisions

### 1. 表结构

#### `sys_project_defect_field`

| 列 | 类型 | 说明 |
|----|------|------|
| field_id | bigint PK | |
| project_id | bigint | 索引，非空 |
| field_key | varchar(64) | 项目内唯一，**创建后不可改** |
| field_label | varchar(128) | 显示名 |
| field_type | varchar(32) | 见类型枚举 |
| max_length | int | 字符串最大长度，其它类型可空 |
| required | tinyint | 必填 |
| nullable | tinyint | 可空；`required=1` 时忽略 |
| enabled | tinyint | 是否开启（软停用） |
| sort_order | int | 表单/列表默认顺序 |
| type_config | json/text | 类型扩展配置 |
| create_time / update_time | datetime | |

**唯一约束：** `(project_id, field_key)`

#### `sys_defect.custom_fields`

```json
{
  "env": "prod",
  "severity": "P0",
  "extra_images": ["url1", "url2"],
  "meta": { "rootCause": "config" },
  "tags": ["regression", "ui"]
}
```

键 MUST 为 `field_key`；停用字段（`enabled=0`）的历史键可保留但不展示、不校验必填。

### 2. 字段类型枚举与 `type_config`

| field_type | type_config 示例 | 存储值形态 |
|------------|------------------|------------|
| string | `{ "trim": true }` | string |
| number | `{ "integer": false, "min": null, "max": null }` | number |
| boolean | `{}` | boolean |
| datetime | `{ "format": "yyyy-MM-dd HH:mm:ss" }` | string（统一格式） |
| enum | `{ "options": [{ "key", "label", "color" }] }` | string（存 key） |
| object | `{ "mode": "free" }` | object |
| image | `{ "maxCount": 9, "maxMb": 5 }` | string[]（URL，对齐 img_urls） |
| file | `{ "maxCount": 9, "maxMb": 30 }` | string[]（URL，对齐 annex_urls） |
| array | `{ "itemType": "string\|number\|boolean\|datetime\|enum\|object\|image\|file", "maxItems": 50, "enumOptions"? }` | array |

**数组元素为 enum/object/image/file 时：** 递归遵守对应类型规则；UI 用行内增删列表。

**对象类型 MVP：** Monaco/textarea JSON 编辑器 + 服务端 `JSON.parse` 校验；不做可视化 schema 设计器。

### 3. 字段定义生命周期规则

| 操作 | 规则 |
|------|------|
| 创建 | 校验 `field_key` 格式 `^[a-z][a-z0-9_]{0,63}$`；单项目上限 **50** 条 |
| 改 label / sort / type_config | 允许 |
| 改 field_type | **禁止**（若已有任意缺陷该键非空） |
| 改 field_key | **禁止** |
| 删除 | **软删** `enabled=0`；不从历史 `custom_fields` 抹 key |
| 必填 | 保存缺陷时：仅对 `enabled=1` 且 `required=1` 的键校验 |

### 4. 后端 API（草案）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/system/project/{projectId}/defect-fields` | 列表（含停用，管理页） |
| GET | `/system/project/{projectId}/defect-fields/enabled` | 仅启用（表单/列表） |
| POST | `/system/project/{projectId}/defect-fields` | 新增 |
| PUT | `/system/project/{projectId}/defect-fields/{fieldId}` | 更新 |
| DELETE | `/system/project/{projectId}/defect-fields/{fieldId}` | 软删 enabled=0 |

缺陷 CRUD 在现有接口上扩展 `customFields` 字段（与实体一并序列化）。

**权限：** `system:project:option` 或复用 `system:project:edit`（实现时与项目 API 密钥同级对齐）。

### 5. 列表筛选（Mapper）

请求体扩展：

```javascript
queryParams.params.customFieldFilters = [
  { fieldKey: 'env', op: 'contains', value: 'prod' },
  { fieldKey: 'severity', op: 'in', value: ['P0','P1'] },
  { fieldKey: 'tags', op: 'hasValue' }  // 复杂类型 MVP
]
```

**运算符 MVP：**

| 类型 | 支持 op |
|------|---------|
| string | `contains`, `eq`, `isEmpty`, `isNotEmpty` |
| number | `eq`, `between`, `isEmpty`, `isNotEmpty` |
| boolean | `eq` |
| datetime | `between`, `isEmpty`, `isNotEmpty` |
| enum | `in`, `eq`, `isEmpty`, `isNotEmpty` |
| object / image / file / array | `isEmpty`, `isNotEmpty`（MVP）；`array` 另支持 `contains`（字符串化匹配） |

**SQL 策略（方案 B）：**

- MySQL：`JSON_EXTRACT(custom_fields, '$.key')` + 动态 `<if>`
- H2：`JSON_VALUE(custom_fields, '$.key')` 等等价函数
- **必须** 提供 H2 + MySQL 各一条集成测试

**性能：** MVP 不做 JSON 索引；单项目字段数 ≤50，列表分页可接受。

### 6. 前端架构

```
cat2bug-platform-ui/src/
  api/system/defect-field.js
  components/DefectCustomField/
    registry.js           # type → { editor, display, cell, excel, filter, validate }
    DefectCustomFieldsSection.vue
    editors/*.vue
    displays/*.vue
    filters/*.vue
  views/system/project/defect-fields/index.vue   # CRUD 管理页
  views/system/project/option/item/DefectSettingsCard.vue
```

**挂载点：**

| 场景 | 组件 |
|------|------|
| 新增 | `AddDefect.vue` — `form.customFields` |
| 编辑 | `EditDefectDialog.vue` |
| 查看 | `HandleDefect.vue` — collapse「扩展字段」 |
| 表格 | `table.vue` — 动态列 + formatter |
| Excel | `excel.vue` — 合并进 `COLS` 生成逻辑 |
| Tab | `DefectTabDialog.vue` — `config.customFieldFilters` |

**列缓存键：** 动态列 `name` / `titleKey` 使用 `custom:{fieldKey}`，与 `KEYS_IN_EXCEL_FOR_PICKER` 生成逻辑同步。

**查询扩展：**

```javascript
export const DEFECT_QUERY_EXTENSION_KEYS = {
  participation: ['participationLogDate', 'participationUserId'],
  customFields: ['customFieldFilters']  // 整包替换
}
```

Tab 切换 / `searchQuery` 无 extension 时清除 `customFieldFilters`。

### 7. 导入导出

- **导出：** 在现有 Excel 导出流程上，按启用字段追加列头（`field_label`）与单元格字符串化规则（枚举导出 label 或 `key:label`，实现时统一为一种并写入 spec 场景）。
- **导入：** 解析列头匹配 `field_key` 或 label 映射表；按类型校验；图片/文件列导入为 URL 文本或留空（文件上传列不走 Excel 二进制 MVP，与现网 excel 图片列一致：路径/后续手工补传）。
- 与 `@Excel` 注解静态列并存：自定义列**不**加到 `SysDefect` 注解，走动态列配置。

### 8. 实现轨道（单变更内排期）

```
A 数据层 + 定义 CRUD + 项目设置页
B 表单/详情（Add/Edit/Handle）+ 服务端校验
C 列表动态列 + 列显隐缓存
D Tab 筛选 + Mapper + query-extension
E 导入导出动态列
```

类型全上：B 与 E 工作量最大，可并行开发，但集成测试放在 E 之后。

## Risks / Mitigations

| 风险 | 缓解 |
|------|------|
| H2/MySQL JSON SQL 不一致 | 双库集成测试 + Helper 封装 |
| Excel 复杂类型导入失败率高 | 文档明确图片/文件列导入限制；校验错误行提示 |
| 数组嵌套 UI 复杂 | registry 单测 + Storybook 式手工用例 |
| 历史数据与改类型 | 禁止改类型；软删字段 |
| 查询性能 | 字段数上限 50；后续可加虚拟列索引 |

## Open Questions

- 枚举导入/导出默认用 **key** 还是 **label**？（tasks 实现前在 spec 场景定死为：导出 `label`，导入优先 key，label 回退匹配）
- 自定义字段变更是否写缺陷操作日志？MVP 建议：**写入**（与描述变更同级），便于审计
