## Context

- 三期已落地 v3 行排版与双页签「工作台 | 看板」（见 `openspec/specs/workbench-row-layout`、ADR-020）。看板用 `tabs.board.widgets` 勾选，与工作台 `rows` 分裂。
- 探索结论：去掉看板；固定「工作台」+ 加号自定义页签（私有/项目共享）；交互对齐缺陷页加号；原看板组件进组件库。
- 现状：个人布局 `sys_user_workbench` + `GET/PUT /system/workbench/{projectId}`；壳 `dashboard/index.vue`；预设 `presets.js`（tester/developer/manager/blank）；角色映射几乎仅 develop→developer。
- 缺陷页参考：`defect/index.vue` + `DefectTabDialog.vue` + `sys_project_defect_tabs`（仅私有筛选，无共享）——交互可抄，存储与权限不可照搬。
- **本变更推翻 ADR-020「保留工作台 | 看板」条款**；行排版 / 24 栅格决策仍有效。

## Goals / Non-Goals

**Goals:**

- 壳层：固定「工作台」+ 共享 tab（≤24）+ 私有 tab（不限）+ 加号；顺序固定；去掉看板。
- 加号创建：名≤12、预设（管理/测试/开发/文档）、可见性（仅 create-by/admin 可见选项）；创建后进编辑。
- 「工作台」个人布局：不可删改名；可编辑；首次按角色种管理/测试/开发/外部人员；编辑可套用五套预设（含自定义空白）且先确认。
- 共享只读：普通成员隐藏编辑；管理员可编/删/改名；私有本人可编/删/改名；可见性创建后不可改。
- 本地记住选中 tab；失效回工作台。
- 存储：工作台仍一人一份；自定义 tab 新表 + API。

**Non-Goals:**

- 页签拖拽排序：仅同可见性组内（共享组 / 私有组各自排序）；「工作台」与加号固定；跨组不可拖。
- 不把共享布局做成「每人一份副本」。
- 不恢复看板页签；不做 `tabs.board` → rows 自动折叠迁移。
- 不实现跨项目工作台首页。
- 不改缺陷页签模型。

## Decisions

### D1. 存储拆分（方案 C）

| 数据 | 存哪 |
|------|------|
| 固定「工作台」个人 layout | 现有 `sys_user_workbench.config_json`（v3，**仅** `tabs.work.rows` + `preset`；读入时剥离 `tabs.board`） |
| 自定义 tab 元数据 + layout | 新表（建议名 `sys_project_workbench_tabs`）：`tab_id`, `project_id`, `owner_user_id`, `visibility` (`private`\|`project`), `tab_name`, `preset`, `config_json`（`{ version:3, rows:[...] }`）, `tab_sort`, 时间戳 |

- 列表 API：返回当前用户可见集合 = 全部 `visibility=project` ∪ 本人 `private`。
- 共享创建：事务内 `COUNT(project + project)` &lt; 24，否则拒绝。
- **备选（否）**：全部塞进 `sys_user_workbench` → 共享不可见；或每人拷贝共享 → 编辑语义混乱。

### D2. API 形状

- 保留：`GET/PUT /system/workbench/{projectId}` → 仅个人工作台。
- 新增（路径示意，实现可微调）：
  - `GET /system/workbench/{projectId}/tabs` → 可见自定义 tab 列表（不含固定工作台；layout 可按需懒加载或随列表返回）
  - `POST .../tabs` → 创建（body：name, preset, visibility?）
  - `PUT .../tabs/{tabId}` → 更新 name 和/或 config_json（按权限）
  - `DELETE .../tabs/{tabId}`
  - `PUT .../tabs/sort` → 同组改序（body：`[{tabId, tabSort}, ...]`，不可跨 visibility）
- 鉴权：
  - 私有：owner == 当前用户
  - 共享写/删/改名/排序：当前用户在项目内含 `project.create-by` 或 `project.admin`
  - 共享读：项目成员即可
  - POST 时非管理员强制 `visibility=private`（忽略客户端传 project）

### D3. 页签壳与本地选中

