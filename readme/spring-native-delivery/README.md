# Spring Native 交付路线

在 **不重写 Spring MVC 业务代码** 的前提下，将 `cat2bug-platform-admin` 构建为 **embedded 单文件 Native ELF**（含 Vue SPA），作为 Cat2Bug 最终 Release 产物。

## 分支

- 开发分支：`feature/spring-native-upx`（历史；默认 Release 已合入 `core`）
- 基线：`core`（Spring Boot 3.3.x）
- 可执行后端：**仅** `cat2bug-platform-admin`（Spring Boot / Spring Native）

## 文档

| 文件 | 说明 |
|------|------|
| [PHASE-0.md](PHASE-0.md) | 构建链骨架与 JVM 回归 |
| [PHASE-1.md](PHASE-1.md) | Embedded Native 编译 + Kill Gate |
| [PHASE-2.md](PHASE-2.md) | 全功能 Native parity 与冒烟清单 |
| [PHASE-3.md](PHASE-3.md) | 体积优化与 UPX |
| [PHASE-4.md](PHASE-4.md) | RPM / 本地交付 / Go-Live |
| [METRICS.md](METRICS.md) | 体积/冷启动实测（含历史对照基线） |
| [NATIVE-API-SMOKE-TEST-PLAN.md](NATIVE-API-SMOKE-TEST-PLAN.md) | API 冒烟分层说明 |

## 构建命令（Phase 0 起）

```bash
# JVM embedded（开发/回滚）
mvn -pl cat2bug-platform-admin -am package -DskipTests

# Spring Native embedded（需 Docker + Node）
./deploy/build-native-spring.sh x86_64
```

## 历史对照（已退役 Quarkus 交付）

- 本仓库 **不再** 构建或发布 Quarkus 模块；`cat2bug-platform-quarkus-*` 已从 `core` 移除。
- `METRICS.md` 中 **190 MB / 42 MB UPX** 等数字为早期 `feature/quarkus-full-migration` 实测，仅作体积对照，**非** 当前交付路径。
- 生产默认交付：**Spring Native**（`./deploy/build-native-spring.sh`）；开发/回滚：**Spring Boot JVM**（`mvn spring-boot:run` / `cat2bug-admin.jar`）。

## OpenSpec

变更说明见 `openspec/changes/spring-native-delivery/`。
