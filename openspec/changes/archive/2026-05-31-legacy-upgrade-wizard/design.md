## Context

Cat2Bug 当前启动与配置体系：

- **首次安装**：`/setup` 向导 + `SetupFilter` 全锁；安装完成后写入 `config/install/application-install.yml` 并设 `cat2bug.install.completed=true`
- **Legacy 迁移**：`InstallConfigMigrationRunner` 在启动时若检测到「无磁盘 install 但有 legacy 数据/schema」，**静默**从 `Environment` 导出配置并写 completed install，用户无感知
- **Schema 升级**：Flyway 脚本位于 `db/migration/h2/` 与 `db/migration/mysql/`；`SetupMigrationService` 已在 setup submit 时调用 migrate
- **安装检测**：`InstallService.isInstalled()` 以磁盘 `cat2bug.install.completed=true` 为准；legacy 兜底检测 `sys_user.user_id=1`

老版本用户替换 JAR 升级时常见问题：

1. 新配置项（如 `cat2bug.cache.type`）未写入 install 文件
2. Flyway 待执行脚本需重启后生效
3. 静默迁移覆盖用户手工调优的 yml/env 值
4. 迁移失败时无 UI 重试，只能查日志手工修复

**产品决策（已确认）**：

| 项 | 决策 |
|----|------|
| 升级期间访问控制 | **全锁系统**（仅 `/upgrade/**`、静态资源、`/version`） |
| 失败处理 | **重试向导**，按步骤幂等续跑 |
| 多实例 | **暂不考虑**，文档声明单进程前提 |
| 配置 merge | **保留用户自定义**，模板仅补缺失项 |

## Goals / Non-Goals

**Goals:**

- Legacy 实例升级到新版本时，自动进入 `/upgrade` 向导而非静默写盘
- 升级向导展示版本概览、配置 diff（保留优先）、预检结果，用户确认后执行 config 写入 + Flyway migrate
- 升级 pending / failed / restartRequired 期间全锁业务 API
- 失败后持久化 `lastError`、`lastStep`，支持重试 submit
- 配置 merge 保留 Environment、磁盘 install 片段、用户向导修改的有效值
- 支持 `CAT2BUG_UPGRADE_SKIP=true`（或 `cat2bug.upgrade.skip=true`）供 Docker 预配置跳过

**Non-Goals:**

- 多实例/集群滚动升级、分布式锁
- H2 → MySQL 跨库数据迁移向导
- 升级期间只读模式（已选全锁）
- 在线热切换数据库类型
- 自动备份数据库（仅 UI 提醒用户自行备份）

## Decisions

### 1. 独立 `/upgrade` 路由（非复用 `/setup`）

**决策**：新建 `UpgradeController` + `views/upgrade/`，复用 setup 步骤子组件（连接测试、表单），但不与 setup 共用路由。

**理由**：setup 面向空库首次安装（含创建 admin）；upgrade 面向已有数据（不创建 admin、不跑完整 7 步）。状态机与 API 语义不同，分离可降低 `SetupFilter` 复杂度。

**备选**：扩展 `/setup?mode=upgrade` —— 前端步骤与后端校验分支过多，不采用。

### 2. 升级触发条件（`upgradeRequired`）

**决策**：满足以下任一条件且 `upgrade.skip=false` 时设 `upgradeRequired=true`：

| 条件 | 说明 |
|------|------|
| L2 配置迁移 | 无 completed disk install，但 `hasLegacyInstallation()` 或 `isSchemaPresent()` |
| L3 Schema 待升级 | Flyway 检测到 pending migrations（相对当前库 baseline） |
| 版本标记 | `cat2bug.upgrade.targetVersion` 高于已记录的 `cat2bug.upgrade.completedVersion`（可选，Phase 1 可与 Flyway pending 合并判断） |

**L1（纯 JAR 替换、无 pending）**：不进入向导，正常启动。

**改造 `InstallConfigMigrationRunner`**：检测到 L2 时 **仅** 调用 `UpgradeService.markPending()`，不写盘、不设 completed。

### 3. 升级状态持久化

**决策**：状态写入磁盘 install 侧车或 incomplete install 文件的 `cat2bug.upgrade` 段：

```yaml
cat2bug:
  upgrade:
    state: pending          # pending | running | failed | restart_required | completed
    lastError: ""
    lastStep: ""            # config | migration | restart
    attemptCount: 0
    targetVersion: "0.6.2"
    completedVersion: ""
    startedAt: ""
```

若 install 文件尚不存在，使用 `config/install/application-upgrade-state.yml` 或在内存 + `sys_config` 备份（优先 install 段避免多存储）。

**理由**：升级可能发生在「install 尚未 completed」阶段，状态需独立于 `cat2bug.install.completed`。

