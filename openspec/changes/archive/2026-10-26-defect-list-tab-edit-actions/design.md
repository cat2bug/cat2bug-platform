## Context

缺陷列表页签条（`views/system/defect/index.vue`）已有：自定义页签 + 固定「全部」「已删除」+ 加号占位 pane；创建走 `DefectTabDialog` → `POST /system/tabs`；删除走确认 + `DELETE /system/tabs/{id}`。关闭叉用 `legacy-icon` **常显**，无编辑入口。工作台（`dashboard/index.vue`）已用悬停 `.wb-tab-actions`（`EditPen` + `CloseBold`）作为统一交互。后端 `PUT /system/tabs`（`updateTabs`）已存在，对话框未接编辑模式。

## Goals / Non-Goals

**Goals:**

- 自定义缺陷页签悬停显示编辑 + 删除；默认不占视觉噪音。
- 编辑复用创建对话框字段（名称、关键词、关注/删除开关、有序筛选字段），提交 `updateTabs`。
- 操作图标与工作台视觉对齐；点击不切换页签（`stop` / `mousedown.stop`）。
- 若编辑的是当前激活页签，保存后按新 config 重新应用查询（沿用现有 `selectDefectTabHandle` / 抑制回声路径）。

**Non-Goals:**

- 不引入项目共享缺陷页签（仍为 per-user `userId` 私有）。
- 不改「全部」「已删除」语义，不给系统页签加编辑/删除。
- 不引入「可见页签仅剩 1 个禁删」（系统页签始终存在）。
- 不改 `defect-tab-list-fetch` 的缓存与单次 list 约定。
- 不改页签拖拽排序（若已有则保持；本次不做新排序能力）。

## Decisions

### 1. 悬停操作区对齐工作台样式语义，类名可本地化

- **选择**：在缺陷页签 label 内加 `.defect-tab-actions`（结构同 `.wb-tab-actions`：默认 `display: none`，`.el-tabs__item:hover` 时 `inline-flex`），内含编辑与删除图标。
- **备选**：直接复用工作台 class 名 → 跨页耦合，否决。
- **备选**：始终显示操作区 → 与用户明确要求的「参考工作台动态显示」不符，否决。

### 2. 编辑 = 对话框双模式，不进页内编辑态

- **选择**：`DefectTabDialog` 增加 `open(tab?)`：无参创建、有参深拷贝 `tab` 进 form；标题/主按钮文案随模式切换；编辑调用 `updateTabs`，`$emit('update', data)`；父组件替换 `config.tabs` 对应项。
- **备选**：页内侧栏编辑筛选 → 范围过大，否决。
- **备选**：仅支持改名 → 筛选项无法改，体验残缺，否决。

### 3. 删除仍确认；无「最后一页禁删」

- **选择**：保持现有 `$modal.confirm` + `delTabs`；自定义可删光。
- **理由**：固定「全部」「已删除」已兜底，与工作台「可隐藏工作台」模型不同。

### 4. 权限

- **选择**：仅展示当前用户自己的自定义页签（既有行为）；能看到即可编辑/删除。不做管理员共享 ACL。
- **理由**：数据模型无 visibility 字段。

### 5. 图标

- **选择**：Element Plus `EditPen` / `CloseBold`，与工作台一致；移除常显 `legacy-icon` close。

## Risks / Trade-offs

- [悬停在触控设备不可用] → 可后续补 long-press / 右键；本期与工作台一致，接受。
- [编辑保存后 list 重复] → 必须走现有 suppress + 单次 `selectDefectTabHandle`；勿在 dialog 关闭后再手动多调一次 list。
- [深拷贝 config 漏字段] → open(edit) 时用结构化克隆或等价深拷贝；自定义字段数组独立拷贝。

## Migration Plan

- 纯前端行为变更，无库表 / API 迁移。
- 回滚：还原 `index.vue` label 与 `DefectTabDialog` 即可。

## Open Questions

- 无（名称 maxlength 保持对话框现有 32，不在本期收紧）。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`（已补「列表自定义页签」悬停操作）
- `docs/01-产品需求/产品定位与核心功能.md`（缺陷页签条目，事实未变则可不改）
- `docs/02-系统架构/数据库.md`（`sys_project_defect_tabs`，无 schema 变更）
- 拟新增 ADR：无（交互对齐，不形成新架构约束）