```
[工作台] [共享…] [私有…] [+]
   ↑固定 id=work
```

- 加号：`el-tab-pane` 占位 + `@click.stop` 开弹窗，对齐缺陷 `__defect_add_tab__`。
- 本地键：`workbench-tab:{projectId}`，值 `work` 或 `tabId`；切换时写入；挂载时校验仍存在，否则 `work`。
- 创建成功：写缓存 → `activeTab=新id` → `editing=true`。

### D4. 权限与 UI

- 前端用项目成员角色（与 Git/成员页同源：`project.create-by` / `project.admin`）控制：加号弹窗「显示类型」、共享 tab 是否显示编辑/删/改名。
- 普通成员打开共享 tab：**不渲染**编辑按钮与改名/删除入口。
- 「工作台」始终可编辑（个人布局），无共享概念。

### D5. 预设两套入口

| 入口 | 可选 |
|------|------|
| 首次种工作台 / 角色默认 | `manager` / `tester` / `developer` / `outsider` |
| 加号创建 | `manager` / `tester` / `developer` / `document`（空白行 + 文档类组件） |
| 编辑「套用预设」 | `manager` / `tester` / `developer` / `outsider` / `custom`（空白可手搭）；**确认后**整表替换当前 tab 的 rows |

角色映射（多角色优先级）：

`create-by|admin` → manager → `develop|developer` → developer → `tester` → tester → 其余（含 `project.outsider`）→ outsider。

### D6. 去掉看板与组件库

- 壳删除 `board` pane；registry 去掉 `tabAffinity: 'board'` 限制（或视为 `both`），统计/图表进入工作台组件库。
- 预设不再输出 `tabs.board`；`alignLegacyConfig` 忽略 board。

### D7. 行编辑器复用

- 固定工作台与自定义 tab **共用** `WorkbenchRowEditor` / `WorkbenchRows`；保存时按当前 tab 分流 PUT workbench vs PUT tab。
- 编辑器已接受 tab id 的形状继续沿用；自定义 tab 的 config 根级为 `{ version, rows }`（可不嵌 `tabs.work`），或统一包一层由前端适配——实现时选一种并在 tasks 写死，**推荐**自定义 tab 存 `{ version:3, preset, rows }`，避免无意义的 `tabs.work` 嵌套。

### D8. 拟新增 ADR

- 标题建议：`ADR-021-工作台固定页与自定义私有共享页签`（取代 ADR-020 双页签条款；保留 v3/24 栅格引用）。

## Risks / Trade-offs

- [共享并发超 24] → 创建接口原子计数校验；前端达上限禁用「项目共享」选项并提示。
- [管理员误删共享] → 删除二次确认；客户端缓存失效回工作台。
- [角色接口缺失导致全员看不到共享选项] → 复用成员/项目详情已有角色查询；失败时默认按非管理员（仅私有）。
- [旧 board 配置残留] → 读入剥离，不报错；用户需自行从组件库拖回统计件。
- [私有不限导致页签条过长] → 接受产品选择；后续可加折叠/更多菜单（非本变更）。

## Migration Plan

1. Flyway 建自定义 tab 表。
2. 发布前端：无看板；加载 tabs API；旧 config 剥离 board。
3. 回滚：恢复双页签前端 + 忽略新表（新 tab 数据保留不删）。
4. 文档：产品定位、接口清单、ADR-021、版本记录。

## Open Questions

- 同组内 tab 序：`tab_sort desc, create_time desc`；拖拽后写 `PUT .../tabs/sort`。

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/01-产品需求/产品定位与核心功能.md`
- `docs/03-技术方案/权限模型与鉴权链路.md`（`project.create-by` / `project.admin` / `project.outsider`）
- `docs/05-接口文档/接口清单-系统与监控.md`（`/system/workbench`）
- `docs/07-技术决策/ADR-020-工作台v3行排版与24栅格.md`（本变更部分取代）
- `docs/07-技术决策/ADR-021-工作台固定页与自定义私有共享页签.md`
- `docs/08-版本记录/2026-10.md`
