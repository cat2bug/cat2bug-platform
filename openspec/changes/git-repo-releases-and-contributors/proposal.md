## Why

仓库浏览页右侧栏目前只有简介与近期动态，缺少 GitHub 风格的「发行版」与「贡献者」入口。团队需要在平台内发布带说明与附件的正式版本，并按提交作者展示贡献者；现在既无 Releases 数据模型，也无侧栏呈现。

## What Changes

- 新增完整 **Releases** 能力：绑定 Git tag、标题、说明（Markdown）、可选预发布/草稿、附件上传与下载；创建/编辑/删除仅 `project.admin` / `project.create-by`（manage）；浏览与下载需 pull 权限。
- 仓库浏览页右侧「简介」下增加 **发行版** 区块：始终显示；无数据时展示「暂无发行版，创建」（有 manage 时可点创建）。
- 新增 **贡献者** 聚合：按提交作者（name/email）去重统计；尽量匹配项目成员头像；侧栏展示前 N 个头像与总数，「全部」进入完整列表；0 人时隐藏整块。外部/非成员贡献者手工挂载留作后续扩展，本 change 不做。
- 侧栏与列表/创建 UI 对齐现有 browse 页视觉；附件走平台本地文件存储。

## Capabilities

### New Capabilities

- `git-repo-releases`: 仓库发行版 CRUD、tag 绑定/创建、附件、权限与浏览 API/UI
- `git-repo-contributors`: 按提交作者聚合贡献者、侧栏摘要与完整列表 API/UI

### Modified Capabilities

- `git-repo-browse`: 浏览页右侧栏增加发行版与贡献者区块的展示与导航行为

## Impact

- 后端：`SysGitBrowse*` / 新 `SysGitRelease*`（或等价）服务与 Controller；Flyway（h2+mysql）发行版与附件表；JGit tag 读写；本地文件上传路径
- 前端：`browse.vue` 右侧栏；发行版列表/创建页或抽屉；贡献者列表；`gitRepo.js` API；i18n
- 权限：复用现有 project manage / pull 矩阵（与 `project-git-repository` / `git-smart-http` 一致）
- 依赖：现有 JGit bare 仓、`FileUploadUtils` / 本地 profile 存储
