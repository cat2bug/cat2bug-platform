# Git / 源码管理 parity checklist

| 能力 | Spring | Quarkus | 状态 |
|------|--------|---------|------|
| 仓库列表 `/system/git/repo/list` | ✅ | `GitRepositoryResource` | 已实现 |
| 仓库 CRUD / import / remote | ✅ | 同 | 已实现 |
| 概览 / 成员 / 关注 / 邀请 | ✅ | 同 | 已实现 |
| 浏览 branches/tags/tree/blob/commits | ✅ | `GitBrowseResource` | 已实现 |
| PR | ✅ | `GitPullRequestResource` | 已实现 |
| Releases | ✅ | `GitReleaseResource` | 已实现 |
| Webhooks | ✅ | `GitWebhookResource` | 已实现 |
| Smart HTTP | ✅ | `GitSmartHttpResource` | 已实现 |
| Flyway Git 表 V1_* | ✅ | `db/migration/V1_*` | 已拷贝 |
| JVM 冒烟 | — | 见 `SMOKE-RESULTS.md` | 进行中 / 人工验收 |

说明：`{repositoryId}` 与字面量 `/list` 在 JAX-RS 下偶发路由抢占，验收时若 list 异常可优先用数字 id 浏览路径验证。
