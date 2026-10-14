# Quarkus JVM 本地运行（Gate A）

## 端口与配置

| 项 | 值 |
|---|---|
| HTTP | `2020`（`quarkus.http.port`） |
| Token | 240 分钟（`cat2bug.token.expire-minutes`） |
| 上传/Git 根 | `cat2bug.profile`（默认 `./upload`）；Git bare 仓在 `{profile}/{git.root}` |
| 默认库 | Bootstrap H2；安装向导可切 MySQL 或文件 H2 |

主配置：`cat2bug-platform-quarkus-admin/src/main/resources/application.properties`  
安装后运行期：`config/install/application-install.yml` + `quarkus-runtime.properties`（由 `dev.sh` / `run-quarkus-jar.sh` 通过 `QUARKUS_CONFIG_LOCATIONS` 加载）

## 开发启动

```bash
# 推荐
./cat2bug-platform-quarkus-admin/dev.sh

# 或
mvn -Pquarkus -pl cat2bug-platform-quarkus-admin -am quarkus:dev
```

## 打包与运行 JAR

```bash
# 默认：H2（命名池 runtime-mysql 在 build-time 固定为 h2；本地/安装向导默认）
mvn -Pquarkus -pl cat2bug-platform-quarkus-admin -am package -DskipTests
./deploy/run-quarkus-jar.sh
# 或 embedded 前端后：
./deploy/build-embedded-quarkus.sh
./deploy/run-quarkus-jar.sh
```

**MySQL 生产包**：`db-kind` / MyBatis `database-id` 为构建期属性，须单独打包：

```bash
mvn -Pquarkus -pl cat2bug-platform-quarkus-admin -am package -DskipTests -Dquarkus.profile=mysql
```

安装向导写入的 `quarkus-runtime.properties` 会带 `jdbc.driver`；与构建 profile 不一致时，启动日志会出现 `Build time property cannot be changed at runtime`。

## 冒烟建议

1. `GET /q/health` 或产品健康端点  
2. 打开 `http://localhost:2020/` SPA  
3. 登录（已安装用现有账号；未安装走安装向导）  
4. 项目切换 → 缺陷/用例列表 → 成员 → 文档 → 源码管理  

## Gate

- **Gate A**：本文件路径上的 JVM 功能验收通过后再做 Native  
- **Gate B**：见 `GATE-NOTES.md`（约 20MB，默认按 UPX 后计）
