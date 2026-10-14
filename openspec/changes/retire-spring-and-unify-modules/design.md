## Context

- 现状：根 `pom.xml` 默认 `<modules>` 是 9 个 Spring 模块；产品默认后端 Quarkus 的 4 个模块只在 `-Pquarkus` profile 下构建。Quarkus 模块不依赖任何 Spring artifact（已核对各 `pom.xml`）。
- 仍指向 Spring 模块的已跟踪文件：根 `pom.xml`、`cat2bug-platform-ui/vite.config.js`（`build:embedded` 输出到 Spring admin `static`）、遗留 `vue.config.js`、`scripts/report-preset/*`（预制模版只写 Spring admin）、`deploy/` 下约 13 个 Spring/Spring Native 脚本、Docker `after-dockerfile`（`java -jar cat2bug-admin.jar --spring.*`）、rpm `build-rpm-spring.sh`、`readme/spring-native-delivery/`、若干主规格。
- 包名 `com.cat2bug.quarkus` 出现在 739 个已跟踪文件，除 Java 外还在 mapper XML、`application.properties`（`quarkus.mybatis.mapper-packages` 等）、`application-native.properties`（`--features=...NativeRuntimeInitFeature`）、`META-INF/services/...ConfigSource`。
- 三个推送地址：`private-gitee`（`core` 默认上游，私有）、`github`、`public-gitee`（公开）。`docs/` 当前被 `.gitignore:77` 忽略。
- `.worktree/` 下 8 个活跃 worktree 基于旧结构。

## Goals / Non-Goals

**Goals:**

- 仓库只剩一套后端；`mvn package` 无需 profile 即构建产品。
- 模块目录、artifactId、Java 包名不再出现 `quarkus` 技术栈后缀（框架依赖、`quarkus.*` 配置键、`io.quarkus` 等第三方标识除外）。
- 删除前可追溯：tag `spring-final-1.1.0` 指向删除前最后一个提交。
- Docker Compose / rpm / Native 交付全部基于新 admin 产物且冒烟通过。
- `docs/` 入私有库；公开发布有可重复的剔除机制。

**Non-Goals:**

- 不改写 git 历史（历史中出现过的 `.claude`、`.github`、`assets`、`CLAUDE.md`、`docs` 保留在历史里）。
- 不重命名 OpenSpec capability 目录（如 `quarkus-backend-parity`）——它们是规格标识，改名只增加归档噪声；正文路径随本变更更新。
- 不迁移代码生成器 `/tool/gen`、若依 `test/user`。
- 不处理 `.worktree/` 中各分支的合并（由各分支后续自行 rebase）。
- 不自动推送任何远端。

## Decisions

### D1 顺序：tag → 删 Spring → 改名 → 验证

改名后目标目录 `cat2bug-platform-admin` 等与 Spring 模块同名，必须先删再改。每一步单独提交（删除、目录与 artifact 改名、包名改名、交付链修正、文档），便于定位与 `git log --follow`。
- 备选：一次性大提交 → 难以审查、`--follow` 失效风险高，否决。

### D2 目录改名用 `git mv`，包名改名用「移动目录 + 全文替换」

- 目录：`git mv cat2bug-platform-quarkus-X cat2bug-platform-X`，保留重命名跟踪。
- 包：每个模块 `src/{main,test}/java/com/cat2bug/quarkus/<sub>` → `com/cat2bug/<sub>`（`git mv`），然后对已跟踪文本文件把 `com.cat2bug.quarkus.` 替换为 `com.cat2bug.`、`com/cat2bug/quarkus/` 替换为 `com/cat2bug/`。
- 替换后 grep 断言：除 `openspec/changes/archive/**` 外不再出现 `com.cat2bug.quarkus`、`cat2bug-platform-quarkus-`、`cat2bug-quarkus-`。
- 冲突检查：改名前确认 4 个模块的子包（`admin`、`system`、`framework`、`common`）之间无同名全限定类；因 Spring 模块已删，`com.cat2bug.*` 下不会与旧类冲突。
- 备选：IDE 重构 → 不覆盖 XML/properties/字符串配置，仍需全文替换，否决。

### D3 构建与命名

- 根 `pom.xml`：`<modules>` 直接列 4 个模块，删除 `quarkus` profile 及 Spring 专属 `dependencyManagement`/properties（Spring Boot BOM、PageHelper、Druid、knife4j 等仅 Spring 用的项，以实际编译为准逐项删）。
- artifactId：`cat2bug-quarkus-{admin,system,framework,common}` → `cat2bug-{admin,system,framework,common}`；`quarkus.application.name=cat2bug-admin`。
- 部署脚本：`build-embedded-quarkus.sh` → `build-embedded.sh`、`build-native-quarkus.sh` → `build-native.sh`、`run-quarkus-jar.sh` → `run-jar.sh`、`deploy/docker/build-quarkus.sh` → `build.sh`、`deploy/ci/quarkus-native.yml` → `native.yml`；`cat2bug-platform-quarkus-admin/{dev.sh,smoke-native-docker.sh}` 随目录迁移。
- 前端：`package.json` 只保留 `build:embedded`（指向原 `embedded-quarkus` 模式与输出 `../cat2bug-platform-admin/src/main/resources/META-INF/resources`），`.env.embedded-quarkus` 合并为 `.env.embedded`；删除 `vue.config.js`。
- 备选：保留脚本旧名做兼容软链 → 增加混乱，仓库内无外部调用方，否决。

### D4 Docker Compose 与 rpm 改用新 admin

