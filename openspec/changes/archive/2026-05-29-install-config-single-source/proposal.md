## Why

首次安装向导已实现 `config/install/application-install.yml` 与 `application-h2.yml` / `application-mysql.yml` 三层叠加，连接信息与 Druid 连接池等大量重复；`InstallService.isInstalled()` 以「install 文件是否存在」判定，与「磁盘 install 仅由向导创建」的目标冲突。需要将运行期基础设施配置收敛为 **单一外部真源**（`application-install.yml`），删除 database profile 文件，并统一开发/生产首次启动均走向导。

## What Changes

- **BREAKING**：删除 `application-h2.yml`、`application-mysql.yml`；移除 `spring.profiles.active: ${spring.database-type}`
- 在 JAR 内新增 classpath 模板 `defaults/application-install-h2.yml`、`defaults/application-install-mysql.yml`（完整 datasource/druid/redis/h2 等），**不**提交磁盘 `config/install/application-install.yml`
- 未安装且磁盘无 install 文件时：从 classpath **H2 模板**注入内存引导配置（不落盘），保证 Setup API 可启动
- 安装向导提交时：`SetupConfigWriter` 按用户选择合并模板，写入完整 `application-install.yml`，并设置 `cat2bug.install.completed: true`
- **BREAKING**：`isInstalled()` 改为以 `cat2bug.install.completed == true`（或 `CAT2BUG_INSTALL_SKIP`）判定，不再以 install 文件是否存在判定
- Legacy 实例：启动时若磁盘无 install 且检测到已有 schema/管理员，自动从当前 Environment 导出并写入 install 文件（过渡版本可仍加载 profile 一版）
- `.gitignore` 忽略 `config/install/application-install.yml`；仓库保留 `config/install/.gitkeep`
- 更新部署文档：切换 MySQL / 改连接仅编辑 `application-install.yml`

## Capabilities

### New Capabilities

- `install-config-single-source`：classpath 安装模板、内存引导、完整 install 写入、install 完成标记、Legacy 自动迁移、移除 database profile

### Modified Capabilities

- `first-run-setup-wizard`：安装状态检测改为 `cat2bug.install.completed`；首次启动（含开发环境）必须经向导创建磁盘 install；持久化内容包含完整基础设施配置

## Impact

- **配置**：`application.yml`、`application-h2.yml`（删）、`application-mysql.yml`（删）、新增 `defaults/application-install-*.yml`、`.gitignore`
- **common**：`InstallStartupSupport`、`InstallEnvironmentPostProcessor`、`InstallConfigSupport`、`InstallProperties`
- **framework**：`InstallService`、`InstallServiceTest`、新增 `InstallConfigMigrationRunner`（或等价）
- **admin**：`SetupConfigWriter`、可能调整 `SetupInstallService`
- **文档**：`readme/production/faq.md`、安装测试清单
- **部署**：Docker 需挂载带 `completed: true` 的 install 或 `CAT2BUG_INSTALL_SKIP`
