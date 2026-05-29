## 1. 升级状态与配置基础

- [x] 1.1 新增 `cat2bug.upgrade.*` 配置项与 `UpgradeProperties`（state、lastError、lastStep、attemptCount、targetVersion、completedVersion、skip）
- [x] 1.2 实现 `UpgradeService`：`isUpgradeRequired()`、`getStatus()`、`markPending()`、状态读写（install 文件 `cat2bug.upgrade` 段或侧车文件）
- [x] 1.3 改造 `InstallConfigMigrationRunner`：L2 legacy 检测时调用 `markPending()`，仅在 `upgrade.skip=true` 时保留静默写盘行为
- [x] 1.4 扩展 `InstallService`：区分 `isInstalled()` 与 `upgradeRequired`；重启后 `restart_required → completed` 转换
- [x] 1.5 支持 `CAT2BUG_UPGRADE_SKIP=true` / `cat2bug.upgrade.skip=true` 跳过升级向导

## 2. 配置 Merge

- [x] 2.1 实现 `UpgradeConfigMerger.mergePreserveExisting(base, template, wizardOverrides)`（保留优先、模板补缺失）
- [x] 2.2 实现 `GET /upgrade/preflight`：导出 Environment 有效配置、classpath 模板、生成 diff 列表
- [x] 2.3 复用 `InstallConfigExporter` / `SetupConfigWriter` 写入路径，升级 submit 不写 completed 直到 migration 成功

## 3. 后端 Upgrade API

- [x] 3.1 新增 `UpgradeController`：`GET /upgrade/status`、`GET /upgrade/preflight`、`POST /upgrade/submit`、`POST /upgrade/retry`
- [x] 3.2 实现升级提交流程：config write → `SetupMigrationService.migrate()` → 设 `install.completed` → `state=restart_required`
- [x] 3.3 失败处理：捕获 Flyway/IO 异常，写 `lastError`/`lastStep`，`state=failed`；retry 按 `lastStep` 幂等跳过已完成步骤
- [x] 3.4 Flyway checksum 冲突时 retry 前可选 `flyway.repair()`（匹配错误类型时）
- [x] 3.5 `SecurityConfig` 放行 `/upgrade/**`（upgrade active 时）并禁止正常业务访问

## 4. UpgradeFilter 全锁

- [x] 4.1 新增 `UpgradeFilter` `@Order(-199)`：`state ∈ {pending,running,failed,restart_required}` 时仅放行 upgrade 白名单
- [x] 4.2 白名单：`/upgrade/**`、`/version`、静态资源；与 `SetupFilter` 协调（legacy → upgrade 优先于 setup）
- [x] 4.3 升级 active 时拒绝 `/login` 及全部业务 API，返回明确错误文案

## 5. 前端 Upgrade 向导

- [x] 5.1 新增路由 `/upgrade` 与 `views/upgrade/index.vue`（5 步：概览、配置确认、预检、执行、完成/失败重试）
- [x] 5.2 步骤 1：版本概览、待执行 Flyway 列表、备份提醒、单实例声明
- [x] 5.3 步骤 2：配置 diff 表（默认保留，缺失项高亮建议）；复用 setup 表单组件
- [x] 5.4 步骤 3：复用 setup 连接测试 API（database/redis/path）
- [x] 5.5 步骤 4/5：执行进度、错误展示、重试按钮、重启提示
- [x] 5.6 `permission.js`：优先查 `/upgrade/status`，`upgradeRequired` 时重定向 `/upgrade`；与 setup 路由互斥
- [x] 5.7 添加 i18n 中英文文案（`upgrade.*` 键）

## 6. 与 Setup 集成

- [x] 6.1 更新 `SetupFilter`：upgrade active 时不走 setup 白名单（或统一由 UpgradeFilter 先行拦截）
- [x] 6.2 确认 fresh empty DB 仍进 `/setup`；legacy 有数据无 install 进 `/upgrade`
- [x] 6.3 更新 legacy 相关单元测试（`InstallConfigMigrationRunnerTest` 等）

## 7. 测试与文档

- [x] 7.1 后端单元/集成测试：upgradeRequired 检测、merge 保留逻辑、submit/retry 幂等、全锁 Filter
- [x] 7.2 前端手工测试清单：legacy 实例升级流程、失败重试、重启后恢复正常
- [x] 7.3 新增 `TESTING.md`：覆盖 L2 配置迁移 + L3 Flyway 场景
- [x] 7.4 更新 `readme/production/faq.md` 升级章节与 CLAUDE.md 说明（单实例、skip env、重试向导）
