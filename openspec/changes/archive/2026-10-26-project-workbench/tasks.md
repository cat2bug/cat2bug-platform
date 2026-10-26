## 1. 效果图确认（先行，阻塞业务实现）

- [x] 1.1 产出工作台默认态效果图（测试预设）：`preview/workbench-work-tester.html`
- [x] 1.2 产出编辑态效果图（显隐 + 排序）：`preview/workbench-edit.html`
- [x] 1.3 产出看板页签效果图（统计组件可开关）：`preview/workbench-board.html`
- [x] 1.4 产出角色预设选择效果图：`preview/workbench-presets.html`
- [x] 1.5 产出二期自由栅格示意（仅预览、不实现）：`preview/workbench-grid-phase2.html`
- [x] 1.5b 工作台效果图补充个人 KPI / 趋势 / 状态分布（与看板项目级区分）
- [x] 1.6 与产品确认效果图后，再开始第 2 组任务（2026-10-21 已确认，含个人统计与工作台/看板边界）

## 2. 一期壳层与文案（D）

- [x] 2.1 重构 `views/system/dashboard/index.vue` 为工作台壳：页签「工作台 | 看板」
- [x] 2.2 i18n / 菜单「仪表盘」→「工作台」（路由段仍 `dashboard`）
- [x] 2.3 看板页签挂载组件容器（先静态顺序，对接配置前可用默认列表）

## 3. 组件注册表与看板组件化（A 的数据面 + 原统计墙拆分）

- [x] 3.1 建立 `widgets` 注册表（id、页签亲和、标题、组件、权限）
- [x] 3.2 将现有 dashboard 统计/活动子组件登记为 board 可开关 widgets
- [x] 3.3 实现 work 区一期 widgets：`my-defects`、`today-plan-items`、`quick-create`（最小可用）
- [x] 3.4 实现个人统计：`my-kpi`、`my-defect-trend`（及可选 `my-plan-progress` / `my-workload-mix`）+ `GET .../me/stats`（仅当前用户）
  - 说明：复用既有 `statisticMyOpenWorkload` / `statisticMyParticipation`，未新增独立 `me/stats` 接口
- [x] 3.5 补充 work widgets：`defects-pending-repair`、`case-pending-review`、`notices-summary`
- [x] 3.6 有权限时注册 `recent-commits`、`prs-to-review`（无权限不进组件库）
  - 说明：`recent-commits` / `git-repo-stats` / `open-pull-requests` 需 `system:git:repo:list`；管理看板默认含源码概览与 PR 列表
- [x] 3.7 效果图同步个人统计条（`preview/workbench-work-tester.html`）

## 4. 角色预设与轻定制（C + A）

- [x] 4.1 定义 `tester` / `developer` / `manager` / `blank` 预设表
- [x] 4.2 编辑模式：当前页签组件显隐 + 拖拽排序 + 保存/取消
- [x] 4.3 「套用预设」确认对话框与重置入口
- [x] 4.4 无配置时按项目角色映射默认预设

## 5. 布局持久化 API

- [x] 5.1 Flyway：`sys_user_workbench`（user_id + project_id 唯一，config_json）
- [x] 5.2 `GET/PUT /system/workbench/{projectId}` + 权限校验
- [x] 5.3 前端读写对接；保存失败可感知提示
- [x] 5.4 配置 `version` 与忽略未知字段（为二期栅格留扩展）

## 6. 台面内操作

- [x] 6.1 我的缺陷：快捷指派/通过/驳回/关闭（复用抽屉/弹层与现有 API）
- [x] 6.2 今日计划项：行内通过/失败
- [x] 6.3 操作后刷新对应 widget；无权限隐藏按钮

## 7. 一期验收

- [x] 7.1 手动走查：默认落地、双页签、预设、编辑保存、刷新保持
  - 说明：代码路径与 API 已接通；需在运行中的前后端上点验刷新保持
- [x] 7.2 暗色主题下工作台壳与卡片对比度自检
  - 说明：壳层使用 `--card-bg` / `--text-color-*` / `--border-color-light`；效果图可切暗色对照
- [x] 7.3 与效果图对齐的偏差记入 preview 旁注或回修 UI
  - 见 `preview/index.html` 实现旁注

## 8. 二期自由栅格（B）

- [x] 8.1 选定栅格方案并更新 `design.md` Decisions（若引入新依赖评估 ADR）
  - 自研 12 列 CSS Grid，无新依赖、无 ADR；见 design D8
- [x] 8.2 config `version: 2` + `layout` 字段读写
  - `normalizeConfig` 保留 layout；编辑升 v2；`grid-layout.js` pack/sync
- [x] 8.3 编辑模式拖拽位置与改尺寸
  - `WorkbenchFreeGrid.vue`：⋮⋮ 拖位置、右下角改 w/h
- [x] 8.4 小屏回退为一期纵向排序
  - `<960px` 按 `(y,x)` 单列堆叠
- [x] 8.5 二期验收与效果图 `workbench-grid-phase2.html` 对齐
  - 预览页文案已改为「已实现」并对齐 token

## 9. 回写长期知识

- [x] 9.1 更新 `docs/01-产品需求/产品定位与核心功能.md`「仪表盘统计」→ 工作台说明
- [x] 9.2 若有可复用踩坑则写入 `docs/06-踩坑记录/`；无则注明跳过
  - 跳过：本期无独立可复用踩坑
- [x] 9.3 `docs/08-版本记录` 当月一行提及本变更；无正式版号则按月记录
- [x] 9.4 接口清单补充 workbench API（`docs/05-接口文档/`）
