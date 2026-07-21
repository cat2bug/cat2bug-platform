## Context

仓库浏览（`git-repo-browse`）已支持分支、目录、blob、提交记录，右侧栏仅有简介与近期动态。Git Smart HTTP 与仓库元数据（`project-git-repository`）已落地，但无 GitHub 风格 Releases，也无按提交作者聚合的贡献者 API。用户决定：完整 Releases（含附件）、贡献者来自提交作者、发行版空态始终显示且仅 manage 可创建。

## Goals / Non-Goals

**Goals:**

- 完整 Releases：tag + 标题/说明 + 预发布/草稿 + 附件，一次交付
- 浏览页侧栏：发行版（含空态 CTA）、贡献者头像摘要
- 贡献者按 author name/email 聚合，匹配项目成员头像
- 权限与现有 git 矩阵一致：manage 写发行版，pull 读/下载

**Non-Goals:**

- 手工添加外部贡献者（后续源码扩展）
- CI/CD 自动发版、签名校验、多仓库批量发版
- 从远程镜像同步 GitHub Releases
- 替换 Smart HTTP 或改变 clone/push 鉴权

## Decisions

### 1. Releases 元数据落库，tag 仍在 Git

- **选择**：`sys_git_release`（及 `sys_git_release_asset`）存标题、说明、状态、发布人、时间、关联 tag 名与 commit id；Git 侧用 JGit 确保/创建 `refs/tags/<tag>`。
- **理由**：说明与附件不是 Git 对象；tag 必须可被 clone 使用。
- **备选**：仅 annotated tag message → 无法可靠存附件与草稿。

### 2. 附件走平台本地文件存储

- **选择**：复用 `FileUploadUtils` / `cat2bug.profile` 下仓库专属子目录（如 `git/releases/{repositoryId}/{releaseId}/`），DB 记相对路径、文件名、大小、content-type。
- **理由**：与现有上传一致，无新对象存储依赖。
- **备选**：附件塞进 Git LFS / 二进制 blob → 复杂且污染仓。

### 3. 创建权限 = manage；读/下载 = pull

- **选择**：创建/更新/删除/上传附件 → `project.create-by` | `project.admin`；列表/详情/下载 → 与 browse 相同的 pull 角色并集。
- **理由**：用户明确选择与建仓同级；避免任意 push 者发正式版。
- **草稿可见性**：草稿仅 manage 可见；已发布（含 pre-release）对有 pull 的用户可见。

### 4. 贡献者服务端聚合，侧栏可降级

- **选择**：`GET .../contributors` 在默认分支（或指定 ref）上遍历提交，按规范化 email（优先）/name 去重，返回 commitCount、author 字段，并尽量解析为平台 `userId`/头像 URL。
- **理由**：提交作者是真贡献；侧栏可复用结果前 N 条。大仓用上限（如扫描最近 N 提交或前 M 作者）控制成本。
- **备选**：仅前端从已加载 commits 凑 → 不准且无法支撑「全部」。

### 5. UI 形态

- **侧栏**：简介下 → 发行版 → 贡献者 → 近期动态。
- **发行版**：最新 1～3 条摘要；空态「暂无发行版，创建」；「创建」进抽屉/独立页（表单：tag、标题、说明、预发布、附件）。
- **贡献者**：标题 `贡献者 (n)` +「全部」；0 人隐藏整块。

### 6. API 草图（挂在现有 `/system/git/repo/{id}` 下）

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `.../releases` | pull |
| GET | `.../releases/{releaseId}` | pull（草稿需 manage） |
| POST | `.../releases` | manage |
| PUT | `.../releases/{releaseId}` | manage |
| DELETE | `.../releases/{releaseId}` | manage |
| POST | `.../releases/{releaseId}/assets` | manage |
| GET | `.../releases/{releaseId}/assets/{assetId}/download` | pull |
| DELETE | `.../releases/{releaseId}/assets/{assetId}` | manage |
| GET | `.../contributors?ref=&limit=` | pull |

## Risks / Trade-offs

- **[Risk] 大仓库贡献者全量扫描慢** → 限制扫描提交数/返回作者数；可后续加缓存表。
- **[Risk] 删除 Release 是否删 tag** → 默认只删 DB 与附件，保留 Git tag（避免误伤）；UI 注明。
- **[Risk] tag 名冲突** → 创建时若 tag 已存在则绑定现有 tip；若指向不同 commit 则拒绝或要求确认（实现选：拒绝并提示）。
- **[Risk] 附件磁盘膨胀** → 单文件大小上限 + 每 release 附件数上限（实现常量，可配置化后续）。

## Migration Plan

1. Flyway `V1_1_0_n__git_repo_releases.sql`（h2 + mysql 同步）建表。
2. 部署后端 API → 前端侧栏与创建流。
3. 回滚：停用 UI/API；表可保留；已上传附件留在 profile 目录（人工清理）。

## Open Questions

- 无（创建权限、附件范围、贡献者来源、空态策略已在探索中确认）。
