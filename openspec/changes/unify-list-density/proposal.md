## Why

业务列表页同时存在两套密度：工具栏控件已强制 32px，表格行仍接近 Element Plus 默认偏扁的 cell padding；约 40 页仍用裸 `el-table`，只有缺陷/用例等少数页走 `Cat2BugTable`。另有 `size="small"` 标签大量残留，以及实心主按钮与空心输入框同高但视觉重量差大，暗色下尤其像「小输入 + 中按钮」。需要一次统一：列表松一点、工具栏视觉对齐，并把旧列表迁到同一套组件与工具条。

## What Changes

- 新增全站「表格轨」密度：单行最小行高约 **56px**、cell 上下约 **16px**（相对 EP default ~39px 肉眼可感）；操作列 link/text 跟随表格轨垂直节奏。
- 工具栏「视觉统一」：列表页主操作默认 `type="primary" plain`（高度 32px）；**暗色下须为描边/轻底**（`cat2bug-btn-outline` 或工具栏作用域），不得与实心 primary 同画；清理列表工具栏上无意义的 `size="small"`。
- 全局先抬高所有业务 `el-table` / `Cat2BugTable` 行呼吸感，使未迁页也立刻「松一点」。
- 首批将**交付物**（`module`）与 **OpenAI 账号**列表迁到 `Cat2BugTable` + `.list-page-toolbar`（或等价宿主布局），验收对齐缺陷/用例列表观感。
- 更新 `docs/04-UI规范/组件与交互约定.md`：补表格最小行高、工具栏主按钮 plain 约定、旧页迁移指引；不推翻现有 32px 控件轨。
- 不改后端接口与权限；树表（交付物）保留树能力，不因迁表丢掉 expand/折叠。

## Capabilities

### New Capabilities

- `list-density`: 列表页控件轨 / 表格轨 / 操作轨的密度与视觉重量约定，以及业务列表须使用 Cat2BugTable + 标准工具条的要求。

### Modified Capabilities

- （无）现有 `openspec/specs/` 中无列表密度级需求规格需改写；本变更新增能力即可。

## Impact

- 前端：`cat2bug-platform-ui` 全局样式（`cat2bug.scss` / `project-list-page.scss` / 必要时 `theme-dark.scss`）、`Cat2BugTable`、交付物页 `views/system/module/index.vue`、OpenAI 账号页 `views/system/project/ai/account/index.vue`。
- 文档：`docs/04-UI规范/组件与交互约定.md`；版本记录一行。
- 无 API / 数据库 / Native 体积变更。
- 后续批次（通知、成员等旧列表）可另开变更或在本变更 tasks 中列为可选后续，不阻塞首批验收。
