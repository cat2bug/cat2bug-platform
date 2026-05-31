# setup-existing-database-attach 手工测试清单

## 前置

- 后端 `http://localhost:2020`，前端 dev `http://localhost:2222` 或嵌入式 `npm run build:embedded`
- 测试前备份 `./data/` 与 `config/install/`

## 自动化测试（JUnit）

| 场景 | 测试类 |
|------|--------|
| H2/MySQL new/existing 探测 | `DatabaseExistenceProbeTest`、`SetupDatabaseTestServiceTest` |
| existing H2 setup 跳过 migrate、admin 覆盖 | `SetupInstallServiceExistingH2Test` |
| install 完成 + pending → upgradeRequired | `UpgradeServiceTest` |
| skip + pending → 静默 migrate / 失败 exit | `PostInstallFlywayRunnerTest`、`UpgradeServiceTest` |

运行示例：

```bash
mvn test -Dtest=DatabaseExistenceProbeTest -pl cat2bug-platform-admin
mvn test -Dtest=SetupInstallServiceExistingH2Test -pl cat2bug-platform-admin
mvn test -Dtest=PostInstallFlywayRunnerTest -pl cat2bug-platform-admin
mvn test -Dtest=UpgradeServiceTest -pl cat2bug-platform-framework
```

---

## A. 全新安装（new 模式）

### A1 H2 新库

1. 删除 `config/install/` 与 `./data/cat2bug_platform*.mv.db`（及旧版 `./data/cat2bug*.mv.db` 若存在）
2. 启动应用 → 应进入 `/setup`
3. 数据库名 `cat2bug_platform`，测试连接 → `databaseMode: new`
4. 完成安装 → 生成 `./data/cat2bug_platform.mv.db` 与 install 文件
5. 重启 → 正常登录，不进 upgrade

### A2 MySQL 新库

1. 删除 install；MySQL 中 DROP DATABASE `cat2bug`（若存在）
2. 测试连接 → `new`；完成安装 → 库创建 + 表存在
3. 重启 → 正常登录

---

## B. 附着旧库（existing 模式）

### B1 H2 旧库

1. 保留已有 `./data/cat2bug_platform.mv.db`（含 legacy 数据），删除 `config/install/`
2. 启动 → `/setup`（**不是** `/upgrade`）
3. 测试连接 → `existing`
4. 设置新 admin 密码，完成安装
5. **不应**在 setup 阶段执行 Flyway
6. 重启 → 若 schema 落后 → `/upgrade`；若已最新 → 直接登录
7. 用向导设置的 admin 密码登录成功

### B2 MySQL 旧库

1. 保留已有 `cat2bug` schema，删除 install
2. 同 B1 流程；setup 不 migrate
3. admin 凭证被覆盖

---

## C. install 完成后 schema 升级

### C1 交互式 upgrade

1. 完成 B1/B2 后，使用版本落后 schema 的 JAR（或手动删 Flyway 记录模拟）
2. 重启 → `/upgrade`，业务 API 与登录被锁
3. 开始升级 → 成功 → 重启提示 → 登录正常

### C2 静默 migrate（Docker）

1. install 已完成，`CAT2BUG_UPGRADE_SKIP=true`
2. 部署含 pending 迁移的新 JAR
3. 启动 → **不**进 upgrade UI；日志显示 Flyway 成功
4. 应用正常提供服务

### C3 静默 migrate 失败 exit

1. 同上，但人为破坏 migration（或错误 checksum）
2. 进程应以非零退出；不应以半升级状态继续服务

---

## D. 回归

### D1 空库不误判 legacy

1. 删 install + 删 H2 文件
2. 启动 → `/setup`，`upgradeRequired: false`

### D2 upgrade skip + 无 install（Docker 预置）

1. 有 legacy 数据、无 install、`CAT2BUG_UPGRADE_SKIP=true`
2. 仍自动写 completed install（保留现有行为）

### D3 双提示 / 重启路由

1. upgrade 完成 `restart_required` 时不重复 toast
2. `permission.js` 不误跳 `/setup`

---

## 验收标准

- [ ] H2/MySQL 均支持数据库名 + new/existing 检测
- [ ] existing setup 不 Flyway，admin 覆盖
- [ ] 无 install 的 legacy 走 setup 不走 upgrade
- [ ] install 后 pending 走 upgrade 或 skip 静默
- [ ] 静默 migrate 失败进程退出
