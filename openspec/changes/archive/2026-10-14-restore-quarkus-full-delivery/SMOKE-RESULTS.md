# Gate A 冒烟结果（JVM）

记录时间：2026-10-14（全模块回归后）  
产物：`cat2bug-platform-quarkus-admin/target/quarkus-app/`（fast-jar）  
冒烟库：H2 `cat2bug-platform-quarkus-admin/data/smoke_h2`  
配置：`/tmp/cat2bug-quarkus-smoke/application-install.yml` + `quarkus-runtime.properties`

## API 冒烟

最终一轮前端契约对齐：**44/44 PASS**（含 dashboard/actions、report/template/picker、notice 创建、团队/项目 invite-link、Git my-access、Excel 导出/用例模板）。

| 类别 | 结果 |
|---|---|
| 登录 / getInfo / getRouters / path 解析 | PASS |
| 团队/项目/成员/角色 | PASS |
| 缺陷/用例/计划/交付物/报告/文档/公告/API | PASS |
| Dashboard（含 `/actions`） | PASS |
| Git list/get/browse/pulls/releases/my-access/invites | PASS |
| 系统 user/role/menu/post/dict/config/ScreenSize/AI account | PASS |
| 监控 operlog/logininfor + JSON 导出 | PASS |
| admin team/project | PASS |
| IM config / 钉钉飞书企微 | PASS |
| 成员邀请 TEAM/PROJECT + preview | PASS |
| Excel 导出 + case importTemplate | PASS |

## UI 浏览器验收（:2222）

| 模块 | 路径 | 结果 |
|---|---|---|
| 仪表盘 | `/{team}/{project}/dashboard` | PASS（actions 500 已修） |
| 缺陷/用例/计划/交付物/文档/源码 | namespaced 模块 | PASS |
| 报告 | `.../report` | PASS（picker 404 已修） |
| 项目设置/成员 | `.../option`、`.../member` | PASS |
| 团队基本信息 | `/team-options/team-base-info` | PASS |
| 角色 | `/admin/role` | PASS |
| 菜单 | `/system/menu` | PASS（Vite `requireIcons` 已修） |
| 操作/登录日志 | `/system/log/operlog`、`logininfor` | PASS |
| 后台团队/项目 | `/admin/team`、`/admin/project` | PASS |

说明：动态菜单里角色在 `/admin/role`，日志在 `/system/log/*`（不是 `/system/role`、`/monitor/operlog`）。

## 本轮修复摘要

1. `SysNoticeService`：空 `noticeId` 时生成 UUID  
2. 成员邀请：Quarkus 完整移植 TEAM/PROJECT/REPO + 公开 preview/accept  
3. Dashboard `actonList`：`sys_case.update_by`（用户名）解析为 `user_id`，避免 H2 类型转换 500  
4. 报告模版 `picker` + classpath 预制资源移植  
5. 前端 `IconSelect/requireIcons.js`：`require.context` → `import.meta.glob`（Vite）

## Gate A 人工验收

**已通过**（2026-10-14，产品负责人手动确认）。

## Gate B（Native，2026-10-14）

产物：`cat2bug-admin-linux-arm64.upx`（对等补齐后 ≈38.8 MiB；前端瘦身后 ≈38.4 MiB、FastExcel 版 ≈46.6 MiB、POI 版 ≈60 MiB 已淘汰），在 Docker `ubi9/ubi-minimal` 中运行，使用 `smoke-native-docker.sh`。下表为对等补齐后的最新一轮结果。

