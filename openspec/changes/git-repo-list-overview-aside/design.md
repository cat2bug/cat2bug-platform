## Context

项目源码管理列表在 `cat2bug-platform-ui/src/views/system/git/repo/index.vue`，当前为单列：关注区、搜索/排序、仓库列表。无右侧概览。

已有能力可复用：

- 仓列表与 `sys_user_git_repository` 关注（当前用户维度 join）
- `sys_git_pull_request`（`state = 'open'`）
- 项目成员 `GET /system/project/{projectId}/member`
- JGit 浏览默认分支树（`git-repo-browse`）

产品已拍板：项目范围；三格统计（仓库 / PR / 关注）；无 Fork；语言按扩展名轻量扫；窄屏隐藏侧栏。

## Goals / Non-Goals

**Goals:**

- 列表页宽屏右侧概览：统计三格、语言条、成员预览
- 项目级 overview API：仓数、open PR 数、关注合计、语言分布
- 关注切换后侧栏关注数即时一致
- 窄屏隐藏侧栏（断点约 `1200px`）

**Non-Goals:**

- Fork、GitHub 公开 star、跨项目全局统计
- linguist 完整规则 / vendor 精细排除（仅轻量扩展名 + 简单目录跳过如 `node_modules`、`.git`）
- 语言结果持久化缓存表（可后续 change）
- 成员 CRUD（只读展示项目成员）

## Decisions

### D1. 单一 overview 接口

- `GET /system/git/repo/project/{projectId}/overview`
- 响应大致：
  ```json
  {
    "repoCount": 17,
    "openPrCount": 0,
    "collectCount": 362,
    "languages": [{ "name": "Java", "percent": 30 }, ...]
  }
  ```
- 成员不强制塞进 overview：前端用现有 `listMemberOfProject`，避免重复权限与分页语义；侧栏展示前若干成员 + 总数标题「成员 (n)」
- **鉴权**：与列表一致——调用者须能列出该项目下至少可读的 Git 仓（项目 Git 读/pull 权限）；无权限则 403
- **备选否决**：前端 N 次按仓拉 PR/树再聚合——首屏慢、逻辑散

### D2. 统计口径

| 字段 | 口径 |
|------|------|
| `repoCount` | 当前用户在该项目下**有权看到**的仓数量（与列表可见集一致） |
| `openPrCount` | 上述可见仓集合中 `state='open'` 的 PR 行数 |
| `collectCount` | 上述可见仓在 `sys_user_git_repository` 中 `collect=1`（或等价真值）的**行数**（跨用户合计，非去重用户数） |

列表「仅我的关注」筛选不影响侧栏口径：侧栏始终是项目概览（可见仓全集），不随 keyword/sort 变。

### D3. 语言：同步 + 上限 + 文件数加权

- 对每个可见仓打开 bare、解析默认分支（无默认分支则跳过）
- 遍历树中**文件路径**（非 blob 内容），按扩展名映射到语言标签（内置小表：`.java`→Java，`.js`→JavaScript，`.ts`→TypeScript，`.vue`→Vue，`.css`/`.scss`→CSS，`.sh`→Shell，Dockerfile→Docker，`.sql`→SQL/PLpgSQL 等；未知归「其他」）
- **加权**：按文件个数（非字节），实现简单、扫描成本低
- **上限**（实现常量，可配置）：每仓最多计入 N 个文件（如 5000）；项目合计最多 M 个文件（如 20000）；路径段跳过 `node_modules`、`vendor`、`dist`、`build`、`.git`
- 合并后按占比降序，取 Top 9 + 剩余并入「其他」；百分比整数（或一位小数），总和约 100%（末项调平）
- 单仓失败（盘损、空仓）：跳过该仓，不导致整个 overview 失败
- **备选否决**：字节加权（需读 blob 大小，更慢）；后台缓存表（MVP 过重）

### D4. 前端布局

```
.git-repo-page
  .git-repo-main     ← 现有列表
  .git-repo-aside    ← 新侧栏（≥1200px）
```

- 三格卡片：仓库 / PR / 关注（文案用「关注」非 Star）
- 语言：名称 + 绿色进度条 + 百分比
- 成员：头像 + 昵称网格；点击可进项目成员页（若已有路由）或仅展示
- `@media (max-width: 1199px) { .git-repo-aside { display: none } }`
- 关注切换：`collect` API 成功后本地 `collectCount ± 1`，或轻量重拉 overview 统计字段

### D5. i18n

- 新增键：侧栏标题段、仓库/PR/关注、仓库语言、其他、成员；多语言文件与现有 git.repo.* 风格一致

## Risks / Trade-offs

- [大仓同步扫拖慢 overview] → 每仓/项目文件上限 + 跳过常见目录；语言区块可独立 loading，统计先出
- [扩展名误判（如 `.h` 多语言）] → 接受轻量误差；文档/注释标明「按扩展名估算」
- [可见集 vs 项目全量] → 与列表权限一致，避免侧栏泄露无权限仓的 PR/语言；关注合计也只计可见仓
- [百分比四舍五入和不等于 100] → 末项或「其他」调平

## Migration Plan

- 无 Flyway；纯 API + UI
- 回滚：隐藏侧栏组件与路由调用即可，无数据迁移

## Open Questions

- （已定）窄屏断点采用 `1200px`；实现时若与全站其它双栏不一致，可对齐现有常量
- 成员区块是否链到项目「成员」菜单：实现时有路由则链，无则只读展示
