# Phase 2 — 全功能 Native Parity

**Change:** `spring-native-delivery`  
**分支:** `feature/spring-native-upx`  
**状态:** 完成（2026-06-15，2.9 Upgrade 全流程已自动化）

## 目标

在 Phase 1 Kill Gate 通过后，将 Spring Native 产物与 JVM 版在 **API 契约与核心业务** 上对齐，并建立可重复的冒烟回归清单。

## 任务进度

| 任务 | 说明 | 状态 | 验收 |
|------|------|------|------|
| 2.1 | MyBatis RuntimeHints + PageHelper | **通过** | `MyBatisNativeConfiguration`；`native-h2-sql-smoke.sh` |
| 2.2 | Security JWT + `@PreAuthorize` | **通过** | `JwtNativeRuntimeHints`、`SecurityNativeRuntimeHints`；`native-api-smoke.sh` L2–L4 |
| 2.3 | J2Cache / 缓存 | **通过（单实例）** | `RedisCache` Native 进程内兜底；详见 [`J2CACHE-NATIVE.md`](J2CACHE-NATIVE.md)；`native-redis-smoke.sh` |
| 2.4 | Open API `/api/**` | **通过**（2026-06-15） | `native-parity-smoke.sh`；`ApiSecurityConfig` `@Bean` 注册过滤器（修复 Native CGLIB NPE） |
| 2.5 | WebSocket / IM | **通过**（2026-06-15） | `native-websocket-smoke.py` HTTP 101 握手 |
| 2.6 | AI `/ai/**` | **通过**（2026-06-15） | `GET /system/ai/project-model-options`、`/system/ai/list` 无 Native 崩溃 |
| 2.7 | Quartz 定时任务 | **通过**（2026-06-15） | `GET /monitor/job/list`；启动日志无 Scheduler 失败 |
| 2.8 | Setup 向导 | **通过**（2026-06-15） | fresh 容器 `POST /setup/submit` + 重启后 `installed=true` |
| 2.9 | Upgrade 向导 | **通过**（2026-06-15） | `native-upgrade-smoke.sh`：L3 drift→pending→全锁→submit→restart→completed |
| 2.10 | Excel FastExcel | **完成** | `native-excel-smoke.sh` 5/5；POI 剔除；raw 304M / UPX 76M |
| 2.11 | Captcha 无 AWT | **通过** | `NativeCaptchaSupport`；`verify-native-no-poi.sh` 通过 |
| 2.12 | native profile 排除 | **通过** | `dev-tools` profile 隔离 generator/devtools/Knife4j；`-Pnative` 编译排除 POI/CaptchaSupport |
| 2.13 | JVM 回归 + 本清单 | **本文件** | `mvn test` 关键模块；对照下表手工项 |

## 手工冒烟清单（对照 JVM）

在 Native Docker 容器 `http://127.0.0.1:2020` 执行：

```bash
# 构建并启动（embedded 或 API-only）
./deploy/build-native-spring.sh aarch64
PORT=2020 ./deploy/docker/run-native-spring-minimal.sh run-bg debian

# 自动化
./deploy/test/native-spa-smoke.sh
./deploy/test/native-api-smoke.sh
./deploy/test/native-redis-smoke.sh --with-redis   # Redis 连通 + Setup 测试（可选）
./deploy/test/native-h2-sql-smoke.sh
# Excel 依赖当前项目：fresh setup 会先跑 bootstrap（团队/项目/userConfig）
./deploy/test/native-smoke-bootstrap.sh   # 可单独执行，输出 export 语句
./deploy/test/native-excel-smoke.sh
./deploy/test/native-parity-smoke.sh      # Open API / WebSocket / AI / Quartz / Setup / Upgrade status
./deploy/test/native-upgrade-smoke.sh     # Legacy Upgrade L3 全流程（隔离卷 @2028）

# UI（可选）
node deploy/test/native-ui-smoke.mjs
```

### L1 公共接口

- [x] `GET /version` → 200
- [x] `GET /captchaImage` → 200
- [x] `GET /setup/status` → 200

### L2 鉴权

- [x] `POST /login` → token
- [x] `GET /getInfo` → user/roles

### L3 文件与团队

- [x] `POST /common/upload` → url
- [x] `POST /system/team` → 无 `MissingReflectionRegistrationError`
- [x] `GET /system/team/my` → 200

### L4 权限抽样

- [x] 团队/项目/缺陷/用例/成员/管理端团队 list → 非 500 反射错误

### L5 H2 SQL

- [x] 缺陷统计、仪表盘、open-workload 等 → 无 `JdbcSQLSyntaxErrorException` / `json_contains` 问题

### L6 Excel（FastExcel）

- [x] 缺陷/用例/用户导入模版
- [x] 缺陷导出
- [x] 仪表盘走势导出（Native 仅数据表，无 POI 图表）

### 待补项

- [x] Upgrade：Legacy 库 pending → completed（`native-upgrade-smoke.sh`，删除 `sys_db_version` 最高版本模拟 drift）
- [ ] Monitor 导出（`/monitor/*/export`）：JVM=POI；Native 暂不可用

## JVM 回归（每个 Phase 2 迭代）

```bash
mvn -pl cat2bug-platform-admin -am package -DskipTests
./deploy/scripts/h2-mapper-smoke.sh
```

## JVM 回归证据

一键脚本（日志追加到 `deploy/test/logs/jvm-regression-YYYYMMDD.log`）：

```bash
./deploy/scripts/jvm-embedded-regression.sh
```

脚本依次执行：

1. `mvn -pl cat2bug-platform-admin -am test`
2. `mvn -pl cat2bug-platform-admin -am package -DskipTests`
3. 记录 `cat2bug-admin.jar` 体积

**最近一次结果格式（示例，以当日 log 为准）：**

```
======== 2026-06-15T… jvm-embedded-regression start host=Darwin-arm64 … ========
======== … mvn -pl cat2bug-platform-admin -am test ========
… BUILD SUCCESS …
[exit=0] mvn -pl cat2bug-platform-admin -am test
======== … mvn -pl cat2bug-platform-admin -am package -DskipTests ========
… BUILD SUCCESS …
127M	cat2bug-platform-admin/target/cat2bug-admin.jar
======== … jvm-embedded-regression done overall_fail=0 log=…/jvm-regression-20260615.log ========
```

补充：`mvn -pl cat2bug-platform-system test` 可作为 system 模块 spot-check，输出可追加到同一 log。


## 与 Quarkus 线 cherry-pick 对照

| 已移植 | 来源 |
|--------|------|
| FastExcel SPI（B9） | quarkus-admin excel 包 |
| CaptchaPngRenderer | quarkus Captcha |
| packaging-slim | prod-docs-images-slim |
| UPX / Docker minimal 脚本结构 | quarkus deploy |

## 已知差距（不阻塞 Phase 3/4）

- Stretch 体积未达标（见 `METRICS.md`）
- 多实例生产需 Redis（`cat2bug.cache.type=redis`）；Native 多实例会话不共享，见 [`J2CACHE-NATIVE.md`](J2CACHE-NATIVE.md)
- Legacy Upgrade 全流程见 `deploy/test/native-upgrade-smoke.sh`（勿设 `CAT2BUG_UPGRADE_SKIP`）
