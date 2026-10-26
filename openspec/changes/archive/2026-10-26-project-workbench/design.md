## Context

- 现状：`cat2bug-platform-ui/src/views/system/dashboard/index.vue` 固定拼接约 10 类统计/活动卡片；路由 `/{teamKey}/{projectKey}` 与 `.../dashboard` 为项目落地页；后端 `/system/dashboard/{projectId}/*` 齐全但只服务「看数」。
- 产品决策（已确认）：一期仅**项目内**；菜单/叙事改「工作台」；一期落地 **A（轻定制）+ C（角色预设）+ D（双页签）**；原统计墙在「看板」页签内**拆成可开关组件**；二期再做 **B（自由栅格）**；配置服务端按用户×项目持久化。
- 可借鉴：缺陷统计模版 `SysUserStatisticTemplate` 的 JSON 配置思路；各业务列表/抽屉已有操作能力，工作台组件应复用而非重写整页。

## Goals / Non-Goals

**Goals:**

- 项目落地页成为可干活的工作台：待办优先、台面内快捷操作、角色预设 + 显隐/排序定制。
- 原仪表盘能力不丢：以「看板」页签下的可开关统计组件呈现。
- 布局配置可跨设备同步（用户×项目）。
- 二期栅格在同一配置模型上扩展，避免推倒重来。
- 任务驱动交付：先效果图确认，再按 tasks 逐项实现。

**Non-Goals:**

- 一期不做跨项目总览工作台。
- ~~一期不做自由拖拽栅格~~（二期 B 已按 D8 落地）。
- 一期不改路由段名（仍 `dashboard`）；不做 MCP 模块键重命名。
- 不在工作台内嵌完整缺陷/用例/计划整页；复杂操作跳转模块或开抽屉。
- 不新增独立 ADR（决策记在本 design；若二期栅格引入重型依赖再评估 ADR）。

## Decisions

### D1. 信息架构：双页签 + 统一布局配置

- **选择**：壳层固定页签「工作台 | 看板」；两页签各自维护一份**有序组件 id 列表**（及每组件可选 filter 参数）；「编辑工作台」同时可编辑当前页签的显隐与排序。
- **工作台 vs 看板（产品边界）**：

| | 工作台 | 看板 |
|---|---|---|
| 视角 | **我**（当前用户） | **项目**（全员/全局） |
| 目的 | 干活 + 看我的进度 | 看项目健康度与趋势 |
| 内容 | 我的待办列表、台面操作、**个人统计图表** | 原仪表盘类项目级统计（可开关） |
| 默认问题 | 「今天我该做什么？我做得怎样？」 | 「项目整体怎样？」 |

- **备选**：仅单页混排统计与待办 → 信息噪音大；看板整页保留原 `index.vue` 不可拆 → 违反「拆成可开关组件」。
- **理由**：D 满足过渡；统计墙组件化满足决策 2；个人统计放工作台避免与项目看板抢叙事。

### D2. 一期定制模型（A）：显隐 + 排序，非栅格

- **配置 JSON 草案**（存服务端）：

```json
{
  "version": 1,
  "preset": "tester",
  "tabs": {
    "work": {
      "widgets": [
        "my-kpi",
        "my-defect-trend",
        "my-defects",
        "today-plan-items",
        "quick-create",
        "case-pending-review"
      ]
    },
    "board": { "widgets": ["stat-defect", "stat-case", "defect-state-chart", "action-list", "plan-burndown"] }
  }
}
```

- 未知 `widgetId` 忽略；新版本组件可进默认预设，不强制写入已有用户配置。
- **二期 B 扩展**：同结构增加 `layout: { i, x, y, w, h }`，`version` 升 2；一期客户端忽略 layout 字段。

### D3. 角色预设（C）

| preset | 工作台默认组件 | 看板默认组件 |
|---|---|---|
| `tester` | 我的 KPI、我的缺陷趋势、我的缺陷、今日计划项、待评审用例、快捷新建 | 轻量项目 KPI（缺陷/用例/计划）+ 活动 |
| `developer` | 我的 KPI、我的处理量趋势、待我修复、待我审 PR（若有权限）、最近提交、快捷新建 | 项目缺陷趋势 + 活动 |
| `manager` | 测试向个人待办 + **源码概览** + **PR 列表**（不含活动/成员排行） | 项目级统计：缺陷/用例/模块/文档/成员 KPI 墙 + 走势/计划/燃尽 |
| `blank` | 快捷新建 | 空（引导添加） |

- 套用预设 = 覆盖当前用户该项目配置（需确认对话框）；「重置为预设」同理。

### D4. 组件注册表

- 前端 `widgets` 注册表：`id`、`tabAffinity`（`work` / `board` / `both`）、`titleKey`、Vue 组件、默认尺寸提示（二期用）、所需权限。
- **工作台（work）一期组件**：
  - 待办：`my-defects`、`defects-pending-repair`、`today-plan-items`、`case-pending-review`、`quick-create`、`notices-summary`、`recent-commits`、`prs-to-review`（无权限不进组件库）。
  - **个人统计（强制「当前用户」过滤）**：
    - `my-kpi`：待我处理缺陷数、待我审核、今日已处理、今日计划项进度（通过/总数）等数字卡。
    - `my-defect-trend`：近 14/30 天「我处理/关闭/驳回」折线或柱状。
    - `my-plan-progress`：我参与的进行中计划项完成率（可选）。
    - `my-workload-mix`：我名下缺陷按状态分布（小环图/条，可选）。
  - 个人统计 API 建议：`GET /system/workbench/{projectId}/me/stats`（及 trend 子路径），不得复用未带 `userId=当前用户` 的项目级 dashboard 接口以免串数。
