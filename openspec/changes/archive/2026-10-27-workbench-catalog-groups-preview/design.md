## Context

行排版编辑器 `WorkbenchRowEditor` 左侧目录目前是 `listCatalogWidgets()` 的扁平 chip：仅标题、可拖，无分组、无预览、无本页占用提示。列内下拉仍按列过滤同 `type`。注册表 `widget-registry.js` 无 `group` / preview 字段。仪表盘编辑主路径只挂行编辑器；`WorkbenchFreeGrid.vue` 仍在仓内但未挂载，故本期做共享目录组件，行编辑器接入，FreeGrid 可日后复用。

约束：同列 `type` 唯一、跨列可重复；不改 v3 持久化；预览必须真图且不变形。

## Goals / Non-Goals

**Goals:**

- 目录按产品业务域分组（可折叠）。
- 每项真图预览；固定画幅宽高比；图片 `object-fit: contain`，禁止拉伸/cover 裁切变形；缺图同比例占位。
- 本编辑面（当前 Tab 全部 rows）已出现的 `type` 显示「已添加」提示；仍可拖到其它列。
- 共享 `WorkbenchWidgetCatalog`（或等价名），`WorkbenchRowEditor` 使用；接口可被其它编辑面复用。
- 目录可见项全量交付真图资源。

**Non-Goals:**

- 不改为「整页 type 唯一」。
- 不做组件实况迷你渲染。
- 不重做 FreeGrid 布局/拖放引擎。
- 不改后端 API / config schema。

## Decisions

### 1. 分组维度：产品业务域

在 widget def 上增加 `group`（稳定 id，如 `overview` | `defect` | `case` | `document` | `git` | `plan` | `member` | `notice`）。分组标题走 i18n。`my-*` / `team-*` 同域归组，靠标题区分。

备选：按「我的 / 团队 / 统计」分组——否决，与已确认产品域扫法不一致。

建议映射（实现时可微调，须覆盖全部 `listCatalogWidgets()` 项）：

| group | 代表组件 |
|-------|----------|
| overview | my-kpi, team-type-kpi |
| notice | notices-summary |
| defect | my/team-defects, pending-repair, defect trends/charts/ranks, stat-defect… |
| case | case-pending-review, stat-case |
| document | my-documents, document-list, document-pin, stat-document |
| git | recent-commits, git-repo-stats, open-pull-requests |
| plan | plan-burndown, plan-statistics, action-list |
| member | stat-member, stat-module, member-defect-* |

### 2. 预览 = 真实组件缩略（非静态示意）

静态示意与窄栏直接渲染都会失真（KPI 在 ~220px 宽被挤成竖条）。改为 `CatalogLiveThumb`：在固定设计宽度（720px）挂载真实组件，再按侧栏宽度 **等比 `scale`**；外框高度 = 内容高 × scale，各卡片高度可不同。

- 手风琴同时只展开一组；折叠 `v-if` 卸载。
- `BoardChartHost` 传 `boardChart` + `embedded`。

备选：窄栏直接渲染——否决（挤扁）；统一固定画幅——否决。

### 3. 「本页已用」仅提示

从当前编辑的 `rows` 收集所有 `widgets[].type`（含 legacy `widget` 归一后）成 `Set`。目录项若 `id ∈ Set` 显示「已添加」角标/文案。拖拽与放置逻辑不变；同列重复仍由既有 `placeWidgetInCol` / 警告处理。

可选增强（非必须）：显示 `×N` 数量；本期默认布尔「已添加」即可。

### 4. 共享目录组件

新建例如 `WorkbenchWidgetCatalog.vue`：

- props：`usedTypes: Set|string[]`（或由父传入 `rows` 自行计算）、可选 `collapsedGroups`。
- emits / DnD：与现 chip 相同 `catalog:<type>` dataTransfer，供 `WorkbenchRowEditor` 现有 drop 复用。
- 聚合：`listCatalogWidgets()` + `group` 排序（定义 `CATALOG_GROUP_ORDER`）。

`WorkbenchRowEditor` 用该组件替换 aside 内扁平 `v-for`。列内 `el-select` 可保持文字列表（本期不强制带预览），避免下拉过重。

若日后 FreeGrid 恢复编辑目录，挂同一组件并传入其 layout 上的 used types。

### 5. 不新增 ADR

属编辑器 UI 约定，不改变架构分叉或存储底座；长期事实回写 `docs/01-产品需求/产品定位与核心功能.md` 工作台一行即可。无新 ADR。

## Risks / Trade-offs

- [真图维护成本] → 任务单列出图清单；命名与 widget id 对齐；缺图占位不破坏布局。
- [侧栏变宽/变长] → 分组默认折叠策略：有「已添加」的组展开，或全部默认展开第一屏可接受的组；可滚动。
- [预览比例不一致的源图] → 出图规范锁 16:10；contain 兜底留白，不裁切。
- [i18n 漏组名] → 组 id 枚举集中，补 zh-CN / en-US。

## Migration Plan

- 纯前端；无配置迁移。
- 回滚：还原目录组件与资源即可，不影响已存 rows。

## Open Questions

无。分组维度、真图保比例、提示语义、共享组件范围已确认。

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/01-产品需求/产品定位与核心功能.md`
- `docs/07-技术决策/ADR-020-工作台v3行排版与24栅格.md`
- 拟新增 ADR：无
