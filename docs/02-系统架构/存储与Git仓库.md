# 存储与 Git 仓库

本篇说明 `cat2bug.profile` 目录下文件与 Git bare 仓库的落盘布局，排查“附件/仓库丢失”问题时先看这里。

## cat2bug.profile

- 配置项 `cat2bug.profile`，默认 `./upload`（相对工作目录）。实际值以安装后的运行期配置为准；Docker Compose 通过 `CAT2BUG_PROFILE=/home/uploadPath` 与卷挂载点对齐（见 [[部署]]）。
- 解析入口：`Cat2BugProfileConfig.profileRoot()`（转绝对路径并规范化）；`resolveUploadPath` 拒绝越出 profile 根的路径。
- 对外 URL 前缀为 `/profile`（`FileUploadService.RESOURCE_PREFIX`）。
- 本地缺文件时回源：配置 `cat2bug.profile-remote-base`（站点根；私有端连云库时一般是 `https://www.cat2bug.com`，不是 `cloud.cat2bug.com`），由 `ProfileResource` 拉同一 `/profile/...` 路径。**不要**写成 `cat2bug.profile.remote-base`——与标量 `cat2bug.profile` 同前缀时 SmallRye 读不到，图片会全 404。

## 目录结构

```
{cat2bug.profile}/
├── upload/yyyy/MM/dd/{名称}_{时间戳}.{扩展名}   # 附件、头像等上传文件
├── download/                                   # 导出/下载临时文件
└── git/                                        # cat2bug.git.root，默认 git
    ├── {teamKey}/{projectKey}/{slug}.git       # 源码 bare 仓（1.1 起默认）
    ├── {projectId}/{slug}.git                  # 1.1 之前的旧路径，slug 改名时迁移
    └── releases/{repositoryId}/{releaseId}/    # 发行版附件
```

- 上传：`FileUploadService` 写入 `upload/` 下的日期目录，返回 `/profile/upload/...` URL。头像上传（`UserProfileResource` 的 `/avatar`）同样走这里，没有单独的 avatar 目录。
- 下载：`FileDownloadHelper` 读 `download/`。
- Git 根：`Cat2BugConfig.getGitRootPath()` = `{profile}/{cat2bug.git.root}`。
- 发行版附件：`GitReleaseStoragePaths`，相对路径写入 `sys_git_release_asset.storage_path`。
- 安装配置（`config/install/`）与 H2 数据（`data/`）不在 profile 下，而在工作目录下，同样需要持久化，见 [[部署]]。

## 源码仓

- 元数据表 `sys_git_repository`：`project_id` + `slug` 在项目内唯一，`disk_path` 存 bare 仓绝对路径，`default_branch` 默认 `main`。
- 路径规则与校验：`GitPathValidator`。它校验 slug 格式（字母数字开头结尾，中间可含 `. _ -`，长度 1~100），拒绝 `..` 和系统关键目录，并检查父目录可写。
- 读写库用 JGit（`JGitBareRepositoryHelper`）；浏览、PR、发行版分别在 `cat2bug-platform-system` 的 `git/browse`、`git/pull`、`git/release` 包。
- 客户端 clone/push 走 Git Smart HTTP（`GitSmartHttpResource`，HTTP Basic 认证），URL 见 [[整体架构]] 认证一节。
- 导入远程仓超时：`cat2bug.git.import-timeout-seconds`（默认 120）。Webhook 签名密钥配置项为 `cat2bug.git.webhook.secret-key`，生产须覆盖。

## 隐藏文档仓（待确认）

- `CLAUDE.md` 描述：每个项目一个隐藏文档仓（slug `cat2bug-docs`），与源码仓同在 git 根下、以 `kind` 字段区分，由 `SysDocumentGitContentStore` / `ISysDocumentGitRepositoryService` 承载 Markdown 正文和“保存即提交”的历史。
- **核实结果**：当前代码（以及删除前的 Spring 线）和迁移脚本中都找不到 `kind` 列、`cat2bug-docs`，也找不到上述两个类。`sys_document` 仍用 `file_url` + `file_version` 字段存文档。该特性疑似未合入或已回退，**待确认**。

## 相关笔记

[[整体架构]] · [[数据库]] · [[部署]]

返回 [[说明]]
