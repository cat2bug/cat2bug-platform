## Context

缺陷模块（`sys_defect`）当前通过 `DELETE` 物理删除；缺陷列表页使用 `sys_project_defect_tabs` 存储用户自定义筛选，内置「全部缺陷」由前端常量 `all-tab` 渲染。项目其它实体（用户、角色等）已采用若依 `del_flag`：`0` 存在、`2` 删除。缺陷日志 `SysDefectLogStateEnum` 使用 `ordinal()` 持久化，现有类型至 `IMPORT` 为止，尚无删除/恢复类型。

## Goals / Non-Goals

**Goals:**

- `sys_defect.del_flag` 默认 `'0'`，软删置 `'2'`，列表默认过滤未删除
- 删除/恢复写入 `sys_defect_log`（`DELETE` / `RESTORE`）
- 内置「已删除缺陷」页签 + 自定义页签「已删除」开关
- 已删行：表格操作仅恢复与查看；Excel 行不可编辑
- Tab 图标区分：全部 / 已删 / 自定义
- H2 + MySQL 双迁移

**Non-Goals:**

- `delete_time`、`delete_by`、`delete_by_id` 列
- 已删除数据的物理 purge
- 在「已删除」Tab 新建缺陷
- OpenSpec 范围外的报表/第三方集成全面改造（仅列出需同步过滤的 Mapper）

## Decisions

### 1. `del_flag` 取值与默认值

- **决策**: `char(1) NOT NULL DEFAULT '0'`；仅使用 `0`（存在）与 `2`（删除）。
- **理由**: 与 `readme/development/database-design.md` 及若依一致；`1` 保留给 `status` 停用语义，避免混淆。
- **备选**: `is_deleted` boolean — 拒绝，与全项目字段名不一致。

### 2. 列表过滤参数

- **决策**: 在 `SysDefect.params` 中使用 `delFlag`（字符串 `'0'` | `'2'`）。内置「已删除」Tab 前端传 `params.delFlag='2'`；「全部缺陷」及默认自定义 Tab 传 `'0'` 或省略（后端按 `0` 处理）。
- **理由**: 与 `DefectTabDialog` 中 `params.collect` 模式一致，配置 JSON 无需改表结构。
- **Mapper**: `selectSysDefectList` 的 `<where>` 默认 `d.del_flag = '0'`；当 `params.delFlag == '2'` 时 `d.del_flag = '2'`。

### 3. 删除与恢复 API

- **决策**: 现有 `DELETE /system/defect/{ids}` 改为软删 + 批量写 `DELETE` 日志；新增 `PUT /system/defect/{id}/restore`（或 `POST .../restore`）置 `del_flag='0'` 并写 `RESTORE` 日志。
- **权限**: 恢复与删除共用 `system:defect:remove`（首版）；可按需后续拆分 `system:defect:restore`。
- **编辑/workflow**: `del_flag='2'` 时拒绝 `PUT`、指派、状态流转等（详情可读）。

### 4. 日志枚举扩展

- **决策**: 在 `SysDefectLogStateEnum` **末尾**追加 `DELETE`、`RESTORE`。
- **理由**: `SysDefectLogStateEnumTypeHandle` 使用 `ordinal()`；禁止在中间插入。

### 5. 前端 Tab 模型

```
[自定义 tabs…] | [全部缺陷] [已删除缺陷] | [+][统计]
     icon-C          icon-A      icon-B
```

- 常量: `ALL_TAB_NAME = 'all-tab'`，`DELETED_TAB_NAME = 'deleted-tab'`。
- 两者无关闭按钮；`selectDefectTabHandle` 对内置 Tab 覆盖 `delFlag`，不使用 DB 配置。
- 自定义 Tab：`DefectTabDialog` 最底部（按钮上方）`el-switch`，`v-model="form.config.params.delFlag"`，`active-value="2"` `inactive-value="0"`，默认 `0`。

### 6. `DefectTools` 已删态

- **决策**: `defect.delFlag === '2'` 时 `isDeleted`；隐藏 share、star、workflow、delete、edit 等；**仅保留 `restoreVisible` 与 `viewVisible`**（恢复权限同删除；查看沿用 `system:defect:query` 或现有 `viewVisible` 规则）。
- **实现位置**: `cat2bug-platform-ui/src/components/Defect/DefectTools/index.vue`；`table.vue` 操作列继续用 `@view` 打开详情，已删记录详情只读。
- `table.vue` 无需改列结构，依赖行数据 `delFlag`。

### 7. Excel 已删行只读

- **决策**: 双层防护 — (1) `handleSheetUpdate` 及计划时间/附件等保存路径若 `row.delFlag==='2'` 则拒绝；(2) `refreshExcelEditorView` 后为已删行单元格加 `readonly` class（与现有 patch 一致）。
- 「已删除」Tab 下可隐藏批量删除、占位新建行（工具栏级）。

### 8. Tab 图标

- 「全部缺陷」: 复用 `all.svg`（或新增 `defect-tab-all`）
- 「已删除缺陷」: 新增 `defect-tab-deleted.svg`（或等价）
- 自定义 Tab: 统一 `defect-tab-custom.svg`（或 `list2`）

### 9. 关联与统计

- **决策**: `SysDefectStatisticMapper`、`getProjectDefectMaxNum`、`selectVersionList`、`ApiDefectMapper`、计划缺陷列表等凡查询 `sys_defect` 的列表/计数，默认 `del_flag='0'`；按 ID 查详情允许返回已删（回收站打开）。
- 用例/计划关联列表：已删缺陷不展示或展示为只读（首版：列表 API 已过滤则不出现）。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| Mapper 漏加 `del_flag` 导致已删数据泄漏 | 清单式改 `SysDefectMapper` + 代码搜索 `from sys_defect`；集成测试列表/统计 |
| 日志 `ordinal` 错位 | 仅尾部追加枚举；迁移不改动历史 `defect_log_type` |
| Excel 列级 `readonly` 无法按行 | `handleSheetUpdate` 硬拦截 + DOM `readonly` class |
| 自定义 Tab 开关与内置「已删除」Tab 功能重叠 | 文档说明：内置=全项目已删；自定义=可叠加其它筛选 |
| 旧客户端仍调物理删除语义 | API 路径不变，行为变软删 — 在 release note 说明 |

## Migration Plan

1. 发布前执行 Flyway（H2/MySQL）：`ADD COLUMN del_flag CHAR(1) NOT NULL DEFAULT '0'`，历史行均为 `0`。
2. 部署后端（软删 + 恢复 + 列表过滤 + 日志枚举）。
3. 部署前端（Tab、DefectTools、Excel、i18n、图标）。
4. 回滚：前端可隐藏 Tab；后端回滚需保留 `del_flag` 列以免数据丢失（回滚代码时列可闲置）。

## Open Questions

- 无（产品侧已确认：无 `delete_time` 列；恢复权限同删除；自定义 Tab 默认未删除）。
