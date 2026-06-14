# Phase 1 — Embedded Native 编译与 SPA 冒烟

## 目标

- embedded 前端打入 Native 二进制
- 首次 `package -Pnative` 编译通过
- Linux/macOS 冒烟：`/version`、SPA `/`、登录 API

## 前置

- JDK 17+（本机构建需 GraalVM native-image）
- Docker（默认 Release 路径：在 `ghcr.io/graalvm/native-image-community:21-ol9` 容器内执行 Maven）
- Node 18+（embedded 前端，`deploy/scripts/ui-build.sh embedded`）

## 关键修复（本 Phase 已落地）

### 1. thin JAR + exec 分类器

`-Pnative` 时根 POM 设置 `repackage.classifier=exec`，admin `spring-boot-maven-plugin` 使用 `${repackage.classifier}`：

- 主 artifact `cat2bug-admin.jar`（~42MB，含 AOT 类 + static，供 native-image classpath）
- 可执行 fat JAR `cat2bug-admin-exec.jar`（~150MB）

**根因**：此前 native-image 把 fat JAR 放在首位，主类在 `BOOT-INF/classes/`，Graal 无法加载。

### 2. MyBatis Native 配置

新增 `MyBatisNativeConfiguration`（mybatis-spring-boot-starter wiki），并在 `Cat2BugApplication` 上 `@Import`。

**根因**：`ClassPathMapperScanner` 静态初始化时 `LogFactory` 在 Native 下 NPE。

### 3. 构建脚本

- `native-maven-plugin` 0.10.x **不支持** `buildTool=docker` 参数
- 默认 `CONTAINER_BUILD=true`：在 GraalVM 容器内 `microdnf install maven && mvn ...`
- `CONTAINER_BUILD=false`：本机 GraalVM（开发冒烟，产物为 Mach-O 而非 Linux ELF）
- 输出文件名按 `file` 检测结果命名（避免 macOS 本机构建误标为 `linux-amd64`）

### 4. MyBatis `databaseId` Native（2026-06-14）

`MyBatisConfig#sqlSessionFactory` 使用 `resolveDatabaseId` 替代直接注入 `VendorDatabaseIdProvider`：

- Native 下 `spring.database-type` 未配置时默认 `h2`
- 避免 AOT/运行时通过 `DatabaseMetaData` 探测库类型失败，导致 H2 分支 SQL（`databaseId="h2"`）未选中

**根因**：Native 启动后 `databaseIdProvider.getDatabaseId(dataSource)` 不可靠，Mapper XML 中 MySQL 专用函数在 H2 上执行报错。

## 构建命令

```bash
cd .worktree/feature-spring-native-upx

# JVM 回归
mvn -pl cat2bug-platform-admin -am package -DskipTests

# Native（Linux Release，Docker 容器内 Maven）
UPX_COMPRESS=false ./deploy/build-native-spring.sh x86_64

# Native（本机 GraalVM，快速迭代）
SKIP_EMBEDDED=true UPX_COMPRESS=false CONTAINER_BUILD=false ./deploy/build-native-spring.sh
```

## Phase 1 首次编译结果（2026-06-13）

| 项 | 值 |
|----|-----|
| 构建方式 | 本机 GraalVM 17（macOS arm64） |
| 编译耗时 | ~5m 38s（含 AOT + native-image） |
| 二进制大小 | **362 MB** raw（Mach-O arm64） |
| Quarkus 对照 | ~190 MB ELF raw / ~42 MB UPX |
| 编译 | ✅ BUILD SUCCESS |
| 启动 | ✅ 2026-06-14：arm64 容器 embedded 构建可启动；`/version`、登录 API、H2 SQL 统计接口冒烟通过 |

## 待办（Phase 1 剩余）

- [x] 二次 Native 构建 + 启动冒烟（MyBatis 修复后）— 2026-06-14：embedded 路径 arm64 重建；`native-h2-sql-smoke.sh` 全通过
- [ ] Docker 容器 Linux amd64 构建验证 — 首次失败：` -Prelease` + POI exclude 导致 process-aot CNFE；已拆至 `-Pslim-jar`，待重试
- [x] arm64 容器构建 — 2026-06-14：`cat2bug-admin-linux-arm64` 338MB raw / 82MB UPX（`build-native-spring.sh aarch64`，非 `SKIP_EMBEDDED`）
- [x] SPA `/`、`/static/**`、登录 API 冒烟 — 2026-06-14：`GET /`、`/static/js/app.js`、登录 API 均在 Native 容器内 200
- [ ] 填入 `METRICS.md` 冷启动 / RSS

## Kill Gate

8 周内 Phase 1 未通过 → 文档化 blocker，保留 Quarkus 作为备选交付路径。
