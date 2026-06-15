# Cat2Bug 部署脚本

## 默认 Release（Spring Native）

| 脚本 | 说明 |
|------|------|
| [`build-native-spring.sh`](build-native-spring.sh) | embedded SPA + Native 双架构构建 + UPX |
| [`docker/run-native-spring-minimal.sh`](docker/run-native-spring-minimal.sh) | 最小 Debian/Distroless 容器运行 UPX 二进制 |
| [`docker/run-native-minimal.sh`](docker/run-native-minimal.sh) | OpenSpec 兼容别名（委托 spring 脚本） |
| [`test/native-api-smoke.sh`](test/native-api-smoke.sh) | L1–L4 API 冒烟 |
| [`test/native-h2-sql-smoke.sh`](test/native-h2-sql-smoke.sh) | L5 H2 SQL / Phase 2 抽样 |
| [`test/native-excel-smoke.sh`](test/native-excel-smoke.sh) | L6 Excel 导出（含 bootstrap） |
| [`test/native-smoke-bootstrap.sh`](test/native-smoke-bootstrap.sh) | fresh setup 补齐团队/项目/userConfig |
| [`test/native-parity-smoke.sh`](test/native-parity-smoke.sh) | Phase 2：Open API / WebSocket / AI / Quartz / Setup |
| [`test/native-upgrade-smoke.sh`](test/native-upgrade-smoke.sh) | Legacy Upgrade L3 全流程（隔离卷 @2028） |
| [`test/measure-native-coldstart.sh`](test/measure-native-coldstart.sh) | raw/UPX 冷启动度量（见 `METRICS.md`） |
| [`scripts/h2-mapper-smoke.sh`](scripts/h2-mapper-smoke.sh) | JVM 秒级 H2 Mapper 回归（~5s） |

```bash
# 构建（示例 arm64）
./deploy/build-native-spring.sh aarch64

# Docker 运行
PORT=2020 ./deploy/docker/run-native-spring-minimal.sh run-bg debian

# 冒烟
./deploy/test/native-api-smoke.sh http://127.0.0.1:2020
./deploy/test/native-h2-sql-smoke.sh http://127.0.0.1:2020
./deploy/test/native-excel-smoke.sh http://127.0.0.1:2020
./deploy/test/native-parity-smoke.sh http://127.0.0.1:2020
./deploy/test/native-upgrade-smoke.sh
# fresh setup（仅 admin/cat2bug、无当前项目）时 excel 脚本会自动 bootstrap
```

## JVM 开发 / 回滚

```bash
mvn -pl cat2bug-platform-admin -am package -DskipTests
java -jar cat2bug-platform-admin/target/cat2bug-admin.jar

# 瘦身 JAR（无 POI，无 Excel 运行时）
mvn -pl cat2bug-platform-admin -am package -DskipTests -Pslim-jar
```

## RPM（Spring Native 草案）

见 [`rpm/cat2bug/build-rpm-spring.sh`](rpm/cat2bug/build-rpm-spring.sh) 与 [`readme/spring-native-delivery/PHASE-4.md`](../readme/spring-native-delivery/PHASE-4.md)。

## 本地构建与冒烟

开源项目**不维护 GitHub Actions / CI**；Release 在本地或自建机构建。

```bash
# 构建（双架构择一）
./deploy/build-native-spring.sh aarch64
./deploy/build-native-spring.sh x86_64

# amd64 构建完成后：冷启动 + RPM（需 nfpm）
./deploy/scripts/post-amd64-native.sh

# Docker 运行 + 冒烟
PORT=2020 ./deploy/docker/run-native-spring-minimal.sh run-bg debian
./deploy/test/native-api-smoke.sh http://127.0.0.1:2020
./deploy/test/native-h2-sql-smoke.sh http://127.0.0.1:2020
./deploy/test/native-parity-smoke.sh http://127.0.0.1:2020
./deploy/test/native-upgrade-smoke.sh
./deploy/test/measure-native-coldstart.sh raw 3
```

Fresh setup 场景可先 `POST /setup/submit` 再跑 parity；`native-upgrade-smoke.sh` 使用隔离卷 @2028，**勿设** `CAT2BUG_UPGRADE_SKIP`（静默 Flyway 仅见 [`docker/README.md`](docker/README.md) Compose 说明）。

**amd64 在 Apple Silicon 上**：Docker `linux/amd64` 交叉编译 embedded Native 内存占用高；若 `native-image` 以 **exit 137** 结束，请调高 Docker Desktop 内存（建议 ≥16GB），或在 **Linux x86_64** 主机执行 `./deploy/build-native-spring.sh x86_64`。可先 `SKIP_EMBEDDED=true` 缩小产物验证链路。

## 详细文档

- [readme/spring-native-delivery/README.md](../readme/spring-native-delivery/README.md)
- [METRICS.md](../readme/spring-native-delivery/METRICS.md)
- [VOLUME-ANALYSIS.md](../readme/spring-native-delivery/VOLUME-ANALYSIS.md)（embedded vs API-only 剖析）
