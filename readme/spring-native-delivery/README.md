# Spring Native 交付路线

在 **不重写 Spring MVC 业务代码** 的前提下，将 `cat2bug-platform-admin` 构建为 **embedded 单文件 Native ELF**（含 Vue SPA），作为 Cat2Bug 最终 Release 产物。

## 分支

- 开发分支：`feature/spring-native-upx`
- 基线：`core`（Spring Boot 3.3.13）
- 与 `feature/quarkus-full-migration` 并行；Quarkus 线作对照与 cherry-pick 来源

## 文档

| 文件 | 说明 |
|------|------|
| [PHASE-0.md](PHASE-0.md) | 构建链骨架与 JVM 回归 |
| [PHASE-1.md](PHASE-1.md) | Embedded Native 编译 + Kill Gate（待 Phase 1） |
| [METRICS.md](METRICS.md) | 体积/冷启动实测（对照 Quarkus） |

## 构建命令（Phase 0 起）

```bash
# JVM embedded（开发/回滚）
mvn -pl cat2bug-platform-admin -am package -DskipTests

# Spring Native embedded（需 Docker + Node）
./deploy/build-native-spring.sh x86_64
```

## OpenSpec

变更说明见 `openspec/changes/spring-native-delivery/`。
