# 安装配置单一真源 — 手工测试清单

本文档用于验证 `install-config-single-source` 变更的端到端行为：磁盘 `application-install.yml` 为运行期基础设施配置的唯一真源；不再依赖 `application-h2.yml` / `application-mysql.yml` 或 `spring.profiles.active`。

建议在干净环境（无磁盘 install 文件、必要时清空 H2 数据目录或空 MySQL 库）下执行。

## 前置条件

- JDK 17+
- 后端：`cd cat2bug-platform-admin && mvn spring-boot:run`
- 前端（开发模式）：`cd cat2bug-platform-ui && npm run dev`，访问 `http://localhost:2222`
- 或嵌入式前端：先 `npm run build:embedded` 再启动 JAR
- 确认仓库中**未**提交 `config/install/application-install.yml`（应在 `.gitignore` 中）

## 1. 全新克隆：无磁盘 install，进入向导

- [ ] 全新 `git clone` 后，确认 `config/install/application-install.yml` **不存在**（可有 `config/install/.gitkeep`）
- [ ] 删除本地 `./data/cat2bug_platform.mv.db`（若曾启动过）或使用空 MySQL 库
- [ ] 启动应用，访问 `/` 或 `/login`，应自动重定向到 `/setup`
- [ ] 直接访问 `/setup` 可打开 7 步向导
- [ ] 启动过程中**不应**在磁盘上自动生成 `application-install.yml`（引导阶段仅内存注入 H2 classpath 模板）
- [ ] 未安装时访问业务 API（如 `/prod-api/system/user/list`）应被拦截（403 或等价错误）

## 2. 向导提交：写入完整 install 与完成标记

- [ ] 完成向导全部步骤并提交，显示成功页及**重启提示**
- [ ] 生成 `./config/install/application-install.yml`
- [ ] 文件中 `cat2bug.install.completed` 为 `true`
- [ ] 文件包含**完整**基础设施配置（不仅 URL/账号）：`spring.database-type`、`spring.datasource`（含 Druid 连接池参数）、`cat2bug.cache.type`、存储/日志路径等，与所选 H2/MySQL、local/redis 一致
- [ ] `cat2bug.cache.type` 与 `spring.database-type` 相互独立（例如 H2 + redis 合法）

## 3. 重启后仅依赖 install 文件

- [ ] **重启**应用（修改 install 后必须重启才生效）
- [ ] 重启后可用安装时设置的管理员账号登录
- [ ] `GET /prod-api/setup/status` 返回已安装
- [ ] 访问 `/setup` 应被拒绝或重定向
- [ ] 确认启动**不**需要 `application-h2.yml` / `application-mysql.yml` 及 `spring.profiles.active`（profile 文件已从 classpath 移除后仍能正常启动）
- [ ] 业务 API 与登录页正常可用

## 4. Legacy 升级：自动生成 install

适用于从旧版本升级、数据库中**已有** schema/管理员（如 `sys_user` 存在 `admin`，或 Flyway 已有成功迁移），但磁盘上**无** `application-install.yml` 的实例。

- [ ] 使用含 legacy 数据的库启动，**不应**被重定向到 `/setup`
- [ ] 首次启动后自动生成 `config/install/application-install.yml`，且 `cat2bug.install.completed: true`
- [ ] 生成的 install 内容反映当前有效 Environment（如 MySQL 连接、缓存类型），而非盲用默认模板
- [ ] 用户可照常 `/login` 并使用原有 admin 账号
- [ ] 访问 `/setup` 应被拒绝

验证方式示例：

1. 准备含 admin 用户或已迁移 schema 的库，确保无磁盘 install 文件
2. 启动应用，确认浏览器打开 `/login` 而非 `/setup`
3. 检查磁盘已生成 `application-install.yml` 且 `completed: true`

## 5. 删除 install 文件：重新进入向导

- [ ] 停止应用，删除 `./config/install/application-install.yml`
- [ ] **注意**：若数据库仍含已安装数据（admin 用户、Flyway 历史），Legacy 迁移可能再次自动生成 install 并视为已安装；要验证「重进向导」，通常需配合**空库**（删除 H2 文件或新建空 MySQL 库）
- [ ] 在空库 + 无 install 条件下启动，应再次重定向到 `/setup`
- [ ] 重新提交向导可再次生成 install 并完成安装

## 6. Docker：`CAT2BUG_INSTALL_SKIP` 与挂载 install

- [ ] 容器设置 `CAT2BUG_INSTALL_SKIP=true`，并挂载含 `cat2bug.install.completed: true` 的 `application-install.yml`，启动后**不**进入 `/setup`，行为与已安装实例一致
- [ ] 挂载的 install 中 datasource/redis 等配置被正确加载
- [ ] 仅设置 `CAT2BUG_INSTALL_SKIP=true` 但**无**磁盘 install 且无足够环境变量预配置时，行为符合设计文档（可能无法完成引导，需挂载 install 或提供属性覆盖）

## 7. 切换 MySQL（已安装）

- [ ] 编辑 `config/install/application-install.yml`：将 `spring.database-type` 改为 `mysql`，填写 `spring.datasource.druid.master` 等连接信息
- [ ] **不要**修改 `application.yml` 中的 profile 或已删除的 `application-mysql.yml`
- [ ] 重启后应用连接 MySQL 且 Flyway 使用 `db/migration/mysql` 路径

## 8. 后端单元测试

```bash
mvn test -pl cat2bug-platform-framework -Dtest=InstallServiceTest
mvn test -pl cat2bug-platform-admin -Dtest=SetupConfigWriterTest,InstallTemplateLoaderTest,InstallConfigMigrationRunnerTest
```

- [ ] `InstallServiceTest`：仅以 `cat2bug.install.completed == true` 或 skip 判定已安装；不以 install 文件是否存在判定
- [ ] `SetupConfigWriterTest` / 模板加载：向导输出包含 Druid 池等完整字段
- [ ] `InstallConfigMigrationRunnerTest`：legacy 条件满足时写出 completed install

## 9. 与首次安装向导回归

完整向导步骤（连接测试、验证码、注册策略等）见 `openspec/changes/first-run-setup-wizard/TESTING.md` 第 2–6 节；本变更不改变向导 UI，但安装判定与持久化语义已更新，建议在两份清单均勾选关键项。
