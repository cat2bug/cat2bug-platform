## Why

`restore-quarkus-full-delivery` 归档后，Quarkus 已是默认后端并与 Spring 功能全量对等，且 Quarkus 模块不依赖任何 Spring 模块。仓库里仍并存两套后端（9 个 Spring 模块 + 4 个 `*-quarkus-*` 模块 + 废弃 spike），默认 Maven 构建跑的却是 Spring、Quarkus 要加 `-Pquarkus`；新功能/修复只在 Quarkus 做，Spring 线持续腐化（如 Git 管理员放行、`defect_ids` LIKE 修复都未同步），维护和认知成本高。同时内部资料（`docs/` 知识库、`openspec/`、`readme/` 内部计划）与开源发布没有边界：`docs/` 被 gitignore 无法入私有库，而 `openspec/`、`readme/` 又会随 `core` 推到公开库。

**推翻已有决策**：本变更推翻 `docs/07-技术决策/ADR-003-恢复Quarkus为默认后端-Spring保留为回滚线.md` 中「Spring 保留为回滚线」部分，新增 ADR-010 取代之；回滚能力改由删除前打的 git tag 承担。

## What Changes

- **BREAKING** 删除 Spring 后端全部模块：`cat2bug-platform-{admin,framework,system,common,api,ai,im,quartz,generator}`，以及 `cat2bug-platform-quarkus-spike`、根目录 `META-INF/`（仅 Spring Native 使用）。删除前打 tag `spring-final-1.1.0`。
- **BREAKING** 删除 Spring 专属交付物：`deploy/build-native-spring.sh`、`deploy/ci/spring-native.yml`、`deploy/ci/quarkus-spike-native.yml`、`deploy/docker/native-spring-minimal/`、`build-cat2bug-spring-upx-image.sh`、`run-native-spring-minimal.sh`、`deploy/rpm/cat2bug/build-rpm-spring.sh` 等，以及 `readme/spring-native-delivery/`。代码生成器 `/tool/gen` 与若依示例 `test/user`（仅 Spring 有、菜单已隐藏）随之永久移除。
- **BREAKING** 模块去 `quarkus` 命名（C 级）：
  - 目录 `cat2bug-platform-quarkus-{admin,system,framework,common}` → `cat2bug-platform-{admin,system,framework,common}`
  - artifactId `cat2bug-quarkus-*` → `cat2bug-*`（产物 jar 名随之变化）
  - Java 包 `com.cat2bug.quarkus.*` → `com.cat2bug.*`（含 mapper XML namespace/resultType、`application*.properties`、Native 反射/Feature 配置、操作日志规则表、`META-INF/services`）
  - 根 `pom.xml` 默认 `<modules>` 即为这 4 个模块，去掉 `quarkus` profile；`quarkus.application.name` 改为 `cat2bug-admin`
- 部署脚本去 `quarkus` 后缀并改指新路径（如 `build-embedded-quarkus.sh` → `build-embedded.sh`、`build-native-quarkus.sh` → `build-native.sh`、`run-quarkus-jar.sh` → `run-jar.sh`）；前端 `build:embedded` 只保留一种并输出到 admin 的 `META-INF/resources`；删除遗留 `vue.config.js`。
- Docker Compose 后端镜像与 rpm 打包从 Spring fat JAR 改为 Quarkus 产物，并修正上传/Git 数据卷与 `cat2bug.profile` 路径不一致的问题。
- 预制报告模版同步脚本 `scripts/report-preset/*` 改写入新 admin 资源目录。
- 内部资料与开源边界：`.gitignore` 取消忽略 `docs/`，`docs/`、`openspec/`、`readme/` 内部部分照常提交到私有库 `private-gitee`；新增「公开发布导出」脚本，生成剔除内部路径的发布提交，供推送到 `github` / `public-gitee`（推送仍需人工执行）。`.cursor/`、`.claude/`、`.github/`、`assets/`、`CLAUDE.md` 保持不入库（已忽略，不改写历史）。
- 本机清理（不入库，实施时执行）：删除根 `node_modules`、`tmp-pw-*`、`cat2bug-platform-automation`、`cat2bug-platform-cloud`、`backup/`、`venv/`、`logs/`、`uploadPath/`；`.worktree/` 不动。

## Capabilities

### New Capabilities

- `single-backend-layout`：仓库只保留一套后端，模块/artifact/包名无技术栈后缀，默认 Maven 构建即产品构建；Spring 线以 tag 留档。
- `public-release-export`：内部资料入私有库、公开发布时按清单剔除的导出机制。

### Modified Capabilities

- `quarkus-backend-parity`：默认后端改为 `cat2bug-platform-admin`（无 profile）；不再允许 Spring 模块留在仓库；对等基线改为 tag `spring-final-1.1.0`。
- `quarkus-native-slim-binary`：Gate B 脚本路径改为 `deploy/build-native.sh`。
- `frontend-vite-build`：embedded 构建输出改为 admin 的 `src/main/resources/META-INF/resources/`。
- `report-template-preset-groups`：预制模版打包位置描述去掉「Spring admin」。
- `backend-prod-packaging-slim`：去掉 Spring fat JAR / api-only / Spring Native 专属要求，保留 static 卫生与 tool 路由剔除要求并改为新产物。
- `spring-native-embedded-delivery`、`spring-native-graal-hints`、`retire-quarkus-delivery`：全部需求移除（能力随 Spring 线下线）。

## Impact

- 代码：删除约 1.3k 个已跟踪文件；Quarkus 线约 900 个文件路径/包名变化（739 个文件含 `com.cat2bug.quarkus`）。
- 构建/交付：jar 名、部署脚本名、Docker 镜像构建方式变化；需重跑全量测试、重建 Native 并做两种冒烟与浏览器回归。
- 协作：`.worktree/` 中 8 个基于旧结构的 feature 分支合并时将大面积冲突（已接受）。
- 数据：删除 `uploadPath/` 后开发库中的附件、头像、项目 Git 仓库引用失效，需要重建或接受开发数据损坏。
- 文档：`docs/`（ADR-010、架构/部署/方案/接口路径）、`README.md`、`CLAUDE.md`、`.gitignore` 同步更新。
