# Git Smart HTTP 与在线浏览

说明平台内置 Git 托管的实现：bare 仓库落盘位置、`git clone/push` 走的 Smart HTTP 端点、Web 端在线浏览接口，以及与 SPA 路由的边界；改 Git 功能或部署持久化时参考。

## 概述

- 仓库用 JGit 以 **bare 仓库**形式存放在 `{cat2bug.profile}/{cat2bug.git.root}` 下（`cat2bug.git.root` 默认 `git`），**不依赖系统 git 命令**。
- 命令行客户端通过 **Smart HTTP**（`info/refs`、`git-upload-pack`、`git-receive-pack`）访问，使用平台账号 **HTTP Basic** 认证。
- Web 端在线浏览（树、文件、提交、分支、标签、贡献者、归档下载）走普通 REST 接口 `/system/git/repo/{repositoryId}/...`，同样由 JGit 读取 bare 仓库。

## 关键流程

### 磁盘路径

- 默认路径：`{git.root}/{teamKey}/{projectKey}/{slug}.git`，与 namespaced Clone URL 对齐；1.1 之前的旧规则是 `{git.root}/{projectId}/{slug}.git`，仅用于 slug 改名时判断是否需要迁移。
- `GitPathValidator` 负责 slug 格式（字母数字开头结尾，中间可含 `.`、`_`、`-`，1~100 位）、拒绝 `..` 路径穿越与系统关键目录、确保父目录可写；库里存的相对 `disk_path` 按进程工作目录解析成绝对路径。
- `JGitBareRepositoryHelper` 提供初始化、从远程克隆导入、移动、清空、校验 bare 仓库等操作。

### Smart HTTP

1. 两套 URL 并存：
   - 旧版：`/git/p/{projectId}/{slug}.git/...`
   - 命名空间版：`/{teamKey}/{projectKey}/code/{slug}.git/...`（路径键见 [[项目级History路由与路径键]]）
2. `JwtAuthFilter` 识别到 `GitSmartHttpPathSupport.matchesGitSecurityChain` 后放行，由 `GitSmartHttpResource` 自行认证：解析 `Authorization: Basic`，调用账号密码认证并构造 `LoginUser`；失败返回真实 HTTP 401 + `WWW-Authenticate: Basic realm="Cat2Bug Git"`，让 git 客户端弹出凭据输入。
3. 按路径找到仓库（`GitRepositoryLookup`），`git-upload-pack` 需 `canPull`，`git-receive-pack` 需 `canPush`，不通过返回 403；`canManage` 决定是否允许非快进推送（force push）。判定矩阵见 [[权限模型与鉴权链路#Git 仓库访问控制]]。
4. 请求体支持 `Content-Encoding: gzip`；没有 `Content-Length` 的分块请求体会先落到临时文件再交给 JGit。
5. `receive-pack` 成功后通过 PostReceiveHook 把推送事件交给 `GitWebhookEnqueueService` 投递 Webhook，Hook 失败只记日志不影响推送。

### 在线浏览

- `GitBrowseResource`（`/system/git/repo/{repositoryId}`）：`branches`、`tags`、`default-branch`、`tree`、`blob`、`raw`、`commit-count`、`commits`、`commits/{commitId}`、`contributors`、`archive`。读操作要求 `system:git:repo:pull`，写操作（建/删分支与标签、改默认分支）要求 `system:git:repo:manage`，服务层再做仓库级 `assertPull` / `assertManage`。
- 前端浏览页的 History 路径是 `/{teamKey}/{projectKey}/code/{slug}/**`（不带 `.git`），与 Smart HTTP 路径靠是否以 `.git` 结尾区分。

## 关键代码位置

- `cat2bug-platform-admin/src/main/java/com/cat2bug/admin/resource/GitSmartHttpResource.java`、`GitBrowseResource.java`、`GitRepositoryResource.java`、`GitPullRequestResource.java`、`GitReleaseResource.java`、`GitWebhookResource.java`
- `cat2bug-platform-framework/src/main/java/com/cat2bug/framework/security/git/GitSmartHttpPathSupport.java`
- `cat2bug-platform-system/src/main/java/com/cat2bug/system/service/git/`：`GitPathValidator`、`JGitBareRepositoryHelper`、`GitRepositoryLookup`、`GitWebhook*`
- `cat2bug-platform-system/src/main/java/com/cat2bug/system/git/browse/`：浏览服务与 DTO
- SPA 回退排除：`cat2bug-platform-admin/src/main/java/com/cat2bug/admin/config/EmbeddedSpaFallbackFilter.java`
- 配置：`cat2bug-platform-admin/src/main/resources/application.properties` 中 `cat2bug.git.*`

## 注意事项

- **部署必须持久化整个 `cat2bug.profile` 目录**（含 `git` 子目录），只挂载上传目录会导致数据库里有仓库记录、磁盘上仓库丢失。
- 修改 `teamKey` / `projectKey` / slug 会让旧的 namespaced Clone URL 失效（规格允许，不做别名跳转）。
- `cat2bug.git.webhook.secret-key` 在默认配置里有占位值，生产环境必须用环境变量覆盖。
- Native 镜像中 JGit 的 `WindowCache`、`WorkQueue` 等需推迟到运行期初始化，见 [[Native镜像与操作日志对等]]。
- Smart HTTP 的 401/403/404 是**真实 HTTP 状态码**，与普通 REST 接口「HTTP 200 + JSON code」的约定不同，这是 git 客户端要求的。
- 标签以裸仓库 `refs/tags/*` 为唯一真相，不镜像到数据库（否则会和 Smart HTTP 推送的标签漂移）；列表时按 `(repository_id, tag_name)` 关联 `sys_git_release` 补出 `releaseId`。页面新建的是附注标签（描述即 tag message），发行版自动补建的仍是轻量标签。
- 删除已关联 Release 的标签：不带 `cascadeRelease=true` 返回 409，前端确认后再带参数重试，服务端按 `deleteRelease` 同样的流程清理 Release 及附件后再删标签；单独删 Release 不删标签。平台内新建/删除标签会触发 `tag` 类型 Webhook。

## 相关规格

- `openspec/specs/git-smart-http/spec.md`、`openspec/specs/git-namespaced-clone-url/spec.md`
- `openspec/specs/git-repo-browse/spec.md`、`git-repo-branches`、`git-repo-releases`、`git-repo-contributors`、`git-repo-pull-requests`、`git-repo-webhooks`、`git-repo-import`、`git-repo-project-overview`（均在 `openspec/specs/` 下）
- `openspec/specs/git-repo-tags/spec.md`、`openspec/specs/quarkus-git-repositories/spec.md`

## 待确认

- 隐藏文档仓「保存即提交」（项目级 `cat2bug-docs` 文档仓、`SysDocumentGitContentStore` / `ISysDocumentGitRepositoryService`）：`CLAUDE.md` 有描述，但当前仓库代码与 `openspec/specs` 中均未找到对应实现或规格，是否已实现、在哪个分支，需确认后再补本节。
