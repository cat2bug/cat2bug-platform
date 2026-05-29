## 1. Classpath 模板与 Git

- [x] 1.1 从现有 `application-h2.yml` / `application-mysql.yml` 提取并新增 `defaults/application-install-h2.yml`、`defaults/application-install-mysql.yml`（含完整 druid/redis/h2）
- [x] 1.2 新增 `InstallTemplateLoader`（或等价工具）从 classpath 加载模板为 `Map` 供引导与 Writer 使用
- [x] 1.3 添加 `config/install/.gitkeep`；`.gitignore` 忽略 `config/install/application-install.yml`；从 Git 移除已跟踪的 install 文件

## 2. 安装判定与引导

- [x] 2.1 `InstallProperties` / `InstallConfigSupport` 增加读取磁盘 install 中 `cat2bug.install.completed` 的方法
- [x] 2.2 修改 `InstallService.isInstalled()`：仅以 `completed==true` 或 skip 为准；更新 `InstallServiceTest`
- [x] 2.3 重构 `InstallEnvironmentPostProcessor` / `InstallStartupSupport`：无磁盘 install 时注入 H2 模板 PropertySource（不落盘）；保留 `bootstrap-mode` 与 CLI 参数过滤
- [x] 2.4 确认 `application.yml` 保留 `spring.config.import` optional install；移除 `spring.profiles.active: ${spring.database-type}`（Phase C 可与删 profile 同批）

## 3. 向导写入与迁移

- [x] 3.1 重构 `SetupConfigWriter`：基于 classpath 模板合并向导输入，写入完整 install YAML + `cat2bug.install.completed: true`
- [x] 3.2 新增 `InstallConfigMigrationRunner`：无磁盘 install 且 Legacy/Flyway 已存在时，从 Environment 导出并写入 install（`completed: true`）
- [x] 3.3 调整 `SetupController` / `GET /setup/status` 与前端 `setup-status`（若需）以匹配新 `installed` 语义
- [x] 3.4 为 `SetupConfigWriter`、模板加载、MigrationRunner 添加单元测试

## 4. 删除 profile（Phase C）

- [x] 4.1 删除 `application-h2.yml`、`application-mysql.yml`
- [x] 4.2 清理依赖 profile 的注释与死代码；验证 Flyway `spring.database-type`、PageHelper 仍从 install/bootstrap 解析

## 5. 文档与验证

- [x] 5.1 更新 `readme/production/faq.md`：仅通过 `application-install.yml` 切换数据库
- [x] 5.2 更新 `openspec/changes/first-run-setup-wizard/TESTING.md` 或新增本变更测试清单（fresh clone、向导写 install、Legacy 迁移、删 install 重进向导）
- [x] 5.3 手工验证：fresh clone → `/setup` → submit → 重启；Legacy 实例升级自动生成 install
