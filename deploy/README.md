# Cat2Bug 部署脚本

## 默认 Release（Spring Native）

| 脚本 | 说明 |
|------|------|
| [`build-native-spring.sh`](build-native-spring.sh) | embedded SPA + Native 双架构构建 + UPX |
| [`docker/run-native-spring-minimal.sh`](docker/run-native-spring-minimal.sh) | 最小 Debian/Distroless 容器运行 UPX 二进制 |
| [`docker/run-native-minimal.sh`](docker/run-native-minimal.sh) | OpenSpec 兼容别名（委托 spring 脚本） |
| [`test/native-api-smoke.sh`](test/native-api-smoke.sh) | L1–L4 API 冒烟 |
| [`test/native-h2-sql-smoke.sh`](test/native-h2-sql-smoke.sh) | L5 H2 SQL / Phase 2 抽样 |
| [`scripts/h2-mapper-smoke.sh`](scripts/h2-mapper-smoke.sh) | JVM 秒级 H2 Mapper 回归（~5s） |

```bash
# 构建（示例 arm64）
./deploy/build-native-spring.sh aarch64

# Docker 运行
PORT=2020 ./deploy/docker/run-native-spring-minimal.sh run-bg debian

# 冒烟
./deploy/test/native-api-smoke.sh http://127.0.0.1:2020
./deploy/test/native-h2-sql-smoke.sh http://127.0.0.1:2020
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

## CI

[`ci/spring-native.yml`](ci/spring-native.yml) — 双架构构建 + Docker 冒烟草案。

## 详细文档

- [readme/spring-native-delivery/README.md](../readme/spring-native-delivery/README.md)
- [METRICS.md](../readme/spring-native-delivery/METRICS.md)
