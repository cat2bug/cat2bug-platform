## Context

Cat2Bug 当前以项目为中心管理缺陷、用例、计划与文档，无 Git 托管。产品目标是在项目内提供多「工程」（bare 仓库），支持创建、clone/push 与 Web 浏览，服务端不依赖外部 `git` 进程或 Gitea 等旁路服务。

约束来自探索结论：

- 技术：JGit 进程内；日常交付与开发以 JVM 为准；Native 仅 Docker 打包事后兼容
- 权限：复用现有项目角色 `project.create-by` / `project.admin` / `project.develop` / `project.tester`；超管与团队管理员不绕过
- 认证：Git Smart HTTP 用平台登录账号（HTTP Basic），不另建 Token 体系（MVP）
- 存储：默认 `{cat2bug.profile}/git/{projectId}/{slug}.git`，亦允许 manage 角色配置绝对路径

利益相关方：项目管理员/创建人（生命周期）、开发/测试（读写代码）、平台维护者（磁盘与安全）。

## Goals / Non-Goals

**Goals:**

- 项目下 CRUD 多个代码工程（元数据 + bare 仓）
- Smart HTTP：有权限用户可用系统 `git` 客户端 clone/fetch/push
- Web/API 浏览：分支、目录、文件内容、简单 commit log
- 角色矩阵落地到 REST 与 Git 协议两条路径
- slug 可改；绝对路径可配（带校验）

**Non-Goals:**

- Merge Request / Code Review、CI、LFS、SSH
- Git 侧 Issue；缺陷↔commit 自动关联
- 独立 Git Access Token / Deploy Key（可后续）
- 以 Native 编译通过作为 MVP 验收条件
- 嵌外部托管进程或 `ProcessBuilder("git")` 服务端

## Decisions

### D1 — 服务端用 JGit，不用系统 git / 外部 Gitea

- **选择**：`org.eclipse.jgit` 完成 init bare、对象读写、Smart HTTP。
- **理由**：满足「不依赖外部进程」；与单 JAR/JVM 部署一致。
- **备选**：`exec git`（违背约束）；嵌入 Gitea（多进程运维）。

### D2 — 领域模型：项目 1–N 工程

- **选择**：表如 `sys_git_repository`（名称待定）：`project_id`、`name`、`slug`、`disk_path`、`default_branch`、审计字段；磁盘为 bare repo。
- **理由**：与「一项目多工程」一致；与现有 `SysModule`（业务模块树）分离，避免语义混淆。
- **默认路径**：未指定绝对路径时使用 `{profile}/git/{projectId}/{slug}.git`。

### D3 — 权限映射到现有 role_key

| 能力 | 角色 |
|------|------|
| manage（创建/删/改 slug/改路径/允许 force-push） | `project.create-by`, `project.admin` |
| pull + push | 上表 + `project.develop`, `project.tester` |
| 其他项目成员 / 仅团队管理员 / 仅系统超管 | 拒绝（除非同时持有上述项目角色） |

- 一人多角色：**并集**。
- REST 用权限字（如 `system:git:repo:manage` / `push` / `pull`）挂到上述角色；Smart HTTP 在认证后按同一规则判定 `upload-pack` / `receive-pack`，force-push 仅 manage。

### D4 — Git 认证复用平台登录（HTTP Basic）

- **选择**：`/git/**` 走 Basic；用户名/密码经现有登录校验（哈希比对）；成功后加载该用户在目标工程所属项目的角色。
- **理由**：用户要求复用登录、MVP 不另建 Token。
- **说明**：浏览器 Session/JWT 不用于 `git` CLI；管理与浏览 REST 仍用现有会话认证。
- **备选**：Personal Access Token（推迟）。

### D5 — slug 可改；旧 URL 不保证兼容

- **选择**：允许更新 slug；同步迁移/重命名磁盘目录（或更新 `disk_path`）；UI 明确提示旧 clone URL 失效。
- **理由**：产品要求可改；MVP 不做永久别名/redirect。

### D6 — 绝对路径允许，但必须校验

- **选择**：manage 可填绝对路径；服务端规范化（解析 `..`）、拒绝明显危险目标策略（实现时定义最小黑名单，如不可写根盘符号链接穿透等）、校验目录可创建/可写。
- **理由**：用户要求可手填；完全不校验风险过高。

### D7 — Native 非门闩

- **选择**：功能在 JVM 验收；Docker Native 后续冒烟；若失败可文档声明 Git 模块需 JVM。
- **理由**：产品日常路径为 JAR；Native 仅部分交付场景。

### D8 — 协议与 API 分层

```
浏览器 ──Session/JWT──▶ /system/git/**     管理 + 浏览
git CLI ──HTTP Basic──▶ /git/{...}.git/**  Smart HTTP
                              │
                              ▼
                           JGit + disk
```

## Risks / Trade-offs

- [绝对路径写穿] → 规范化 + 可写校验 + 审计日志；文档警告仅信任管理员
- [改 slug 打断本地 remote] → UI 强提示；可选后续别名
- [Basic 传密码] → 强制 HTTPS 部署建议；与「复用登录」的权衡；后续可加 Token
- [JGit ≠ 完整 git 生态] → 不支持 LFS/部分 hook；文档写明边界
- [Native + JGit 反射] → 不阻塞 MVP；需要时用 GRM / agent 补 metadata
- [force-push 破坏历史] → 仅 manage；默认拒绝非快进，除非 manage

## Migration Plan

1. Flyway（h2 + mysql 同步）增加工程表与菜单/权限种子（挂到角色 6/7/8/11）
2. 引入 JGit 依赖与服务；配置 git 根路径配置项
3. 前端工程管理与浏览页；文档说明 clone URL 与 Basic 用法
4. 回滚：关闭菜单/权限；保留磁盘数据或按运维策略清理；DB 表可留待下一版本 drop

## Open Questions

- clone URL 对外路径最终形态（例如 `/git/p/{projectId}/{slug}.git` vs 含项目短码）——实现时选一种并写进 API 文档
- 默认分支名（`main` vs `master`）——建议 `main`，创建空仓时可不建首 commit，由首次 push 决定
- 绝对路径黑名单的具体列表——实现阶段在 design 附录或代码常量中落地最小集
