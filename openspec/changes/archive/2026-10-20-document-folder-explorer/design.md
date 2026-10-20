## Context

文档页（`views/system/document/index.vue`）当前是单栏列表：用 `docPid` / `currentFolder` 表示当前位置，列表内点文件夹或「上级目录」行切换；移动对话框里已有懒加载文件夹 `el-tree`（`docType=0` + `leaf`）。列表侧已有 `handleSelectionChange` / `handleDelete(ids)` 半成品，但默认列多半未挂 `type=selection`，批删入口也未露出。

缺陷列表（`defect/list/table.vue`）已有可抄的壳：`Multipane` + 左侧树显隐本地键 `defect_tree_module_visible`、拖宽缓存。交付物树数据源是 module API，**不可直接复用 TreeModule 业务**，只抄交互与布局。

删除路径：前端 `doc.is-delete-dir` 文案承诺级联，后端 `deleteSysDocumentByDocIds` 仅按 id 删除，子孙会成孤儿。批删前必须修齐。

产品已拍板：右侧列表仍显示子文件夹；树搜全项目；删前确认且级联；表/树双向同步高亮；pageSize 全局本地键；保留「上级目录」行。

## Goals / Non-Goals

**Goals:**

- 文档页左侧常驻文件夹树（可显隐、状态本地恢复），树顶全项目模糊搜，节点旁新建文件夹。
- 点树 / 点列表夹 / 「上级」三线导航同步 `docPid` 与树高亮；右侧仍列出当前夹下的文件夹与文件。
- 多选 + 批移（复用移动对话框）+ 批删（确认 + 级联）。
- pageSize 全局本地记忆；刷新先读本地再 **一次** `list`（不要「读本地 → list → 再改 pageSize → 再 list」）。
- 文件夹删除与文案一致：真正删除子孙文档/文件夹记录。

**Non-Goals:**

- 不改造 Markdown 编辑页 / 评审纪要模板壳。
- 不按项目隔离 pageSize；不缓存「上次打开的文件夹」为硬性需求（可选增强，首版可不做）。
- 不把 TreeModule 改成通用组件；不引入新后端「树专用」资源，除非全量夹过多再议。
- 不改文档权限标识串。

## Decisions

### D1：新建 `DocumentFolderTree`，文档页套 multipane

- **选择**：新组件承载树 UI（搜索框、懒/全量树、节点旁「+」、显隐由父控制）；`index.vue` 用与缺陷类似的 `Multipane` 左右布局。
- **理由**：交付物与文档语义不同；复制交互比硬抽象 TreeModule 便宜。
- **备选**：通用化 TreeModule——首版不做。

### D2：树数据——无搜索懒加载，有搜索则全量夹本地滤

- **选择**：
  - 无关键字：按父节点 `listDocument({ docType: 0, docPid, pageSize: 足够大 })` 懒加载（与移动对话框同模式）。
  - 有关键字：一次（或缓存）拉取项目下全部文件夹 `docType=0` 且 **不传 docPid**（现有 WHERE 在 `docPid==null` 时不加 pid 条件），前端建树并按 `docName` 模糊过滤，展开到命中节点。
- **理由**：不新开搜索 API；文件夹量通常远小于文件。
- **备选**：仅返回命中 + 祖链的专用接口——文件夹上万时再开。

### D3：导航单一事实源 `currentFolder` / `docPid`

- **选择**：`applyFolder(folder)` 为唯一写入口；树 `@node-click`、列表 `goFolder`、「上级」都走它，再 `getList`，并 `setCurrentKey` / 展开路径同步树。
- **理由**：避免树、列表各自改 query 导致双 list 或高亮漂移。
- **备选**：树与列表各自 state + watch 同步——易抖，不采用。

### D4：本地键

| 键 | 作用域 | 说明 |
|----|--------|------|
| `document_folder_tree_visible` | 全局 | 左树显隐，仿缺陷 |
| `document.pageSize` | 全局 | 分页每页条数 |

- 树宽可选用 session/local（仿 `defect_tree_module_width`），非阻塞。
- **hydrate**：`created`/`beforeMount` 读 pageSize 与显隐 → 赋 `queryParams.pageSize` → 再 `getList`；改 pageSize 时落盘并查 list，避免「默认 10 list 完再换成缓存值再 list」。

### D5：多选与批处理

- **选择**：`Cat2BugTable` 增加 selection 列；工具条在「新建文件夹」前放「批量移动」「批量删除」（无选中时禁用）。批移打开现有移动对话框，目标夹选定后对 `ids` 批量 `updateDocument`（或现有移动逻辑扩展）。批删走确认后 `delDocument(ids)`。
- **理由**：半成品已在；对齐产品工具条顺序。
- **备选**：行内菜单批处理——不符合「新建文件夹前」要求。

### D6：级联删除在服务端完成

- **选择**：`SysDocumentService.deleteSysDocumentByDocIds`（或专用方法）对每个待删 id：若为文件夹（`docType=0`），递归收集子孙再一并删除；文件只删自身。事务内执行。前端确认文案可沿用/微调批删条数。
- **理由**：文案已承诺；只靠前端拼 id 易漏、并发不安全。
- **备选**：仅前端 BFS 再传超长 id 列表——不作为主方案。
- **注意**：正文在每项目隐藏 Git 仓（ADR-007）；若现网单文件删除已有清附件/Git 逻辑，级联时对每个文件文档走同等清理；若仅删元数据，保持与现单删一致，不在本次扩大 Git GC 范围。

### D7：右侧列表仍含文件夹 + 「上级」行

- **选择**：保持现有 `getList` 后注入 `docType=-1` 上级行的行为；树上选中夹时列表第一行仍可回到父级。
- **理由**：产品明确保留；与树并存形成三线导航。

## Risks / Trade-offs

- [全量夹一次拉取过大] → Mitigation：首版 pageSize=9999；若项目夹数异常大，后续加「命中+祖链」接口。搜索防抖，避免每键一请求。
- [级联删误伤] → Mitigation：强制确认；文案标明含子孙；可在确认中展示「将删除 N 个文件夹 / M 个文件」（可选增强）。
- [树与列表双 list] → Mitigation：导航只经 `applyFolder`；树懒加载请求与右侧 list 分离，搜索全量夹不触发文件 list。
- [pageSize hydrate 双 list] → Mitigation：先读缓存再首次 `getList`；对照 `defect-tab-list-fetch` 抑制旁路模式。
- [批移循环 N 次 update] → Mitigation：可接受；夹数量通常小。若需原子性再加批量 move API（非首版）。

## Migration Plan

- 无 DB schema 变更；部署后旧数据删除行为变为真正级联（与文案对齐，属行为修复）。
- 回滚：还原前端树壳 + 还原服务端删除为按 id（回滚后文案与行为再次不一致，需知悉）。

## Open Questions

- 无阻塞。可选：是否在确认框展示级联统计——实现时若成本低则加，否则沿用现有单夹文案扩展为「已选 N 项」。

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/04-UI规范/组件与交互约定.md`（回写文档页左侧树 / pageSize 本地约定）
- `docs/05-接口文档/接口清单-报告与文档.md`（回写删除级联语义）
- `docs/07-技术决策/ADR-007-文档正文存每项目隐藏Git仓库.md`（级联删时与单删同等处理正文，不新开 ADR）
- `docs/07-技术决策/说明.md`（本次不新增 ADR）
