# ADR-006 Git 仓库用 JGit 自托管与 Smart HTTP

记录项目源码仓库为什么用内嵌 JGit，而不是系统 git 或外部 Gitea。

- 日期：2026-07-21
- 状态：已采纳（权限部分被 [[ADR-008-系统管理员对Git仓库全权限]] 修订）

## 背景

需要「一项目多工程」的源码仓库：网页浏览、git CLI clone/push，且不依赖外部进程，符合单 JAR / 单二进制部署。依据：`openspec/changes/archive/2026-07-21-project-git-repositories/design.md`。

## 候选方案

### 方案 A：`org.eclipse.jgit` 内嵌

优点：不依赖外部进程；与单 JAR / Native 部署一致。

缺点：不是完整 git 生态（不支持 LFS、部分 hook）；Native 需补反射元数据。

### 方案 B：调用系统 `git`（exec）

缺点：违背「不依赖外部进程」约束，否决。

### 方案 C：嵌入 Gitea

缺点：多进程运维，否决。

## 最终决定

- JGit 完成 init bare、对象读写、Smart HTTP。
- 表 `sys_git_repository`（`project_id`、`name`、`slug`、`disk_path`、`default_branch` 等），磁盘为 bare 仓，与业务模块 `SysModule` 分离。
- 分层：浏览器经 Session/JWT 走 `/system/git/**`（管理 + 浏览）；git CLI 经 HTTP Basic 走 `/git/{...}.git/**`（Smart HTTP），Basic 复用平台登录，暂不做 Personal Access Token。
- 权限映射项目角色：manage = `project.create-by` / `project.admin`；pull+push 另含 `project.develop` / `project.tester`；多角色取并集；force-push 仅 manage。
- slug 可改，旧 clone URL 不保证兼容；仓库可填绝对路径但须规范化与可写校验。
- Native 不作门闩（后来 Quarkus Native 已注册 JGit 枚举反射并冒烟通过）。

## 原因

满足「不依赖外部进程」与单文件交付；复用现有登录与角色体系，MVP 成本最低。

## 副作用

- Basic 认证传密码，部署需 HTTPS。
- 仓库落在 `{cat2bug.profile}/{cat2bug.git.root}`，必须持久化整个 `cat2bug.profile`（Docker 默认挂载）。
- 开发代理需转发 `/git`，大仓 push 要关闭代理超时（`cat2bug-platform-ui/vite.config.js`）。
