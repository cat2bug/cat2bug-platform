# Docker Compose 部署说明

## 构建与启动

```bash
./deploy/docker/build.sh          # 前端 build:prod → static/；后端 MySQL 版单 JAR → cat2bug-admin.jar
cd deploy/docker
docker compose build
docker compose up -d
```

默认端口 `8022`（nginx 反代后端 `2020`）。首次访问进入 `/setup` 安装向导；提交后后端进程退出，由 `restart: always` 拉起，容器重启一次属正常现象。

`SKIP_FRONTEND=true ./deploy/docker/build.sh` 只重打后端。

后端镜像按 `-Dquarkus.profile=mysql` 打包（数据源驱动类型为构建期属性），运行期 `QUARKUS_PROFILE=prod`，因此 **Compose 镜像只支持 MySQL**，安装向导中不要选 H2。

## 持久化

| 宿主目录 | 容器路径 | 内容 |
|----------|----------|------|
| `./uploadPath` | `/home/uploadPath` | `cat2bug.profile`：附件、头像、Git bare 仓（`git/` 子目录，含隐藏文档仓） |
| `./config` | `/home/config` | 安装向导写入的 `install/application-install.yml`、`install/quarkus-runtime.properties`（含数据库密码） |
| `./mysql-data` | `/var/lib/mysql` | MySQL 数据 |

三者都必须持久化：丢 `uploadPath` 会出现「数据库里有记录但 Git 仓库/附件读不到」；丢 `config` 则重建容器后回到 `/setup`。`CAT2BUG_PROFILE` 必须与 `uploadPath` 的容器挂载点一致。

## 环境变量

### 运行期

| 变量 | 说明 |
|------|------|
| `CAT2BUG_IN_DOCKER=true` | 容器模式：安装完成后不自行重启进程，交给容器重启策略 |
| `CAT2BUG_PROFILE` | 存储根目录 |
| `CAT2BUG_CACHE_TYPE` | `redis` 时必须同时设置 `CAT2BUG_REDIS_HOSTS`，否则启动失败 |
| `CAT2BUG_REDIS_HOSTS` | 如 `redis://cat2bug-platform-redis:6379` |
| `CAT2BUG_UPGRADE_SKIP=true` | 已完成安装且有待执行 Flyway 脚本时，不进入 `/upgrade` 页面，启动期静默迁移；失败则进程非零退出 |
| `CAT2BUG_INSTALL_SKIP=true` | 跳过首次 `/setup`（已预置安装配置时使用） |

### 安装向导预填

`GET /setup/preflight` 从环境变量读取默认值填入向导表单，未设置的项保留向导内置默认。

| 向导字段 | 环境变量 |
|----------|----------|
| 数据库类型 | `SPRING_DATABASE_TYPE`；未设置时按 JDBC URL 是否含 `jdbc:mysql` 推断 |
| MySQL 连接 | `SPRING_DATASOURCE_DRUID_MASTER_URL`、`SPRING_DATASOURCE_DRUID_MASTER_USERNAME`、`SPRING_DATASOURCE_DRUID_MASTER_PASSWORD` |
| MySQL 主机/库名覆盖 | `CAT2BUG_SETUP_MYSQL_HOST`、`CAT2BUG_SETUP_MYSQL_DATABASE` |
| 缓存类型 | `CAT2BUG_CACHE_TYPE` |
| Redis 连接 | `CAT2BUG_SETUP_REDIS_HOST`，或 `SPRING_REDIS_HOST`、`SPRING_REDIS_PORT`、`SPRING_REDIS_PASSWORD`、`SPRING_REDIS_DATABASE` |
| 存储/临时/日志路径 | `CAT2BUG_PROFILE`、`CAT2BUG_TEMP`、`LOGGING_FILE_PATH` |
| AI / Ollama | `CAT2BUG_AI_ENABLED`、`CAT2BUG_AI_HOST` |

`SPRING_*` 前缀是安装配置 YAML 键的历史命名，只用于预填，与运行期框架无关。

## 首次安装与附着旧库

- MySQL 库不存在，或库存在但没有成功的 Flyway 迁移记录（例如 Compose 预建的空库）：向导按 `new` 处理，提交时执行 Flyway 建表。
- 库里已有 Flyway 迁移记录：按 `existing` 附着，安装阶段不迁移；若 schema 落后，安装完成后进入 `/upgrade`，或用 `CAT2BUG_UPGRADE_SKIP` 静默迁移。
