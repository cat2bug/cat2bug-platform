# TESTING — project-git-repositories

JVM 验收清单（Native Docker 冒烟见文末，**非 MVP 门闩**）。

## 前置

- 应用以 JVM 启动（H2 或 MySQL）
- Flyway 已执行 `V1_0_3_1__git_repository`
- 配置：`cat2bug.profile` 可写；`cat2bug.git.root` 默认 `git`（落盘 `{profile}/git/...`）
- 准备账号（同一项目内）：
  - 项目管理员 / 创建人（`project.admin` / `project.create-by`）
  - 开发 / 测试（`project.develop` / `project.tester`）
  - 仅团队管理员或系统超管、且**无**上述项目角色的账号（用于否定用例）

## 权限矩阵

| 操作 | create-by / admin | develop / tester | 仅超管/团队管理员（无项目角色） |
|------|-------------------|------------------|--------------------------------|
| 创建/删/改 slug/改路径 | 允许 | 拒绝 | 拒绝 |
| 列表 / Web 浏览 | 允许 | 允许 | 拒绝 |
| `git clone` / fetch | 允许 | 允许 | 拒绝 |
| `git push`（快进） | 允许 | 允许 | 拒绝 |
| `git push --force` | 允许 | 拒绝 | 拒绝 |

## 管理与浏览（JWT 会话）

1. 管理员创建两个不同 slug 的仓库 → 列表可见两条；默认路径在 `{profile}/git/{projectId}/{slug}.git`
2. 可选：填写可写绝对路径创建 → 成功；非法路径（如 `/`、含 `..`）→ 拒绝
3. 改 slug → API/UI 提示旧 clone URL 失效；新 URL 可用
4. 删除仓库（wipe disk）→ 元数据与磁盘 bare 仓移除
5. 开发账号：可见列表与浏览，无创建/删除按钮；浏览 branches/tree/blob/commits 正常
6. 无项目角色的超管调用 REST → 403

## Smart HTTP（HTTP Basic = 平台登录账号）

Clone URL 形态：`http(s)://{host}/git/p/{projectId}/{slug}.git`

```bash
# 开发账号 clone
git clone http://develop:PASSWORD@127.0.0.1:2020/git/p/{projectId}/{slug}.git

# 推送（快进）；推送人由 HTTP Basic 识别，不强制 commit author 与账号一致
cd {slug}
echo test >> README.md && git add . && git commit -m t && git push

# 非管理员 force-push 应失败
git push --force

# 管理员 force-push 允许（可选）
```

否定：错误密码 → 401；无角色成员 → 403/拒绝 pack。

## 进程约束

- Smart HTTP / 建仓路径不得 `ProcessBuilder`/`Runtime.exec` 调用系统 `git`
- 可用代码检索或单测守卫确认

## 绝对路径安全（实现注释 + 本清单）

- 路径规范化（解析 `.` / `..`）
- 可写性校验
- 最小拒绝集（如 Unix `/`、`/etc`、`/System` 等）— 以代码常量为准

## Native / Docker（可选后续）

- MVP **不要求** Native 镜像通过 Git 冒烟
- 若 Docker Native 构建后 JGit/反射失败：文档声明「Git 模块需 JVM JAR 运行」即可，不阻塞本 change 归档
