# 去掉 Spring BOM 后传递依赖版本漂移

删除根 pom 的 BOM / dependencyManagement 后，构建失败或依赖版本悄悄变化的原因与排查方法。

- 日期：2026-10-14
- 涉及：后台

## 现象

删除 Spring 后端时，从根 `pom.xml` 去掉了 Spring Boot BOM 等仅 Spring 使用的依赖管理。之后离线构建（`mvn -o`）报依赖解析失败：本地仓库里没有新解析出来的版本。另有一些依赖虽然能解析，版本却变了，例如 system 模块的 jedis 从 5.0.2 变成 5.1.5。

## 根因

BOM 不只管自己声明的直接依赖，也统一钉住了大量**传递依赖**的版本。BOM 一删，这些传递依赖改由 Maven 「最近优先」规则或 Quarkus BOM 决定版本，结果和之前不同；离线时新版本不在本地仓库，于是解析失败。

## 解决办法

1. 删除前后对每个模块跑一次依赖清单并对比：

   ```bash
   mvn -pl cat2bug-platform-system dependency:list -DoutputFile=deps-before.txt   # 删除前
   mvn -pl cat2bug-platform-system dependency:list -DoutputFile=deps-after.txt    # 删除后
   diff <(sort deps-before.txt) <(sort deps-after.txt)
   ```

2. 只在必要处固定版本：本次在根 pom `dependencyManagement` 补了 `commons-pool2 2.12.1`、`jakarta.websocket-client-api 2.1.1`；jedis 的 5.1.5 与运行时实际使用的版本一致，接受变化不回钉。
3. 全量测试通过（162 个）后再提交。

## 以后怎么避免

- 删除或升级 BOM、父 pom 时，把「各模块 `dependency:list` 前后对比」作为固定步骤，不要只看编译是否通过。
- 不要因为漂移就把所有依赖都钉死；优先跟随 Quarkus BOM，只给 BOM 不管的依赖补版本。
