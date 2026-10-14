## 1. 准备

- [x] 1.1 打 tag 记录升级前版本（如 `quarkus-3.15-final`），记录当前全量测试数、两种 Native 冒烟结果和 UPX 前后体积作为基线
- [x] 1.2 确认目标版本：Quarkus 3.27.x 最新补丁版，以及与之兼容的 `quarkus-mybatis` 2.4.x 和 `quarkus-mcp-server-http` 1.9.x 版本号
- [x] 1.3 通读 Quarkus 3.16～3.27 的迁移指南，列出本项目用到的扩展中改名、废弃或默认值变化的配置项和 API

## 2. 升级与修复

- [x] 2.1 修改根 `pom.xml` 的 `quarkus.platform.version` 与 admin 的 `quarkus.mybatis.version`，修复编译错误
- [x] 2.2 按 1.3 的清单调整 `application*.properties`，旧键在新版本不再识别时补启动期兼容映射
- [x] 2.3 跑全量测试（先 `unset QUARKUS_CONFIG_LOCATIONS CAT2BUG_INSTALL_CONFIG_PATH`），修复失败用例，结果与 1.1 基线对比
- [x] 2.4 新增测试：用 3.15.7 版本写出的 `quarkus-runtime.properties`（H2 与 MySQL 两种）启动并登录成功

## 3. MCP 扩展引入验证

- [ ] 3.1 临时引入 `quarkus-mcp-server-http`，加一个最小工具，确认 JVM 启动、`/mcp` 可被 MCP 客户端列出工具
- [ ] 3.2 同一配置做一次 Native 构建，确认可构建可启动；记录结论后移除临时依赖与代码

## 4. 交付回归

- [ ] 4.1 更新 `deploy/build-native.sh` 中的 Mandrel 构建镜像版本（如需要），重建 arm64 Native，记录 UPX 前后体积并与基线对比
- [ ] 4.2 `smoke-native-docker.sh arm64` 默认模式与 `FRESH=true` 两种冒烟通过
- [ ] 4.3 JVM 单 JAR（H2 与 MySQL profile）、Docker Compose 构建并启动，安装向导、登录正常
- [ ] 4.4 浏览器回归主流程：登录、项目切换、缺陷新建与流转、用例、报告、Git 克隆与推送、在线浏览、通知、WebSocket 消息

## 5. 回写长期知识

- [ ] 5.1 更新 `docs/00-项目总览.md`、`docs/02-系统架构/整体架构.md` 中的 Quarkus 版本号；`docs/02-系统架构/部署.md` 如构建镜像变化同步修改
- [ ] 5.2 升级中遇到的可复用问题写入 `docs/06-踩坑记录/`（没有则注明无）；`docs/08-版本记录/2026-10.md` 记录本变更与新的 Native 体积
- [ ] 5.3 如 Native 体积口径或交付形态被迫改变，补 ADR（没有则注明无）；`check_links.py` 坏链为 0
