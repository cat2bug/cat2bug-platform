## Context

报告详情由 `ViewReport.vue` 以 90% `el-drawer` 挂在报告列表页上打开，正文为 `reportDescription` 经 markdown-it + Cat2Bug 插件渲染为 HTML。现有 `sys_comment` 仅服务缺陷日志（`moduleType=defect_log`），字段只有内容与关联 id，无父评论、无锚点。报告模板编辑器（Milkdown）不在本次范围。

约束：有权限成员可改正文 → 锚点可能失效；产品选择保留全部批注并标记 orphaned，由创建人或项目管理员 / 团队管理员手动删除；无通知 / @。

## Goals / Non-Goals

**Goals:**
- 报告详情三栏：目录、正文、评论
- TOC 来自渲染后标题，点击滚动并支持 scroll-spy
- 整行 / 选中文字批注 + 扁平回复
- quote + context 锚点；匹配失败显示「原文已变更」
- 删除鉴权：创建人、项目管理员、团队管理员
- Word/PDF 导出仍只导出正文

**Non-Goals:**
- 模板编辑器内批注
- 通知、@、嵌套楼中楼
- 独立全屏详情路由（MVP 仍用 drawer 三栏；窄屏折叠侧栏）
- Google Docs 级实时协同光标 / 精确字符级 CRDT 锚点

## Decisions

### 1. 仍改 `ViewReport` drawer，不做新路由（MVP）
- **选择**：在现有 90% drawer 内做三栏壳，目录/评论可折叠。
- **替代**：独立 `/project/report/:id` 全页——空间更好，但改动路由/菜单更深。
- **理由**：复用列表深链 `?reportId=`、导出与关注人逻辑；后续若空间不够可再升全页。

### 2. 数据模型：扩展 `sys_comment` + 锚点字段（而非全新表）
- **选择**：`moduleType=report`，`correlationId=reportId`；新增：
  - `parent_id`（NULL = 主评；非空 = 扁平回复挂主评）
  - `anchor_kind`：`line` | `selection`
  - `quote`、`context_before`、`context_after`（原文快照）
  - `anchor_hint`（可选：最近标题 id / 块指纹，仅辅助）
  - `resolve_status`：可不落库，由前端匹配结果即时计算；或落库 `orphaned` 缓存（MVP 推荐**前端计算**，避免改正文后批量写库）
- **替代**：独立 `sys_report_annotation` 表——隔离更干净，但要复制作者/删除/列表能力。
- **理由**：复用现有 Comment API / UI；注意 `module_type` 现为 `varchar(12)`，`report` 足够；`parent_id` 对 `defect_log` 保持 NULL 即可兼容。

### 3. 锚点策略：quote + context，正文变更不自动删
- 创建时截取选区 / 整行文本为 `quote`，并取前后上下文。
- 打开报告时在正文纯文本中搜索；命中则高亮并双向定位；未命中则线程标「原文已变更」，仍可读写回复。
- **不做**自动删除或强制「解决」流；删除仅手动。

### 4. 「整行」定义
- **选择**：用户点击行旁批注入口，或未选文字时对当前块级元素（`p` / `li` / `tr` / 标题等）取 `textContent` 作为 line quote。
- **替代**：仅 markdown 源码行号——渲染后插件/表格会错位。
- **理由**：与渲染 DOM 一致，覆盖普通文字与表格行。

### 5. 删除权限
- 后端删除接口校验：`createById == 当前用户` **或** 当前用户为该报告所属项目的项目管理员 **或** 所属团队的团队管理员（对齐现有 `SysRole.isProjectAdmin()` / 团队角色判定）。
- 删主评时级联删除其扁平回复（或禁止有回复时删主评——MVP 选**级联删除**，避免孤儿回复）。
- 回复作者可删自己的回复；项目管理员（roleId=6）/ 团队管理员（roleId=4，`team.admin`）/ 团队创建人（roleId=12）可删任意回复。

### 6. 前端结构
```
ViewReport drawer
├── header（既有）
└── body.report-detail-shell
    ├── aside.toc（可折叠）
    ├── main#report-data（markdown-it + 选区浮条 + 高亮）
    └── aside.comments（线程列表 + 输入）
```
- TOC：渲染后扫描 `h1–h6`，写入稳定 `id`，IntersectionObserver 做 scroll-spy。
- 导出：继续只序列化 `#report-data` 内正文节点（排除浮条/高亮控件）。

### 7. 权限与创建
- 能打开报告详情的成员即可创建批注 / 回复（与现报告查看权限一致）。
- 改正文权限沿用现有报告编辑权限，与批注解耦。

## Risks / Trade-offs

- [重复 quote 误匹配] → 用 context_before/after 消歧；多候选时取首次并允许用户从右侧手动聚焦线程
- [正文大改导致大量 orphaned] → UI 明确标记；管理员可批量删（手动逐条即可，MVP 不做批量）
- [扩展 sys_comment 影响缺陷评论] → 新列可空；缺陷路径不读写新字段
- [drawer 三栏窄屏拥挤] → 默认折叠一侧；断点下 TOC/评论改为抽屉叠层
- [module_type 长度] → 使用 `report`；若未来更长名字需迁移加宽列

## Migration Plan

1. Flyway（h2 + mysql）：`sys_comment` 增加 `parent_id`、`anchor_kind`、`quote`、`context_before`、`context_after`、`anchor_hint`；必要时加宽 `module_type`
2. 后端 Domain / Mapper / Service 支持新字段与删除鉴权
3. 前端上线 `ViewReport` 三栏；旧报告无批注时右侧为空态
4. 回滚：前端回退单栏；新列可留存（兼容旧代码忽略）

## Open Questions

- 项目管理员 / 团队管理员在报告所属项目上的具体角色 key 以实现时与 `SecurityUtils` / 成员角色查询对齐（实现阶段对照现有缺陷删除鉴权）
- 无标题报告的 TOC 空态文案与是否展示「正文」占位项（实现时可默认空态提示「暂无标题」）
