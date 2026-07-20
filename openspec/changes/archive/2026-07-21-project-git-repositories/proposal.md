## Why

Cat2Bug 已覆盖缺陷、用例、计划与文档，但项目内缺少自托管的代码仓库能力。中小团队希望在同一平台内完成「项目 → 多工程 → clone/push → 浏览器查看」，而不依赖外部 GitLab/Gitea 进程或本机 `git` 服务端。

## What Changes

- 在项目下新增「代码工程」资源：一个 `SysProject` 可挂多个 Git bare 仓库
- 服务端用 **JGit**（进程内）提供创建仓库、Smart HTTP（clone/fetch/push）与 Web 文件浏览；不 `exec git`、不嵌入外部托管进程
- Git HTTP 认证复用平台登录账号（HTTP Basic + 现有用户校验），再按项目角色授权
- 权限：`project.create-by` / `project.admin` / `project.develop` / `project.tester` 可 pull 与 push；仅创建人与项目管理员可管理工程（创建/删除/改 slug/配置磁盘路径/允许 force-push）；团队管理员与系统超管**不**因身份绕过；一人多角色取权限并集
- Git 侧只管代码；不引入 MR、CI、Git Issue；缺陷仍走现有缺陷模块
- 工程 slug 创建后允许修改（旧 clone URL 失效，UI 提示）；磁盘路径允许手填绝对路径（须规范化与可写校验）
- 实现与验收以 **JVM** 为主；Spring Native 仅作为 Docker 交付时的事后兼容检查，不当 MVP 门闩

## Capabilities

### New Capabilities

- `project-git-repository`: 项目下多工程的元数据生命周期、磁盘 bare 仓、slug/路径管理与菜单/API
- `git-smart-http`: JGit Smart HTTP（upload-pack / receive-pack）、Basic 复用登录与按角色的 pull/push/force-push
- `git-repo-browse`: Web/API 只读浏览分支、目录树、文件内容与简单 commit 历史

### Modified Capabilities

- （无）现有 `openspec/specs/` 能力的需求级变更；本 change 为净新增能力

## Impact

- **后端**：新增工程领域模型与 Flyway（h2/mysql）；JGit 依赖；Smart HTTP 与浏览 REST；Spring Security 对 `/git/**` 的 Basic 通道；项目角色鉴权（不绕过超管）
- **前端**：项目内代码工程列表/管理、clone URL、文件浏览页；改 slug 时的 URL 失效提示
- **存储**：默认落在 `cat2bug.profile` 下，或管理员配置的绝对路径
- **交付**：日常 JVM；Native 镜像后续冒烟，失败时可文档标明 Git 需 JVM（非本 MVP 阻塞）
- **非目标**：MR/Code Review、CI、LFS、SSH、缺陷↔commit 自动关联（可后续 change）
