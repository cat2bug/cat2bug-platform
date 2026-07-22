## Why

项目内已支持「空白新建」bare 仓库，但团队代码往往已在 GitHub / GitLab / Gitee 或任意可 clone 的远端。缺少一次性导入时，用户只能本地 clone 再 push，摩擦大。需要在统一的新建对话框内支持从远端一次性搬迁到本平台托管仓。

## What Changes

- 将「新建仓库」统一为带模式切换的对话框（列表页「新建」与源码库下拉「+」共用）：**空白仓库** / **从 URL 导入** / **GitHub** / **GitLab** / **Gitee**
- **一次性**导入：服务端用 JGit 将远端 mirror/clone 为本地 bare 仓后与远端脱钩；不持续同步、不保留 upstream 配置义务
- 凭据**不落库、不进配置**：平台 Tab 用会话内 PAT 列仓；导入请求体可带 token，请求结束后丢弃
- GitLab Tab 支持可填**主机地址**（默认 `https://gitlab.com`，兼容自建）
- **同步模型**：clone 成功并完成元数据落库前，列表不可见；失败不留半成品库记录与脏目录（或等价清理）
- 公共字段仍归属当前项目：名称、slug、可见性（`public` / `project`）、默认分支、备注；不做「选择语言」、不做 OAuth App 绑定

## Capabilities

### New Capabilities

- `git-repo-import`: 从 URL / GitHub / GitLab / Gitee 一次性导入为项目下托管 bare 仓；含会话内 PAT 列仓、可配 GitLab 主机、同步导入与失败清理、统一新建对话框模式切换

### Modified Capabilities

- `project-git-repository`: 创建入口改为统一多模式对话框；空白新建行为保持，并与导入模式共用名称/slug/可见性等字段与 manage 权限

## Impact

- **后端**：在现有 `SysGitRepository` / JGit bare 助手上扩展 clone/mirror 与导入 API；可选「探活」远端；凭据仅请求作用域；超时策略与失败回滚
- **前端**：抽取共用「新建/导入」对话框；列表与 `GitRepoSelect` 共用；GitHub/GitLab/Gitee 列仓 UI；i18n
- **依赖**：继续仅用 JGit（不 `exec git`）；出站 HTTPS 访问用户填写的主机（含自建 GitLab）
- **非目标**：OAuth App、持续 mirror、凭据持久化、异步导入队列、语言字段、SSH 私钥导入
