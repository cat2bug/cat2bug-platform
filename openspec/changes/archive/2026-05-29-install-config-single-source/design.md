## Context

当前启动链：

```
application.yml
  spring.profiles.active → application-h2.yml | application-mysql.yml
  spring.config.import   → optional application-install.yml（部分覆盖）
```

`SetupConfigWriter` 仅写入 `druid.master` 连接信息；Druid 池参数仍依赖 profile。`InstallService.isInstalled()` 以 install 文件存在为准，与「开发首次也走向导、磁盘 install 仅向导创建」冲突。

已归档 `first-run-setup-wizard` 建立向导与 install 导入机制；本变更在其上收敛配置真源并删除 profile。

## Goals / Non-Goals

**Goals:**

- 运行期基础设施配置单一真源：`config/install/application-install.yml`
- JAR 内 classpath 模板（h2/mysql 完整骨架），供引导、向导写入、Legacy 迁移
- 未安装时内存引导 H2（不写磁盘），向导提交后写完整 install + `cat2bug.install.completed: true`
- 删除 `application-h2.yml`、`application-mysql.yml` 及 profile 绑定
- Legacy 自动导出 install；`.gitignore` 磁盘 install

**Non-Goals:**

- 修改向导 UI 步骤或验证码/注册业务规则
- 将 `application.yml` 中 server/mybatis/flyway 等应用级默认迁入 install
- 无重启切换数据库类型
- 提供 `application-install.yml.example` 或预置可运行磁盘 install

## Decisions

### 1. Classpath 模板 vs 磁盘 install

**决策**：模板位于 `classpath:defaults/application-install-h2.yml` 与 `application-install-mysql.yml`；磁盘 `config/install/application-install.yml` 仅由向导或 Legacy 迁移创建，加入 `.gitignore`。

**理由**：开发与生产首次启动一致，均进入 `/setup`；避免 clone 即误判已安装。

**备选**：仓库提交默认 install 文件 —— 与「首次走向导」冲突，不采用。

### 2. 未安装时的数据源

**决策**：`InstallEnvironmentPostProcessor`（或等价）在「无磁盘 install 且非 skip」时，将 **H2  classpath 模板** 解析为 `MapPropertySource` 注入 `Environment`（`addFirst`），并设置 `cat2bug.install.bootstrap-mode=true`。不写入磁盘。

**理由**：向导提交前必须能连库跑 Flyway/Setup API；与现有 bootstrap 强制 H2 一致，但数据源来自模板而非 profile 文件。

**备选**：继续保留 `application-h2.yml` profile —— 造成重复，不采用。

### 3. 安装完成判定

**决策**：`InstallService.isInstalled()` 当且仅当：

- `CAT2BUG_INSTALL_SKIP` / `cat2bug.install.skip=true`，或
- 磁盘 install 文件中 `cat2bug.install.completed: true`

**不再**使用「install 文件是否存在」。

**理由**：磁盘文件可能在向导前不存在；完成后必须有显式标记。

**备选**：仅以 `sys_config.cat2bug.install.completed` —— 删 install 文件后无法重进向导，不采用为主判定。

### 4. SetupConfigWriter 输出结构

**决策**：提交时按 `databaseType` + `cacheType` 加载对应 classpath 模板，合并用户输入（连接、路径、redis、ai），写入完整 YAML，并设置：

```yaml
cat2bug:
  install:
    completed: true
```

**理由**：用户要求 Druid 池等全部在 install 内，删除 profile 后 install 必须自包含。

### 5. Legacy 迁移

**决策**：`ApplicationRunner`（`InstallConfigMigrationRunner`）在上下文就绪后：

- 若磁盘 install **不存在**
- 且 `hasLegacyInstallation()` 或 Flyway 已有成功迁移
- 则从当前 `Environment` 导出相关属性树，写入 install，并设 `completed: true`

**过渡**：首个包含本变更的版本 **仍保留** profile 文件一版，确保升级实例 Environment 含真实 MySQL 连接；随后版本删除 profile。

**备选**：按 `application.yml` 的 `database-type` 选模板盲写 —— 丢失用户自定义连接，不采用。

### 6. 移除 profile 机制

**决策**：删除 `spring.profiles.active: ${spring.database-type}`；`spring.database-type` 仅来自 install 或 bootstrap 内存属性。Flyway `locations: classpath:db/migration/${spring.database-type}` 保持不变。

### 7. Git 与仓库布局

**决策**：

- 新增 `config/install/.gitkeep`
- `.gitignore`：`config/install/application-install.yml`
- 移除已跟踪的 `config/install/application-install.yml`（若存在）

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 直接升级且跳过过渡版、无 install、已删 profile | 文档要求经过渡版或手动从模板生成 install；迁移 Runner 在过渡版从 Environment 导出 |
| 删 install 文件后启动失败 | 文档说明：删文件后重新走引导 H2 + 向导；可选从 classpath 恢复 |
| install 文件变大 | 可接受；单一真源 |
| Docker skip 无 install | 挂载 install 或 env 覆盖；`CAT2BUG_INSTALL_SKIP` 行为不变 |
| `SetupConfigWriter` 与模板漂移 | 单测对比模板字段；Writer 以模板为基合并 |

## Migration Plan

1. **Phase A**：classpath 模板、`completed` 判定、Writer 全量写入、bootstrap 读模板；**仍保留** profile
2. **Phase B**：`InstallConfigMigrationRunner` + `.gitignore` + 移除 tracked install
3. **Phase C**：删除 `application-h2.yml`、`application-mysql.yml`，去掉 `profiles.active` 绑定
4. **Phase D**：更新 FAQ、TESTING.md

**回滚**：恢复 profile 文件与旧 `isInstalled()`；删除磁盘 install 或设 `completed: false`。

## Open Questions

- （无）产品已确认：无 example、开发首次走向导、Legacy 自动生成、全量写 install。
