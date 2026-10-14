# Quarkus 数据源 db-kind 是构建期属性

用默认单 JAR 连 MySQL 失败的原因，以及 MySQL 包该怎么打。

- 日期：2026-10-14
- 涉及：后台 / 部署

## 现象

`./deploy/build-embedded.sh` 打出的 `cat2bug-admin.jar` 用 H2 一切正常，但在安装向导里选 MySQL（或 Docker 镜像连 Compose 的 MySQL）时启动或连库失败，报错：

```
Driver does not support the provided URL
```

## 根因

Quarkus 的 `quarkus.datasource.<name>.db-kind` 是**构建期属性**：打包时就决定了内置哪种 JDBC 驱动和方言，运行期改 URL 或 profile 都改不了。`application.properties` 中命名池 `runtime-mysql` 默认 `db-kind=h2`，只有 `%mysql` profile 下才是 `mysql`，所以默认构建出来的 JAR 只认 H2 的 URL。

## 解决办法

需要 MySQL 时按 mysql profile 打包，运行期再切回 prod（不切回的话，`%mysql` 下的占位数据源会污染安装向导预填）：

```bash
mvn -Pembedded -Prelease -Dquarkus.profile=mysql -pl cat2bug-platform-admin -am package -DskipTests
QUARKUS_PROFILE=prod java -jar cat2bug-platform-admin/target/cat2bug-admin-runner.jar
```

Docker 镜像已按此方式构建（`deploy/docker/build.sh` + `after-dockerfile` 中的 `QUARKUS_PROFILE=prod`），因此 Compose 只支持 MySQL，见 [[部署]]。

## 以后怎么避免

- 同一个 JVM 包不要指望「运行期切数据库类型」；H2 包和 MySQL 包是两个产物，发布说明里写清楚。
- 改 `quarkus.datasource.*` 配置前先查 Quarkus 文档里该属性是否标为 build time（锁形图标）。
- Native 包是否同时内置 H2 与 MySQL 仍待确认，见 [[数据库]]。
