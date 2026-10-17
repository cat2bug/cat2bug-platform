# cat2bug-platform-admin

Quarkus 全量迁移主入口（Phase 0 自 Spike 演进）。

## 模块依赖

```
quarkus-admin → quarkus-framework → quarkus-common
```

## 命令

```bash
# 从仓库根目录
# 推荐：一键 dev（含 install + quarkus:dev）
./cat2bug-platform-admin/dev.sh

# 或分步
mvn -pl cat2bug-platform-admin -am install -DskipTests
mvn -f cat2bug-platform-admin/pom.xml quarkus:dev

# 测试
mvn -pl cat2bug-platform-admin -am test
```

默认端口 `2020`，开发账号 `admin` / `cat2bug`。

**Phase 1 API**：`/login`、`/logout`、`/getInfo`、`/getRouters`、`/version`、`/system/defect/list`。会话默认 `cat2bug.session.store=memory`；生产 Redis 见 [PHASE-1.md](../readme/quarkus-full-migration/PHASE-1.md)。

**注意**：无 embedded Vue 页面时，完整 UI 用 `npm run dev`（`:2222`）代理到 `:2020`。

**Phase 5.3 embedded 单 JAR**：

```bash
# 从仓库根目录
./deploy/build-embedded.sh

# 或分步
cd cat2bug-platform-ui && npm run build:embedded
cd .. && mvn -Pembedded -pl cat2bug-platform-admin -am package -DskipTests
java -jar cat2bug-platform-admin/target/quarkus-app/quarkus-run.jar
```

前端产物写入 `src/main/resources/META-INF/resources/`（Quarkus 静态根）。`-Pembedded` 将该目录打入 JAR；默认 dev 构建不包含该目录，避免无前端时打包失败。

本仓库编译只打 JAR。Native 构建在 `cat2bug-build/scripts/platform/build-linux-native.sh`（`./deploy/build-native.sh` 会转过去），冒烟：`./deploy/smoke-native-docker.sh`（`FRESH=true` 测全新安装）。更多说明见 [deploy/README.md](../deploy/README.md) 与 `docs/02-系统架构/部署.md`。
