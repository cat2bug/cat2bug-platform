## 1. 后端：自定义 Tab 持久化

- [x] 1.1 Flyway 新增 `sys_project_workbench_tabs`（或等价表名）：tab_id、project_id、owner_user_id、visibility、tab_name、preset、config_json、tab_sort、时间戳
- [x] 1.2 实体 / Mapper / Service：按项目列出「共享 ∪ 当前用户私有」；创建时共享计数 ≤24；私有不限
- [x] 1.3 REST：`GET/POST /system/workbench/{projectId}/tabs`，`PUT/DELETE .../tabs/{tabId}`（布局与改名）；鉴权：私有仅 owner，共享写删改名仅 create-by/admin；非管理员 POST 强制 private
- [x] 1.4 个人 `GET/PUT /system/workbench/{projectId}` 保持兼容；文档/注释标明仅承载固定「工作台」；读侧可忽略 board 字段

## 2. 预设与配置模型

- [x] 2.1 `presets.js`：新增 `document`（空白+文档组件）、`outsider`、`custom`（空白槽）；去掉预设对 `tabs.board` 的依赖
- [x] 2.2 `presetIdFromRoleKeys`：create-by/admin→manager，develop→developer，tester→tester，否则 outsider
- [x] 2.3 `alignLegacyConfig` / `normalizeV3Config`：剥离或忽略 `tabs.board`；自定义 tab payload 约定为 `{ version:3, preset, rows }`
- [x] 2.4 `widget-registry`：取消 board-only 亲和，原看板统计/图表进入工作台组件库

## 3. 前端壳：去掉看板 + 页签条

- [x] 3.1 `dashboard/index.vue`：移除看板 pane 与 board 编辑勾选逻辑
- [x] 3.2 页签条：固定「工作台」+ 共享列表 + 私有列表 + 加号占位（参考缺陷 `__defect_add_tab__`，click 开弹窗不选中）
- [x] 3.3 本地缓存 `workbench-tab:{projectId}`：切换写入；挂载恢复；失效回 work；恢复不进编辑态
- [x] 3.4 拉取项目角色：控制加号「显示类型」与共享 tab 的编辑/删/改名入口；普通成员看共享时隐藏编辑相关控件

## 4. 加号弹窗与 Tab CRUD UI

- [x] 4.1 新建 `WorkbenchTabDialog`（或等价）：名称≤12、预设 manager/tester/developer/document、可见性（按角色显示）
- [x] 4.2 创建成功：调 POST → 刷新列表 → 选中新 tab → `editing=true`
- [x] 4.3 私有：本人可改名、删除（确认）；共享：管理员可改名、删除（确认）；工作台可删（本地隐藏）；可见仅剩 1 个 tab 时不可删；编辑态可改自定义 tab 名
- [x] 4.4 共享达 24：禁用/拦截选 project，并提示
- [x] 4.5 同组内拖拽改序（Sortable + `PUT .../tabs/sort`）；工作台/加号固定；不可跨共享/私有

## 5. 编辑与保存分流

- [x] 5.1 查看/编辑复用 `WorkbenchRows` / `WorkbenchRowEditor`；按当前 surface（work vs tabId）绑定 rows
- [x] 5.2 保存：work → PUT workbench；自定义 tab → PUT tab；共享无权限时不可进入编辑
- [x] 5.3 编辑工具条「套用预设」：manager/tester/developer/outsider/custom；确认弹窗后再替换 rows
- [x] 5.4 i18n：页签、弹窗、预设名、共享上限、套用确认、只读共享等文案

## 6. 验收与回写

- [x] 6.1 手工验收：首次角色种模板、加号私有/共享、共享只读隐藏编辑、删共享回工作台、本地选中记忆、组件库可拖原看板件、套用预设确认/取消
- [x] 6.2 回写长期知识：产品定位去双页签；接口清单补 tabs API；新增 ADR-021（取代 ADR-020 双页签条款）；版本记录一行；更新 ADR/总览列表；`tasks` 本组勾选
