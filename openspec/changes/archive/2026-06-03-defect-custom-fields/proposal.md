## Why

缺陷核心字段（类型、状态、处理人等）为全平台固定列，不同项目无法扩展业务属性（环境、根因分类、客户单号等）。需要在**项目级**配置字段定义，在**缺陷实例**上以 JSON 存储取值，并贯通列表、Tab 筛选、导入导出、新增/编辑/查看，且与现有列显隐本地缓存机制一致。

## What Changes

- **数据库**
  - `sys_defect` 新增 `custom_fields`（JSON），存 `{ fieldKey: value }`
  - 新增 `sys_project_defect_field`（项目缺陷字段定义表）
- **后端**
  - 字段定义 CRUD API（按 `project_id`，权限对齐项目设置）
  - 缺陷创建/更新/查询读写 `custom_fields`；服务端按定义校验类型、必填、长度
  - 列表查询支持 `params.customFieldFilters`（Tab/查找 MVP）
- **前端 — 项目设置**
  - `project/option` 新增「缺陷设置」卡片，链接至自定义字段管理页（增删改查：名称、类型、长度、必填、可空、开启；枚举含 key/label/color）
- **前端 — 缺陷生命周期**
  - `AddDefect`、`EditDefectDialog` 动态表单项；`HandleDefect` 只读展示
  - 共享 `defect-custom-field` 组件注册表（类型全上：字符串、数值、布尔、日期时间、枚举、对象 JSON、图片、文件、数组及数组元素子类型）
- **前端 — 列表与交换**
  - 表格/Excel：动态列，列 key `custom:{fieldKey}`，并入现有 `defect-tabledefect-table-field-list` 用户本地缓存
  - 导入/导出动态列（与 Excel 视图列选择一致）
- **前端 — Tab/查找 MVP**
  - `DefectTabDialog` 支持按已开启自定义字段筛选；`config.customFieldFilters`
  - 扩展 `defect-query-extension.js` 注册自定义筛选键清理逻辑（与参与筛选不冲突）

## Capabilities

### New Capabilities

- `defect-custom-field-schema`：项目字段定义表、CRUD、类型与 `type_config` 约束
- `defect-custom-field-values`：`custom_fields` 读写、表单/详情渲染与校验
- `defect-custom-field-list-export`：列表列、Excel、导入导出、用户列显隐缓存
- `defect-custom-field-query`：Tab 与列表 JSON 筛选（Mapper 双库）

### Modified Capabilities

- （无）不修改既有固定缺陷字段语义；`sys_project_defect_tabs.config` 仅**扩展** `customFieldFilters` 字段，不破坏现有键

## Impact

- **模块**：`cat2bug-platform-system`（Domain/Mapper/Service）、`cat2bug-platform-admin`（Controller）、`cat2bug-platform-ui`（项目设置、缺陷全链路）、Flyway `h2` + `mysql`
- **依赖**：现有上传组件（图片/文件）、`Cat2BugTable` / `excel.vue` 列缓存、`searchQuery` / Tab 切换
- **测试**：字段校验单测、JSON 筛选 H2/MySQL 集成测试、手工 TESTING.md 路径
- **Non-Goals（MVP）**
  - Open API / IDEA 插件 / 缺陷分享页自定义字段展示
  - 可视化对象 schema 设计器（对象类型用 JSON 编辑器 + 校验）
  - 项目级「默认列显隐」下发（沿用用户本地缓存）
  - 统计块按自定义字段聚合

## 产品决策（探索阶段已确认）

1. **字段类型一期全上**（含数组嵌套子类型）
2. **Tab/列表筛选纳入 MVP**
3. **列显隐沿用用户本地缓存**（`defect-tabledefect-table-field-list`）
4. **新增、编辑、查看缺陷均展示自定义字段**