### 4. UpgradeFilter 全锁

**决策**：新增 `UpgradeFilter` `@Order(-199)`（在 `SetupFilter` 之后）。

```
upgradeActive = state ∈ { pending, running, failed, restart_required }

upgradeActive 时：
  ✅ /upgrade/**、/version、静态资源（同 SetupFilter 规则）
  ✅ GET /setup/status、GET /upgrade/status（供前端路由）
  ❌ 其他全部 API（含 /login、业务接口）

installed=false 且 upgradeRequired=false → 仍走 SetupFilter（setup 优先）
installed=true  且 upgradeRequired=true  → UpgradeFilter 全锁
```

**理由**：与用户确认的「升级期间全锁」一致；与 setup bootstrap 对称。

### 5. 配置 merge：保留优先

**决策**：`UpgradeConfigMerger.mergePreserveExisting(base, template, wizardOverrides)`：

```
优先级（高 → 低）：
  1. wizardOverrides（用户在本轮向导显式修改）
  2. base 中已有非空值（磁盘 partial install、Environment 导出）
  3. template 默认值（classpath application-install-*.yml）
```

**不覆盖**：已有效的 `spring.datasource.druid.master` URL/账号、已有 `cat2bug.profile` 路径等，除非用户在步骤 2 点「采用建议」或修改字段。

**UI**：步骤 2 展示 diff 表，默认「保留」；仅缺失项高亮「建议补全」。

### 6. 升级提交流程（幂等）

**决策**：`POST /upgrade/submit` 按 `lastStep` 续跑：

```
1. state → running；attemptCount++
2. if lastStep < config：
     mergePreserveExisting → write application-install.yml
     不立即设 install.completed=true（直到 migration 成功）
3. if lastStep < migration：
     SetupMigrationService.migrate()
     on Flyway error → state=failed, lastStep=migration, lastError=摘要
4. 全部成功：
     cat2bug.install.completed=true
     state=restart_required
5. 用户重启后：
     state=completed, completedVersion=targetVersion
     upgradeRequired=false
```

**重试**：`POST /upgrade/retry` 等价于 submit，读取 failed 状态从 `lastStep` 继续。

**Flyway repair**：checksum 冲突时在 retry 前可选调用 `flyway.repair()`（仅当错误类型匹配时）。

### 7. 前端路由

**决策**：`permission.js` 判断顺序：

```
1. fetch /upgrade/status
2. if upgradeRequired && state !== completed → redirect /upgrade
3. else fetch /setup/status
4. if !installed → redirect /setup
5. normal app
```

升级向导步骤（5 步）：

1. 概览（当前版本 → 目标版本、待执行 Flyway 列表、备份提醒）
2. 配置确认（diff 表，保留优先）
3. 预检（DB 连接、Redis 可选、路径可写）
4. 执行（进度 + 错误展示）
5. 完成 / 失败重试（restart 提示 或 重试按钮）

### 8. Skip 与环境变量

**决策**：`cat2bug.upgrade.skip=true` 或 `CAT2BUG_UPGRADE_SKIP=true` 时：

- 不进入 upgrade 向导
- **恢复现有静默行为**：`InstallConfigMigrationRunner` 可写 completed install + 自动 Flyway（与 today 行为一致，供 Docker）

**理由**：与 `CAT2BUG_INSTALL_SKIP` 对称，不阻断自动化部署。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 全锁期间用户无法登录查看数据 | 升级前 UI 明确提示；建议维护窗口 |
| Flyway 中途失败导致半升级状态 | `lastStep` 幂等 + repair；文档说明从 backup 恢复 |
| 多实例同时启动两个 JVM | 文档声明单实例；不做分布式协调（已确认 Non-Goal） |
| merge 保留旧值导致新功能默认项缺失 | diff UI 高亮「模板新增项」；预检报告缺失 key |
| upgrade 与 setup 状态交叉 | 判定顺序：未安装且无 legacy → setup；有 legacy → upgrade |

## Migration Plan

1. **发布前**：文档更新升级流程（备份 → 停服 → 换 JAR → 启动 → 完成向导 → 重启）
2. **首次启动新版本**：
   - Legacy 有数据无 install → `upgradeRequired`，进入 `/upgrade`
   - 用户确认 → config + Flyway → restart
3. **回滚**：保留旧 JAR + DB 备份；若 Flyway 已执行需按版本回滚脚本或还原备份
4. **Docker**：预配置镜像可设 `CAT2BUG_UPGRADE_SKIP=true` 跳过向导

## Open Questions

- Phase 2 是否在 diff UI 中支持逐项「采用建议 / 保留」批量操作（MVP 可只做缺失项）
- `completedVersion` 是否绑定 `cat2bug.version` 或 Flyway 最新 script 版本号（建议 Flyway max version）
