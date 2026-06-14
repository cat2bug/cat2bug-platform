# Phase 0 — 分支与构建链骨架

## 目标

- 从 `core` 建立 `feature/spring-native-upx` worktree
- 启用 Spring Boot 3 AOT / `native-maven-plugin` 骨架
- 提供 `deploy/build-native-spring.sh` 与文档模板
- **不要求** Phase 0 末 Native 编译通过（Phase 1 Kill Gate）

## 前置环境

| 工具 | 用途 | 安装示例 |
|------|------|----------|
| JDK 17+ | Maven 编译 | `brew install openjdk@17` |
| Maven 3.9+ | 构建 | 随 IDE 或 `brew install maven` |
| Docker | Native 容器构建（Phase 1+） | Docker Desktop |
| Node 18+ | `build:embedded` | `nvm install 18` |
| npm | 前端依赖 | `cd cat2bug-platform-ui && npm install` |
| upx（可选） | 分发包压缩 | `brew install upx` |

## Worktree

```bash
git worktree add .worktree/feature-spring-native-upx -b feature/spring-native-upx core
cd .worktree/feature-spring-native-upx
```

## Maven Profile

| Profile | 说明 |
|---------|------|
| `native` | 启用 AOT + `native-maven-plugin` |
| `embedded` | 默认；打包 `static/**` |
| `release` | Release 构建标记 |

Native 构建示例（Phase 1 验收用）：

```bash
./deploy/build-native-spring.sh x86_64
# 等价 Maven：
# mvn -pl cat2bug-platform-admin -am -Pnative -Pembedded -Prelease clean package -DskipTests \
#   -Dnative.build.tool=docker
```

## Phase 0 验收清单

- [x] worktree `feature/spring-native-upx` 已创建
- [x] 根 `pom.xml` 启用 `native-maven-plugin`（`native-buildtools` 0.10.3）
- [x] `cat2bug-platform-admin/pom.xml` profile `native` 绑定 AOT + native compile
- [x] `application-native.properties` 存在
- [x] `deploy/build-native-spring.sh`、`deploy/scripts/ui-build.sh` 可执行
- [x] `readme/spring-native-delivery/` 文档模板
- [x] JVM 回归：`mvn -pl cat2bug-platform-admin -am package -DskipTests` 成功

## 已知限制（Phase 0）

- Native **尚未**配置 `RuntimeHints`（Phase 1.2）
- 全量 admin Native 编译 **预期失败** 直至 Phase 1 blocker 修复
- `core` 上 embedded static 可能含 `stats.json`（packaging-slim 未合并时）；Phase 1 前需清理

## 下一步

Phase 1：embedded Native 首次编过 + SPA 冒烟 + 8 周 Kill Gate → 见 `PHASE-1.md`（Phase 1 开始时编写）。