- **看板（board）一期组件**：迁移现有 dashboard 子组件（**项目级**）——`stat-case`、`stat-defect`、`stat-module`、`stat-document`、`stat-member`、`defect-state-chart`、`member-defect-line`、`plan-statistics`、`action-list`、`member-defect-rank`、`plan-burndown`。
- 台面操作：缺陷组件内快捷指派/通过/驳回/关闭（复用现有 API 与抽屉）；计划项行内通过/失败；其余「打开模块」。

### D5. 持久化

- 新表（建议）`sys_user_workbench`：`user_id`、`project_id`、`config_json`、时间戳；唯一键 `(user_id, project_id)`。
- API：`GET/PUT /system/workbench/{projectId}`，权限：项目成员 + 能访问 dashboard 模块。
- 无配置时：按用户项目角色映射默认 preset（测试→tester，开发→developer，管理员/项目负责人→manager，否则 tester），不落库直至用户首次保存或「套用预设」。

### D6. 路由与文案

- 路径不变：`dashboard` 模块；i18n `dashboard` 展示为「工作台」；看板页签文案「看板」。
- `project-path-route` / `project-scoped-module-routes` 需求用语改为 workbench，路径行为不变。

### D7. 效果图先行

- 预览 HTML 放在 `openspec/changes/project-workbench/preview/`（工作台默认态、编辑态、看板组件开关、角色预设选择）。
- 实现任务勾选前以预览为视觉基准；大改须同步更新预览。

### D8. 二期 B：自由栅格（已选定方案）

- **选择**：**自研 12 列 CSS Grid** + pointer 事件拖位置 / 右下角改尺寸；**不新增 npm 依赖**（已有 `vuedraggable` 仅用于一期排序编辑的小屏回退路径）。
- **备选**：`grid-layout-plus`（vue-grid-layout Vue3 移植）→ 能力全但增依赖与包体积，需另开 ADR；本期不做。
- **配置**（`version: 2`）：

```json
{
  "version": 2,
  "preset": "tester",
  "tabs": {
    "work": {
      "widgets": ["my-kpi", "my-defects", "..."],
      "layout": [
        { "i": "my-kpi", "x": 0, "y": 0, "w": 12, "h": 2 },
        { "i": "my-defects", "x": 0, "y": 2, "w": 7, "h": 6 }
      ]
    },
    "board": { "widgets": ["..."], "layout": ["..."] }
  }
}
```

- `layout[]`：`i`=widgetId，`x/y` 为格点坐标（0-based），`w/h` 为跨列/跨行；列数固定 **12**；行高约 **48px**（`--wb-grid-row-h`）。
- 组件默认 `w/h` 写在注册表 `defaultW` / `defaultH`；无 layout 时按 widgets 顺序自动装箱生成。
- **查看态**：`version >= 2` 且当前页签有 `layout` → 自由栅格；否则保持一期分区布局（`wb-proto`）。
- **编辑态**：进入编辑即升级为 v2 并保证有 layout；拖手柄改 `x,y`，右下角改 `w,h`（`minW/minH` 默认 2）；保存写回服务端。
- **小屏**（`max-width: 960px`）：忽略 `x/y/w`，按 `y` 再 `x` 纵向单列堆叠（一期可读性），编辑亦同。
- **不新增 ADR**（无新依赖）；决策记于本 design。

## Risks / Trade-offs

- [组件内操作与列表页行为不一致] → 快捷操作只覆盖高频动作；其余跳转模块；复用同一 API。
- [看板组件过多导致首屏慢] → 按页签懒加载；看板进入后再请求统计。
- [预设覆盖用户自定义] → 套用前二次确认。
- [二期栅格配置迁移] → version 字段 + 忽略未知字段；一期不写 layout。
- [角色与 preset 映射不准] → 允许用户随时换预设；映射表可配置于前端常量。

## Migration Plan

1. 发布前端工作台壳：无后端配置时用角色默认预设，行为可用。
2. 发布表 + API；前端改为读写配置。
3. 菜单/i18n 文案切换；旧「仪表盘」文案下线。
4. 回滚：保留 API 读兼容；前端可 feature-flag 回退到旧 `index.vue` 布局（短期）。

## Open Questions

- 角色→预设的精确映射是否以项目内角色名为准（需对照现有角色字典）——实现时按现有角色字典落地，可调常量。
- 「待我审 PR」是否一期必做，还是二期组件（取决于 git PR 列表 API 成熟度）——默认一期做占位/有权限才注册。

## 产品确认（2026-10-21）

- 效果图方向通过（现网侧栏 + 浅/暗主题 + 个人统计进工作台）。
- 工作台 = 我的待办 + 我的统计；看板 = 项目级统计（原仪表盘拆件）。
- 一期 A+C+D，二期 B；仅项目内；路由段仍 `dashboard`。

## 相关长期文档

- `docs/01-产品需求/产品定位与核心功能.md`（功能域「仪表盘统计」待回写为工作台）
- `docs/00-项目总览.md`
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`（dashboard 段）
- `docs/04-UI规范/组件与交互约定.md`（列表工具条等，工作台内列表应对齐）
- 拟新增 ADR：无（一期与二期栅格均未引入新依赖）
