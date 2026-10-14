# 接口清单：Git 代码仓库

内置 Git 托管的仓库管理、代码浏览、PR、Release、Webhook 与 Smart HTTP（clone/push）接口速查；约定见 [[接口约定]]。

资源类目录：`cat2bug-platform-admin/src/main/java/com/cat2bug/admin/resource/`。下文 `{repositoryId}` 在代码中为 `{repositoryId:[0-9]+}`。权限简写：`list` = `system:git:repo:list`，`pull` = `system:git:repo:pull`，`push` = `system:git:repo:push`，`manage` = `system:git:repo:manage`；`A|B` 表示任一即可。

## 仓库与成员 `GitRepositoryResource`（`/system/git/repo`）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/system/git/repo/list` | 仓库列表 | `list\|pull` |
| GET | `/system/git/repo/{repositoryId}` | 仓库详情 | `list\|pull` |
| POST | `/system/git/repo` | 新建仓库 | `manage` |
| POST | `/system/git/repo/import` | 从远程导入仓库 | `manage` |
| POST | `/system/git/repo/remote/list` | 列出远程仓库（导入前） | `manage` |
| PUT | `/system/git/repo` | 编辑 | `manage` |
| DELETE | `/system/git/repo/{repositoryIds}` | 删除 | `manage` |
| GET | `/system/git/repo/project/{projectId}/overview` · `/members` | 项目代码概览 / 项目可选成员 | `list\|pull` |
| POST | `/system/git/repo/{repositoryId}/collect` | 收藏 | `list\|pull` |
| GET | `/system/git/repo/{repositoryId}/my-access` | 当前用户访问级别 | `list\|pull\|manage` |
| GET/POST | `/system/git/repo/{repositoryId}/members` | 仓库成员列表 / 添加 | `manage\|list\|pull` |
| PUT/DELETE | `/system/git/repo/{repositoryId}/members/{userId}` | 修改 / 移除成员 | `manage\|list\|pull` |
| POST | `/system/git/repo/{repositoryId}/members/from-repo` | 从其它仓库批量导入成员 | `manage\|list\|pull` |
| GET/POST | `/system/git/repo/{repositoryId}/invites` | 邀请列表 / 创建 | `manage\|list\|pull` |
| POST | `/system/git/repo/{repositoryId}/invites/{inviteId}/revoke` | 撤销邀请 | `manage\|list\|pull` |
| GET | `/system/git/repo/{repositoryId}/invite-sources[/{sourceRepositoryId}/members]` | 可导入成员的来源仓库及其成员 | `manage\|list\|pull` |

成员/邀请/Webhook 接口注解层放宽为 `manage|list|pull`，细粒度仓库角色应在服务层校验（具体规则「待确认」）。

## 代码浏览 `GitBrowseResource`（`/system/git/repo/{repositoryId}`）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `.../branches`、`.../tags` | 分支 / 标签列表 | `pull` |
| POST | `.../branches`、`.../tags` | 创建分支 / 标签 | `manage` |
| DELETE | `.../branches/{name}`、`.../tags/{name}` | 删除分支 / 标签；标签已关联 Release 时返回 409，须带 `?cascadeRelease=true` 才连同 Release 一起删除 | `manage` |
| PUT | `.../default-branch` | 设置默认分支 | `manage` |
| GET | `.../tree`、`.../blob`、`.../raw` | 目录树 / 文件内容 / 原始文件 | `pull` |
| GET | `.../commits`、`.../commits/{commitId}`、`.../commit-count` | 提交列表 / 详情 / 计数 | `pull` |
| GET | `.../search?q=&ref=&path=&limit=` | 代码文本检索（不区分大小写的字面量匹配，有文件数 / 大小 / 结果数上限） | `pull` |
| GET | `.../contributors` | 贡献者 | `pull` |
| GET | `.../archive` | 源码归档下载 | `pull` |

## Pull Request `GitPullRequestResource`（`.../pulls`）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `.../pulls` | PR 列表 | `pull` |
| POST | `.../pulls` | 创建 PR | `push` |
| GET | `.../pulls/{number}`、`/commits`、`/files`、`/comments` | 详情 / 提交 / 变更文件 / 评论 | `pull` |
| POST | `.../pulls/{number}/comments` | 发表评论 | `push` |
| PATCH | `.../pulls/{number}` | 关闭 PR | 方法上无 `@RequiresPermissions`（服务层校验「待确认」） |
| POST | `.../pulls/{number}/merge` | 合并 | `manage` |

## Release `GitReleaseResource`（`.../releases`）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `.../releases`、`.../releases/{releaseId}` | 列表 / 详情 | `pull` |
| POST/PUT/DELETE | `.../releases`、`.../releases/{releaseId}` | 新建 / 编辑 / 删除 | `manage` |
| GET/POST | `.../releases/{releaseId}/assets` | 附件列表 / 上传 | `pull` / `manage` |
| DELETE | `.../releases/{releaseId}/assets/{assetId}` | 删除附件 | `manage` |
| GET | `.../releases/{releaseId}/assets/{assetId}/download` | 下载附件 | `pull` |

## Webhook `GitWebhookResource`（`.../webhooks`）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET/POST | `.../webhooks` | 列表 / 创建 | `manage\|list\|pull` |
| GET/PUT/DELETE | `.../webhooks/{webhookId}` | 详情 / 编辑 / 删除 | `manage\|list\|pull` |
| POST | `.../webhooks/{webhookId}/secret/regenerate` | 重新生成签名密钥 | `manage\|list\|pull` |
| GET | `.../webhooks/{webhookId}/deliveries` | 投递记录 | `manage\|list\|pull` |
| POST | `.../webhooks/{webhookId}/test` | 测试投递 | `manage\|list\|pull` |

## Smart HTTP `GitSmartHttpResource`（`/`）

使用 HTTP Basic（平台账号），JWT 过滤器跳过这些路径，由资源类自行认证；两套 URL 等价。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/{teamKey}/{projectKey}/code/{slug}.git/info/refs` | 引用发现（命名空间 URL） |
| POST | `/{teamKey}/{projectKey}/code/{slug}.git/git-upload-pack` | clone / fetch |
| POST | `/{teamKey}/{projectKey}/code/{slug}.git/git-receive-pack` | push |
| GET/POST | `/git/p/{projectId}/{slug}.git/{info/refs\|git-upload-pack\|git-receive-pack}` | 旧版 URL（按项目 ID） |

- 开发模式前端 Vite 将同源 `/git/**` 与 `^/[^/]+/[^/]+/code/[^/]+\.git` 两类路径直接代理到后端（不加 `/dev-api`，超时设为 0），见 `cat2bug-platform-ui/vite.config.js`。
- 仓库文件落在 `{cat2bug.profile}/{cat2bug.git.root}`，须持久化。