- `after-dockerfile`：基于 JRE 17，复制 Quarkus fast-jar 目录（`target/quarkus-app/`）并以 `java -jar quarkus-run.jar` 启动；数据库/Redis 通过安装配置或 `CAT2BUG_*` 环境变量注入（删除 `--spring.*` 参数与 `SPRING_*`/`REDIS_HOSTS` 变量，改为 Quarkus 实际读取的变量名）。
- 卷：后端 `cat2bug.profile` 默认路径与卷挂载点统一为同一目录（`/home/uploadPath`），确保上传与 `git/` 子目录持久化。
- 前端 nginx 容器保持不变；`/prod-api` 前缀转发规则以 Compose 冒烟验证为准。
- rpm：`build-rpm-spring.sh` 改为 `build-rpm.sh`，打包 Native ELF（无 JRE），`application.properties` 模板改为 Quarkus 配置键。
- 备选：Compose 直接用 Native 镜像 → 需要多架构构建链，留作后续，否决为本次范围。

### D5 公开发布导出（`public-release-export`）

- 清单文件 `deploy/public-release-exclude.txt`，列出不公开路径：`docs/`、`openspec/`、`readme/core/` 等内部目录（`readme/production` 等用户文档保留公开）。
- 脚本 `deploy/scripts/export-public.sh`：基于 `core` 的 HEAD 树，用临时索引（`GIT_INDEX_FILE`）`read-tree` 后 `rm --cached` 清单路径，`write-tree` + `commit-tree` 生成一个提交，父提交为本地分支 `public/master` 的上一次导出（首次则为 `github/master`），并更新 `public/master`。不改工作区、不推送；输出推送命令供人工执行。
- 脚本自检：导出提交的树中不包含清单任一路径。
- 备选：`git filter-repo` 每次全量重写 → 慢且改写历史；`git subtree split` 只能按单目录拆，均否决。

### D6 内部文件入库策略

- `.gitignore` 删除 `/docs/` 规则；保留 `docs/.obsidian/workspace*` 等个人文件忽略。
- `.cursor/`、`.claude/`、`.github/`、`assets/`、`CLAUDE.md` 维持忽略。

### D7 本机清理

删除未跟踪/已忽略的本机目录：根 `node_modules`、`tmp-pw-*`（含 zip）、`cat2bug-platform-automation`、`cat2bug-platform-cloud`、`backup/`、`venv/`、`logs/`、`uploadPath/`。`.worktree/`、`data/`、`upload/`、`config/` 不动。删除 `uploadPath/` 后，开发库对应引用失效，验证阶段改用测试库或重新创建测试数据。

## Risks / Trade-offs

- [包名全文替换误伤非包名字符串] → 只替换 `com.cat2bug.quarkus.` 与 `com/cat2bug/quarkus/` 两种精确形式；替换后全量测试 + Native 构建 + 冒烟。
- [Native 反射/Feature 配置漏改导致运行期 ClassNotFound] → `application-native.properties` 与 `NativeRuntimeInitFeature` 前缀纳入 grep 断言；Native 两种冒烟（常规 + `FRESH=true`）必须通过。
- [MyBatis XML namespace 与接口不一致] → 依赖 Quarkus 启动期 mapper 校验 + 全量测试覆盖。
- [Docker Compose 配置变量名不匹配导致连不上库] → Compose 实际起一遍完成安装与登录冒烟。
- [worktree 分支合并大面积冲突] → 已接受；`spring-final-1.1.0` 与改名提交分开，便于分支先 rebase 到 tag 再处理改名。
- [删除 uploadPath 后开发数据损坏] → 已接受；验证用测试库。
- [公开导出漏剔内部文件] → 脚本树级自检；首次导出人工核对 `git show --stat`。

## Migration Plan

1. `git tag spring-final-1.1.0`（指向删除前 HEAD）。
2. 删除 Spring 模块与专属交付物 → 提交。
3. 目录 + artifactId + 根 pom + 脚本改名 → `mvn package` 通过 → 提交。
4. 包名改名 → 全量测试通过 → 提交。
5. Docker/rpm/前端/预制模版脚本修正 → JVM 冒烟、Compose 冒烟 → 提交。
6. Native 重建 + 两种冒烟 + 浏览器回归。
7. `.gitignore`、导出脚本、文档（ADR-010、docs、README、CLAUDE.md）→ 提交。
8. 本机清理。

回滚：`git revert` 对应提交，或从 `spring-final-1.1.0` 检出 Spring 线。

## Open Questions

- `readme/` 下哪些子目录属于内部资料需要剔除（初定 `readme/core/`，其余如 `readme/development/`、`readme/design-system/` 实施时逐个确认）。
- Compose 的 `/prod-api` 前缀由 nginx 剥离还是后端处理，以冒烟结果定。

## 相关长期文档

- `docs/02-系统架构/整体架构.md`、`docs/02-系统架构/部署.md`、`docs/02-系统架构/存储与Git仓库.md`
- `docs/03-技术方案/Native镜像与操作日志对等.md`
- `docs/06-踩坑记录/Maven单模块测试未加am用到旧jar.md`
- ADR：`docs/07-技术决策/ADR-003-恢复Quarkus为默认后端-Spring保留为回滚线.md`（将被部分取代）、`ADR-004-Native交付分两道门并接受约38MiB.md`
- 拟新增 ADR：`ADR-010-移除Spring后端并统一模块命名`、`ADR-011-内部资料入私有库公开发布按清单剔除`
