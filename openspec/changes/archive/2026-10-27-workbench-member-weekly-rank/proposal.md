## Why

工作台已有「成员缺陷排行」（累计/当日、仅缺陷），缺少一张**本周跨模块成员贡献**视图——把缺陷处理、代码提交、文档处理放在同一行对比。现有周报组件是项目聚合，不是按成员展开。**不做排名**（无加总名次、无名次角标）。

## What Changes

- 新增工作台组件 **成员本周贡献**（稳定 id 仍为 `member-weekly-rank`）：
  - 每行左侧头像，右侧上方显示用户名称；
  - 下方三条不同色/状态的进度条：缺陷处理数、代码提交数、文件处理数（一期＝项目文档本周新建或更新次数）；
  - **不按加总排名**；列表按展示名排序；进度条百分比按各维本周最大值归一。
- 时间窗与周报一致：周一 00:00～周日 23:59:59（服务端按项目时区或服务器本地日界，与现有周报对齐）。
- 新增服务端聚合接口，一次返回成员列表与三维计数；前端只渲染，不做多源拼装排序。
- 组件挂入 widget 注册表与目录（`group: member`），走现有目录分组/预览/已用标记能力；**不替换**既有 `member-defect-rank`。
- 可选：将组件加入 manager / developer 预设的一行（非破坏性；若预设表过满可仅进目录）。

## Capabilities

### New Capabilities

- `workbench-member-weekly-rank`: 工作台「成员本周贡献」组件的展示、列表顺序、指标口径、空态与聚合 API 契约。

### Modified Capabilities

- （无）目录/行排版能力本身不因新增一个 widget id 而改 REQUIREMENTS；注册与预览沿用 `workbench-widget-catalog` 既有规则。

## Impact

- 前端：`widget-registry.js` 注册；新 Vue 组件（可参考 `MemberOfDefectRank` 行布局）；`BoardChartHost` 或独立 host；i18n；目录 live 预览；可选 presets。
- 后端：`DashboardResource`（或等价）新增成员周排行查询；缺陷日志按周聚合；文档按 `sys_document` 本周 create/update 聚合；Git 提交按项目仓库本周 author→成员匹配后计数。
- 权限：沿用 `system:dashboard:query`；Git/文档计数仅统计当前项目可见数据。
- 文档：实现后在 `docs/01` 工作台组件列表补一行；接口清单补 API；一般无需新 ADR（未推翻既有决策）。
- 性能：Git 周统计可能偏重，设计中需限深/截断策略，避免每次全历史扫描。
