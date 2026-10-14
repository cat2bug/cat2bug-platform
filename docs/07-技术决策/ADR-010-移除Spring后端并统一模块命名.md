# ADR-010 移除 Spring 后端并统一模块命名

记录 2026-10 删除整条 Spring Boot 后端、把 Quarkus 模块去掉技术栈后缀的决定；部分取代 [[ADR-003-恢复Quarkus为默认后端-Spring保留为回滚线]] 中「Spring 保留为回滚线」的内容。

- 日期：2026-10-14
- 状态：已采纳

## 背景

[[ADR-003-恢复Quarkus为默认后端-Spring保留为回滚线]] 落地后，Quarkus 已是默认后端并与 Spring 全量对等，且 Quarkus 模块不依赖任何 Spring artifact。但仓库仍并存两套后端：9 个 Spring 模块 + 4 个 `cat2bug-platform-quarkus-*` 模块 + 废弃的 `quarkus-spike`。默认 `mvn package` 构建的却是 Spring，产品构建反而要加 `-Pquarkus`。新功能和修复只在 Quarkus 做，Spring 线持续腐化（例如 [[系统管理员在Git模块无权限]]、`defect_ids` LIKE 精确匹配都没同步过去），维护和认知成本都很高。依据：`openspec/changes/retire-spring-and-unify-modules/proposal.md`、`design.md`。

## 候选方案

### 方案 A：继续保留 Spring 作回滚线（维持 ADR-003）

优点：随时能切回 Spring JAR。

缺点：两线行为已经分叉，回滚到一个「缺修复」的后端价值很低；默认构建与产品构建不一致，新人和 AI 都容易改错线。

### 方案 B：删除 Spring 线，以 git tag 留档；同时把模块、artifactId、包名去掉 `quarkus`

优点：只剩一套后端，默认 `mvn package` 即产品构建；目录与包名不再带技术栈后缀，以后换框架也不必再改名。

缺点：一次性改动面大（约 1.3k 个文件删除、约 900 个文件改路径或包名）；`.worktree/` 中基于旧结构的分支合并时会大面积冲突。

### 方案 C：只删 Spring，不改名

优点：改动小。

缺点：`-quarkus-` 后缀已失去区分意义，长期噪声；以后再改名成本只会更高。

### 方案 D：在实现手段上，用 IDE 重构改包名，或一次性大提交

缺点：IDE 重构覆盖不到 mapper XML、`application*.properties`、Native `--features` 等字符串配置；一次性大提交难审查，`git log --follow` 容易失效。否决。

## 最终决定

采用方案 B：

- 删除前打本地 tag `spring-final-1.1.0`（未推送远端），指向删除前最后一个提交。
- 删除 Spring 9 个模块（`admin/framework/system/common/api/ai/im/quartz/generator`）、`cat2bug-platform-quarkus-spike`、根 `META-INF/`，以及 Spring Native 交付物、`deploy/test`、`readme/spring-native-delivery`、前端 `vue.config.js`。代码生成器 `/tool/gen` 与若依示例 `test/user` 随之永久移除，不迁移。
- 改名：
  - 目录 `cat2bug-platform-quarkus-{admin,system,framework,common}` → `cat2bug-platform-{admin,system,framework,common}`（`git mv`）。
  - artifactId `cat2bug-quarkus-*` → `cat2bug-*`；`quarkus.application.name=cat2bug-admin`。
  - Java 包 `com.cat2bug.quarkus.*` → `com.cat2bug.*`，只精确替换 `com.cat2bug.quarkus.` 与 `com/cat2bug/quarkus/` 两种形式，覆盖 mapper XML、`application*.properties`、Native 反射/Feature 配置、操作日志规则表、`META-INF/services`。
  - 根 `pom.xml` 默认 `<modules>` 即这 4 个模块，删除 `quarkus` profile，不再需要 `-Pquarkus`。
  - 脚本：`build-embedded.sh`、`build-native.sh`、`run-jar.sh`、`deploy/docker/build.sh`、`deploy/ci/native.yml`；前端只保留 `build:embedded`（输出 `cat2bug-platform-admin/src/main/resources/META-INF/resources`）。
- 顺序为「tag → 删 Spring → 目录与 artifact 改名 → 包名改名 → 交付链修正」，每步单独提交：`fcaab5d5`、`82440389`、`4608c36e`、`543c1085`。
- Docker Compose 与 rpm 改用新 admin 产物：Compose 跑 MySQL 版单 JAR，rpm 打包 Native ELF，见 [[部署]]。
- 不改名的部分：OpenSpec capability 目录（如 `quarkus-backend-parity`）是规格标识，保留；`quarkus.*` 配置键、`io.quarkus` 等第三方标识不在改名范围。

## 原因

- Spring 线已不再接收修复，作为「回滚线」名不副实；真正可靠的回滚手段是 git：`git revert` 对应提交，或从 `spring-final-1.1.0` 检出。
- 默认构建即产品构建，消除「改错线」「漏加 profile」这类错误来源。
- 去掉技术栈后缀后，模块名只表达职责，和框架解耦。

## 副作用

- 回滚只能靠 tag 和 `git revert`，不再有可直接替换部署的 Spring JAR；tag 只在本地，换机器或重新克隆前需要自行推送或备份。
- `.worktree/` 中 8 个基于旧结构的分支合并会大面积冲突（已接受），建议先 rebase 到 `spring-final-1.1.0`，再处理改名提交。
- 去掉 Spring BOM 后传递依赖版本会漂移，根 pom 补了 `commons-pool2 2.12.1`、`jakarta.websocket-client-api 2.1.1` 的版本管理，见 [[去掉Spring-BOM后传递依赖版本漂移]]。
- 改目录名必须先改 `.gitignore`，否则含密码的 `config/install` 会变成未跟踪文件，见 [[改目录名后gitignore旧规则失效导致敏感配置暴露]]。
- 包结构变更必须同步 Native 反射前缀，见 [[Native镜像与操作日志对等]]。
- 操作日志规则表 `oper-log-rules.tsv` 原本由 Spring `@Log` 生成，此后只能手工维护。
- 安装向导预填原沿用 `SPRING_DATASOURCE_DRUID_MASTER_*` 等变量名；2026-10-14 起改为优先读 `CAT2BUG_SETUP_*`，旧名保留为兼容回退（见 [[安装向导预检与环境变量预填]]）。
