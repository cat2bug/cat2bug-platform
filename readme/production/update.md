# 如何升级

Cat2Bug-Platform **1.0.0** 起，在已完成首次安装且检测到数据库结构待迁移时，启动后会**自动在浏览器中打开升级向导**（`/upgrade`），无需手工执行 SQL 或命令行迁移。

> **版本说明**：升级向导面向 **0.5.0 及以后**已部署的实例。系统 Flyway 基线版本为 `0.5.0`（`quarkus.flyway.baseline-version=0.5.0`），低于该基线的历史库需先通过 [首次安装向导](/admin-guide/install-wizard) 以「已有数据库」模式附着，或联系维护人员处理后再升级。

---

## 升级前准备

请在**维护窗口**内完成以下事项：

1. **备份数据**
   - **MySQL**：导出 `cat2bug_platform` 库（`mysqldump` 或运维平台快照）。
   - **H2**：复制 `./data` 目录下的 `cat2bug_platform.mv.db` 等文件。
   - **配置**：备份 `config/install/`（含 `application-install.yml`）。
   - **业务文件**：备份上传目录（`cat2bug.profile`）、日志目录（按需）。
2. **单实例运行**：升级向导**不支持多实例协调**，请确保同一时刻只有一个 Cat2Bug 进程在运行。
3. **替换程序**：停止旧版本，将 JAR、Native 二进制或 Docker 镜像替换为新版本后再启动。

---

## 何时会进入升级向导

| 场景 | 行为 |
| --- | --- |
| 已完成 install（`cat2bug.install.completed=true`）且新版本存在**未执行的 Flyway 脚本** | 浏览器自动进入 **`/upgrade`** |
| 无 install 文件的 legacy 数据 | 先走 **`/setup`** 附着旧库（`databaseMode: existing`），**不会**直接进入 `/upgrade` |
| 仅替换 JAR、无 pending migration、且 install 已完成 | 直接重启即可，通常**无需**进入向导 |
| 全新空库 | 走 **`/setup`** 首次安装，不会进入 `/upgrade` |

升级进行期间系统**全锁**：无法登录或访问业务 API，仅可访问升级相关接口与静态资源。

---

## 升级步骤（浏览器向导）

向导共 **4 步**：概览 → 管理员验证 → 执行升级 → 完成（失败时可重试）。

### 步骤 1：概览

展示当前版本、目标版本、数据库类型及**待执行迁移脚本**列表；可展开查看与 install 模板的配置差异（缺失项建议补全，已有不同值默认保留）。

![步骤 1：概览](../images/admin-guide/upgrade-step-1.png)

- 确认待执行迁移与维护窗口安排。
- 阅读单实例提示，确保无多余进程。

### 步骤 2：管理员验证

升级将修改数据库结构与系统配置，需输入**系统管理员**账号和密码确认身份（与登录凭据相同，须具备管理员权限）。

![步骤 2：管理员验证](../images/admin-guide/upgrade-step-2.png)

### 步骤 3：执行升级

确认是否启用**数据库备份**及备份文件名；若有缺失配置项，可选择「应用建议的配置补全」。点击「开始升级」后，系统将依次执行：**备份 → 写入配置 → 数据库迁移 → 重启**。

![步骤 3：执行升级](../images/admin-guide/upgrade-step-3.png)

- 默认**保留**已有 JDBC、路径等有效配置，模板仅用于补全缺失项。
- 若迁移失败，可在结果页查看 `lastError` 与失败步骤，修复后点击**重试**（按 `lastStep` 幂等续跑）。

### 步骤 4：完成

迁移成功后状态为 **`restart_required`**，应用会自动或提示**重启**；重启且 `upgradeRequired` 为 `false` 后，使用原管理员账号正常登录。

![步骤 4：完成](../images/admin-guide/upgrade-step-4.png)

---

## 跳过升级向导（Docker / 自动化）

设置环境变量 **`CAT2BUG_UPGRADE_SKIP=true`**（或配置 `cat2bug.upgrade.skip=true`）时：

- **无 install 的 legacy 实例**：启动时仍自动写出 completed install 文件，不进入 `/setup` 或 `/upgrade`。
- **install 已完成 + 有待执行 Flyway 脚本**：不进入 `/upgrade` UI，启动时由后台**静默执行 Flyway**；成功则正常提供服务，**失败则进程退出**（fail-fast，避免半升级状态）。

Docker Compose 示例见 `deploy/docker/README.md`。

---

## 相关文档

- [常见问题：如何升级系统版本？](/faq#q-如何升级系统版本)
- [系统安装向导](/admin-guide/install-wizard)（首次部署或 legacy 库附着）
- [快速开始](/quick-start)
