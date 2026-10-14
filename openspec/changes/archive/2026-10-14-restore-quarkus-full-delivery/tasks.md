## 1. Restore Quarkus modules and reactor

- [x] 1.1 Ensure `cat2bug-platform-quarkus-{common,framework,system,admin}` are present (baseline `4598a68f`) and parent version is `1.1.0`
- [x] 1.2 Wire root `pom.xml` `-Pquarkus` modules + `quarkus-maven-plugin` management; avoid duplicate `<profiles>` tags
- [x] 1.3 Restore Quarkus deploy helpers (`deploy/build-*-quarkus.sh`, `run-quarkus-jar.sh`, related CI) and point docs to them as preferred
- [x] 1.4 `mvn -Pquarkus -pl cat2bug-platform-quarkus-admin -am package -DskipTests` succeeds on JVM

## 2. JVM runtime and core parity

- [x] 2.1 Document/verify Quarkus application config (H2/MySQL, ports, profiles) for local run
- [x] 2.2 Start Quarkus admin JVM; confirm health/SPA entry and login smoke with known credentials or setup wizard
- [x] 2.3 Walk core SPA flows vs Spring: project switch, defect/case list, members, documents (as present on current core)
- [x] 2.4 Inventory Spring Excel import/export entry points; implement full Quarkus parity (library choice free; no feature cuts)
- [x] 2.5 Fix any API contract mismatches discovered in 2.3–2.4 without forking the Vue client unless unavoidable

## 3. Git / source management on Quarkus

- [x] 3.1 Inventory Spring Git APIs and SPA clients used by current `code/{slug}` routes; produce parity checklist
- [x] 3.2 Implement Quarkus resources/services/mappers for repository list + browse (minimum for UI open)
- [x] 3.3 Implement remaining wired Git flows (branches / commits / settings / PR / webhooks as required by SPA)
- [x] 3.4 Migrate or share Flyway scripts/data model pieces needed for Git tables on Quarkus
- [x] 3.5 Smoke Git UI against Quarkus JVM until checklist items pass

## 4. Docs and delivery narrative

- [x] 4.1 Update `CLAUDE.md` / `README.md` default backend quickstart to Quarkus; Spring marked optional rollback
- [x] 4.2 Note Gate A vs Gate B (Native deferred) in change notes or readme snippet
- [x] 4.3 Align `retire-quarkus-delivery` / Spring Native priority language with delta specs when archiving

## 5. Gate A — human acceptance

- [x] 5.1 Run automated smoke script/checklist and capture results
- [x] 5.2 Package JVM runnable artifact for the product owner
- [x] 5.3 Stop for **manual UI/功能验收**; do not start Native size work until owner confirms
- [x] 5.4 **Gate A 人工验收通过**（2026-10-14，产品负责人手动确认）

## 6. Gate B — Native slim binary (after approval only)

- [x] 6.1 Embedded SPA into Quarkus Native build pipeline（`build:embedded-quarkus` → META-INF/resources；`deploy/build-native-quarkus.sh`）
- [x] 6.2 Produce Native binary; record size (with UPX if used); iterate toward ~20MB（arm64：对等补齐后原始 169 MiB / UPX 后 ≈38.8 MiB；前端瘦身后 167 / ≈38.4 MiB（FastExcel 版 177 / ≈46.6 MiB），POI 版为 258 / ≈60 MiB；未达 ~20MB，偏差与后续方向见 `NATIVE-RELEASE.md`）
- [x] 6.3 Native smoke: start, SPA, login, one Git and one core business path（已有库 + 全新安装两种模式，`smoke-native-docker.sh`）
- [x] 6.4 Update release docs with final size and run commands（`NATIVE-RELEASE.md`）
- [x] 6.5 体积口径由产品负责人确认接受（≈38 MiB，不再强求 ~20MB）

## 7. 全量端点对比后的对等补齐

- [x] 7.1 系统监控：在线用户/强退、服务监控、缓存监控/缓存列表、登录日志（Redis 中旧版 Spring fastjson 会话不再显示为空行）
- [x] 7.2 定时任务 `monitor/job`、`monitor/jobLog`（增删改查、启停、执行一次、导出、清空；调用目标白名单/黑名单校验同 Spring）
- [x] 7.3 数据监控 `/druid/*`：Quarkus 无 Druid，改为同路径渲染 Agroal 连接池配置与指标（密码打码、需 `monitor:druid:list`）
- [x] 7.4 操作日志：按 Spring `@Log` 生成规则表 `oper-log-rules.tsv`，`OperLogFilter` 统一记录（密码字段脱敏、≥400 记失败）
- [x] 7.5 报告编辑历史、评论批注字段及缺失迁移脚本对等；MySQL 迁移目录改为 `db/mysql`（Native Flyway 扫描按前缀匹配，不能与 `db/migration` 同前缀）
- [x] 7.6 `GET /setup/preflight`、`user/config` 增删（非管理员只能操作本人）、`defect/log` 增改删、`POST /common/uploads`
- [x] 7.7 JSON 日期按 JVM 默认时区输出（与 Spring `ApplicationConfig` 一致，Quarkus 默认 UTC 会整体偏 8 小时）
- [x] 7.8 测试改用独立库 `cat2bug_platform_test`（启动时自动建库并迁移），不再写入开发库
- [x] 7.9 `cat2bug.version` 与 Spring 一致为 1.1.0；Native 冒烟增加监控、数据监控页、定时任务执行、安装预检检查
- [x] 7.10 去掉 Quarkus 分页每页 500 条上限（Spring PageHelper 无上限；前端多处 pageSize=9999 一次取全量，超过 500 条会被截断）
- [x] 7.11 列表排序对等：`orderByColumn`/`isAsc` 此前被 Quarkus 忽略（缺陷页默认按更新时间倒序也失效）。新增 `PageOrder`（驼峰转下划线、ascending/descending、忽略 `custom_` 列、与 `SqlUtil.escapeOrderBySql` 相同的白名单/长度校验）与 `PageOrderFilter`（非法参数返回「参数不符合规范，不能进行查询」），缺陷/用例/计划/计划项/计划缺陷/报告/文档/操作日志/登录日志 Mapper 改为「有排序参数用之，否则用原默认排序」；前端各表格可排序列逐列验证升降序
- [x] 7.12 计划项 `defect_ids` 格式对等：Spring 存逗号分隔（`,12,34`），Quarkus 误用 JSON 处理器导致关联过缺陷的计划其缺陷列表 500、且两种写法混写；改为 `LongCsvTypeHandler`（兼容已写入的 JSON）。同时修正计划缺陷列表只匹配末尾缺陷、统计中 id 前缀误匹配（如 1 匹配 12）的 LIKE 条件（Spring 同样存在）
- 刻意不迁：`tool/gen`（代码生成）、`test/user`（若依示例）——两边菜单父节点均隐藏，SPA 无入口

## 8. 回写长期知识

- [x] 8.1 ADR-003/004/008、14 篇踩坑、`docs/02`～`05` 架构/部署/方案/接口事实与 `docs/08-版本记录/2026-10.md` 已写入 `docs/`
