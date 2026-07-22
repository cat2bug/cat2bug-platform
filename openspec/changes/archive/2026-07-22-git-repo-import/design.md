## Context

项目 Git 仓今日仅支持空白 bare 初始化（`JGitBareRepositoryHelper.initBareRepository` + `insertSysGitRepository`）。前端「新建」对话框与 `GitRepoSelect` 下拉「+」各自/即将各自打开创建表单，无导入能力。平台无 GitHub/GitLab/Gitee OAuth 集成。约束：全程 JGit、不 `exec git`；仓归属当前项目（`projectId` + slug）；manage 权限与现网一致。

## Goals / Non-Goals

**Goals:**

- 统一「新建/导入」对话框：空白 / URL / GitHub / GitLab / Gitee
- 一次性 mirror-clone 到本地 bare，成功后与远端脱钩
- 会话内 PAT 列仓与 clone；凭据不落库、不写配置
- GitLab 可填自建主机（默认 gitlab.com）
- 同步导入：成功前列表不可见；失败无半成品元数据/脏目录

**Non-Goals:**

- OAuth App / 持久 token / 持续 mirror
- 异步任务队列（第一期）
- 语言字段、SSH 私钥导入、LFS 专项优化

## Decisions

### D1. 统一对话框组件，双入口共用

- **选择**：抽取 `GitRepoCreateDialog`（或等价），列表「新建」与 `GitRepoSelect`「+」共用；Tab 切换模式。
- **替代**：入口拆成「新建」「导入」两个按钮 → 表单分叉、易漂移。
- **理由**：与产品决议一致，公共字段只维护一份。

### D2. 平台 Tab = PAT 列仓，非 OAuth

- **选择**：用户粘贴 PAT → 后端（或受控代理）调用平台 REST 列仓 → 选中后得到 `clone_url` → 导入 API 携带同一 token 做 HTTPS clone。
- **替代**：OAuth App → 需注册三家应用与回调，与「不存凭据」冲突更大。
- **理由**：满足「三家都做 + 不存」；公开仓允许无 token（受平台 API 限流约束）。

### D3. GitLab 主机可配置

- **选择**：GitLab Tab 字段 `host`（默认 `https://gitlab.com`），列仓与 clone 均基于该主机。
- **理由**：产品明确要求自建 GitLab。

### D4. 同步导入 + 成功后才落库可见

- **选择**：先在临时/目标路径完成 JGit bare clone；成功后再 `insert` 元数据；失败 wipe 目录且不插入（或事务回滚等价）。列表只读已提交元数据 → 成功前不可见。
- **替代**：先插 `importing` 再异步 → 需状态机与凭据短暂驻留队列。
- **理由**：与「不存凭据」「最简单可见性」一致；大仓可能超时 → 明确超时错误，用户可重试。

### D5. JGit bare clone / mirror

- **选择**：`Git.cloneRepository().setBare(true).setCloneAllBranches(true)`（或 mirror 等价），`CredentialsProvider` 仅绑定当次请求凭据。
- **理由**：与现有「只用 JGit」纪律一致。

### D6. API 形态

- **空白**：保留现有 `POST /system/git/repo`。
- **导入**：`POST /system/git/repo/import`（body：公共字段 + `sourceUrl` + 可选 `username`/`token`）。
- **列仓**：`POST /system/git/repo/remote/list`（或分 provider），body：`provider` + `token` + 可选 `host`；响应仅仓库摘要（name、fullPath、cloneUrl、defaultBranch），**不**持久化 token。
- **探活（可选）**：导入前 `ls-remote` 校验 URL/凭据，失败返回可读错误。

### D7. 默认分支

- **选择**：导入后默认分支优先取远端 HEAD / API 返回的 default_branch；表单可覆盖写入元数据（与空白新建字段对齐）。

### D8. 出站网络

- **选择**：服务端需能访问用户填写的 HTTPS 主机（GitHub/GitLab.com/Gitee/自建）；无 SSRF 白名单硬编码，但对 URL 做 scheme 限制（仅 https，可选允许 http 自建并文档提示）。
- **缓解**：拒绝非 http(s)、拒绝明显内网探测可后续加开关；第一期以 https + 主机可达为主。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 大仓同步超时 | 可配置超时；失败清理；提示缩小范围或稍后重试 |
| PAT 经后端转发 | TLS；日志/审计禁止打印 token；不写磁盘 |
| 自建 GitLab 证书问题 | 文档说明；必要时 JVM 信任配置（运维），不做随意 disable-verify 默认 |
| API 限流（无 token 公开搜索） | UI 引导填写 PAT；错误信息提示限流 |
| SSRF | 限制 scheme；后续可加 allowlist/块私网 |

## Migration Plan

- 无 DB schema 强制变更（若仅用现有 `sys_git_repository`）；无 Flyway 则跳过
- 纯功能增量；回滚 = 关闭导入 API/UI，空白新建不受影响
- 已有空白仓无数据迁移

## Open Questions

- 导入超时默认秒数（建议 120s，可配置 `cat2bug.git.import-timeout-seconds`）
- 是否允许 `http://` 自建主机（建议允许但 UI 警告）
- GitHub/Gitee 是否支持「组织」切换过滤（建议第一期：token 可见的全部仓 + 本地关键字过滤）
