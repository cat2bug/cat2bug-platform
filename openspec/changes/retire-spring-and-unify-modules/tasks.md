## 1. 留档与准备

- [x] 1.1 确认工作区干净（除本变更与 `docs/` 外无未提交改动），记录当前 HEAD
- [x] 1.2 打 tag `spring-final-1.1.0` 指向删除前 HEAD（不推送）
- [x] 1.3 改名前检查 4 个 Quarkus 模块间无同名全限定类、无 `com.cat2bug.<sub>` 与第三方包冲突

## 2. 删除 Spring 线

- [x] 2.1 `git rm` 9 个 Spring 模块、`cat2bug-platform-quarkus-spike`、根 `META-INF/`
- [x] 2.2 删除 Spring 专属交付物：`deploy/build-native-spring.sh`、`deploy/ci/spring-native.yml`、`deploy/ci/quarkus-spike-native.yml`、`deploy/docker/native-spring-minimal/`、`build-cat2bug-spring-upx-image.sh`、`run-native-spring-minimal.sh`、`deploy/rpm/cat2bug/build-rpm-spring.sh`，以及其余仅服务 Spring 的 `deploy/scripts`、`deploy/test` 脚本（逐个确认）
- [x] 2.3 删除 `readme/spring-native-delivery/` 与 `cat2bug-platform-ui/vue.config.js`
- [x] 2.4 根 `pom.xml`：默认 `<modules>` 改为 4 个 Quarkus 模块（暂用旧目录名），删除 `quarkus` profile 包装与仅 Spring 使用的版本属性/依赖管理；`mvn -o -DskipTests install` 通过
- [x] 2.5 提交「删除 Spring 后端」

## 3. 目录与 artifactId 改名

- [x] 3.1 `git mv cat2bug-platform-quarkus-{admin,system,framework,common}` → `cat2bug-platform-{admin,system,framework,common}`
- [x] 3.2 各模块与根 `pom.xml` 的 `<module>`、`<artifactId>`、`<name>`、模块间依赖改为 `cat2bug-{admin,system,framework,common}`；`quarkus.application.name=cat2bug-admin`
- [x] 3.3 部署脚本改名并更新内部路径：`build-embedded.sh`、`build-native.sh`、`run-jar.sh`、`deploy/docker/build.sh`、`deploy/ci/native.yml`；`dev.sh`、`smoke-native-docker.sh` 内路径更新
- [x] 3.4 `.gitignore`（如 `config/install/`、`META-INF/resources` 产物规则）中旧目录名改为新目录名
- [x] 3.5 `mvn -o -DskipTests install` 通过后提交「模块去 quarkus 命名」

## 4. Java 包名改名

- [ ] 4.1 各模块 `src/{main,test}/java/com/cat2bug/quarkus/*` 用 `git mv` 上移到 `com/cat2bug/*`
- [ ] 4.2 已跟踪文本文件中 `com.cat2bug.quarkus.` → `com.cat2bug.`、`com/cat2bug/quarkus/` → `com/cat2bug/`（含 mapper XML、`application*.properties`、`META-INF/services`、`NativeRuntimeInitFeature` 前缀、操作日志规则表）
- [ ] 4.3 grep 断言：`openspec/changes/archive/**` 以外不再出现 `com.cat2bug.quarkus`、`cat2bug-platform-quarkus-`、`cat2bug-quarkus-`
- [ ] 4.4 `unset QUARKUS_CONFIG_LOCATIONS CAT2BUG_INSTALL_CONFIG_PATH` 后 `mvn -o install`（含测试）全部通过
- [ ] 4.5 提交「Java 包名去 quarkus」

## 5. 交付链修正

- [ ] 5.1 前端：`package.json` 只保留 `build:embedded`（输出 `../cat2bug-platform-admin/src/main/resources/META-INF/resources`），合并 `.env.embedded-quarkus` 到 `.env.embedded`；`vite.config.js` 去掉 Spring `static` 分支
- [ ] 5.2 `scripts/report-preset/*` 与 `scripts/sync-report-preset-templates.sh` 改写入 `cat2bug-platform-admin/src/main/resources/报告/模版/`，跑一次同步确认无差异
- [ ] 5.3 Docker：`after-dockerfile` 改为运行 admin fast-jar，删除 `--spring.*` 参数；`docker-compose.yml` 环境变量改为后端实际读取的变量名，`cat2bug.profile` 与卷挂载点统一
- [ ] 5.4 rpm：`build-rpm-spring.sh` 改为 `build-rpm.sh` 打包 Native ELF，`application.properties` 模板改为新配置键，`smoke-install.sh` 路径更新
- [ ] 5.5 JVM 冒烟：`build-embedded.sh` + `run-jar.sh` 启动后登录、打开项目、Git 仓库浏览、报告预制模版可用
- [ ] 5.6 Compose 冒烟：起栈 → 安装 → 上传附件 + 建 Git 仓库 → 重建后端容器后数据仍在
- [ ] 5.7 提交「交付链改用统一 admin」

## 6. Native 验证

- [ ] 6.1 `deploy/build-native.sh` 重建 Native，记录 UPX 前后体积
- [ ] 6.2 `smoke-native-docker.sh arm64` 与 `FRESH=true` 两种冒烟通过
- [ ] 6.3 浏览器回归：缺陷/用例/计划/报告/文档/代码仓库/系统管理主要页面

## 7. 内部资料与公开发布

- [ ] 7.1 `.gitignore` 删除 `/docs/` 规则，保留 Obsidian 个人工作区忽略；确认 `.cursor/`、`.claude/`、`.github/`、`assets/`、`CLAUDE.md` 仍忽略
- [ ] 7.2 新增 `deploy/public-release-exclude.txt`（`docs/`、`openspec/`、`readme/core/` 及确认后的其它内部目录）
- [ ] 7.3 新增 `deploy/scripts/export-public.sh`：临时索引生成剔除后的提交，增量挂到 `public/master`，树级自检，打印推送命令且不推送
- [ ] 7.4 运行导出脚本，`git ls-tree -r public/master` 核对无内部路径；再提交一次后重跑验证增量父提交

## 8. 文档

- [ ] 8.1 `README.md`、`deploy/README.md`、`deploy/docker/README.md` 改为单后端与新脚本名
- [ ] 8.2 本机 `CLAUDE.md`（不入库）同步：技术栈、模块、命令、Native 体积

## 9. 本机清理（不入库）

- [ ] 9.1 删除根 `node_modules`、`tmp-pw-*`（含 zip）、`cat2bug-platform-automation`、`cat2bug-platform-cloud`
- [ ] 9.2 删除 `backup/`、`venv/`、`logs/`、`uploadPath/`；`.worktree/`、`data/`、`upload/`、`config/` 保留

## 10. 回写长期知识

- [ ] 10.1 新增 `ADR-010-移除Spring后端并统一模块命名`（ADR-003 状态改为「部分被 ADR-010 取代」）与 `ADR-011-内部资料入私有库公开发布按清单剔除`，加入 `07-技术决策/说明.md` 与总览
- [ ] 10.2 `docs/02`～`05` 全文替换模块路径、包名、脚本名、构建命令；删除仅描述 Spring 线的段落；总览技术栈去掉「回滚/对照后端」
- [ ] 10.3 实施中遇到的可复用坑写入 `docs/06-踩坑记录/`（没有则注明无）
- [ ] 10.4 `docs/08-版本记录/2026-10.md` 记录本变更；跑 `check_links.py` 坏链为 0