| 检查项 | 已有库（Gate A smoke 库） | 全新安装（FRESH=true） |
|---|---|---|
| health / SPA `/` / `/version`（1.1.0） | OK | OK |
| 安装预检 `/setup/preflight` | — | OK |
| `/setup/submit`（H2）→ Flyway 34 个迁移 → 重启 | — | OK |
| 登录 / getInfo / getRouters | OK | OK |
| 项目、缺陷、用例、模块、计划、公告列表 | OK | OK |
| 报告模版 picker | OK | OK |
| Git 仓库列表 | OK | SKIP（新库无项目） |
| 用例 Excel 导出 / 导入模板 / 导入 | OK / OK / OK | OK / OK / SKIP |
| 缺陷 Excel 导出 / 导入模板 | OK / OK | OK / SKIP（模板需项目） |
| 用户 Excel 导出 / 导入模板 | OK / OK | OK / OK |
| 缺陷状态走势、成员走势导出（含折线图） | OK（各 1 张图） | OK（无数据行，按原逻辑不画图） |
| 测试计划统计导出（4 张柱图） | OK（4 张图） | — |
| 监控：在线用户、服务器、缓存、操作日志、登录日志、定时任务/调度日志列表 | OK | OK |
| 数据监控页 `/druid/login.html`（Agroal） | OK | OK |
| 定时任务新增 → 执行一次 → 调度日志成功 → 删除 | OK | OK |

修复详情见 `NATIVE-RELEASE.md`。

列表排序对等补齐后：JVM 回归 161 个用例全部通过（1 个跳过）；本地开发库真实数据下，10 个列表 × 前端全部可排序列 × 升/降序逐一请求均返回 200，浏览器中缺陷页（默认更新时间倒序、点击缺陷名称升序）、用例页（等级）请求参数与返回顺序正确；Native 重建后两种冒烟均通过，新增排序与排序注入拦截检查。

对等补齐后 JVM 回归：151 个用例全部通过（1 个需环境变量的 MySQL 全量迁移测试默认跳过，单独设置变量后通过）。测试已改用独立库 `cat2bug_platform_test`，不再写入开发库。

对等补齐后浏览器回归（Quarkus JVM + 本地 MySQL/Redis）：在线用户、定时任务（新增、执行一次、启停、删除、调度日志）、数据监控、服务监控、缓存监控、缓存列表（名称/键/内容）、操作日志（列表与详情）、登录日志均正常。期间修复：Redis 中旧版 Spring 写入的 fastjson 会话被解析成空行（在线用户出现 186 条空记录）；JSON 日期按 UTC 输出导致所有时间偏 8 小时；`/version` 为 1.0.0（应为 1.1.0）。

JVM 回归（`mvn -Pquarkus -pl cat2bug-platform-quarkus-admin test`，本机 MySQL 8 + Redis）：131 个用例中 129 个通过（连续 3 次一致）；2 个失败均为本机 `cat2bug_platform` 库历史遗留（部分列排序规则为 `utf8mb4_0900_bin` 导致 dashboard actions 联表报错；迁移历史最后一条为乱序补跑的 0.6.2.19，导致“删除最后一条后应有待执行迁移”断言不成立），与代码无关。

前端瘦身后浏览器回归（Native 容器 + 已有库）：登录、语言切换与刷新后保持（非默认语言按需加载）、41 个菜单页面逐个打开无 chunk 加载/初始化错误、仪表盘 echarts 图表、报告模版编辑器（milkdown + md-editor-v3，代码块高亮）、jsPDF 动态加载生成 PDF、项目封面 SVG 均正常。顺带修复通知页「全部」分组 `$i18n.t('')` 在 vue-i18n 9 下抛 `Invalid argument`。

冒烟脚本修正：非 FRESH 模式也先清空数据目录，否则上一次 FRESH 运行留下的 `config/install/` 会覆盖注入的配置，Native 连到空库。

Native 冒烟中发现并修复：`/system/member/importTemplate`、`/system/user/importTemplate` 未声明 `@Consumes`，前端以表单 POST 下载模板时返回 415（已补 `@Consumes(WILDCARD)` 与测试）。

## 验收入口

```bash
# 后端（已配置 smoke 时）
java -Dquarkus.config.locations=file:///tmp/cat2bug-quarkus-smoke/application-install.yml,file:///tmp/cat2bug-quarkus-smoke/quarkus-runtime.properties \
  -Dcat2bug.install.config-path=/tmp/cat2bug-quarkus-smoke/application-install.yml \
  -jar cat2bug-platform-quarkus-admin/target/quarkus-app/quarkus-run.jar

# 前端开发代理
cd cat2bug-platform-ui && npm run dev
```

- UI：http://127.0.0.1:2222/  
- API：http://127.0.0.1:2020/  
- 账号：`admin` / `cat2bug`  
- 示例项目：`smoke-team2/smoke-project2`
