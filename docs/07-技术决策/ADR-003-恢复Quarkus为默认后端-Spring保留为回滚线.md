# ADR-003 恢复 Quarkus 为默认后端，Spring 保留为回滚线

记录 2026-10 再次以 Quarkus 为默认后端、并保留 Spring admin 作对照/紧急回滚的决定。

- 日期：2026-10-14
- 状态：部分被 [[ADR-010-移除Spring后端并统一模块命名]] 取代（「Spring 保留为回滚线」部分失效：Spring 后端已于 2026-10-14 删除，回滚改由 tag `spring-final-1.1.0` 承担；「Quarkus 为默认后端」仍有效）

## 背景

产品需要更好的冷启动和常驻内存，并最终交付「内嵌前端的 Native 单文件」（历史目标约 20 MB）。Quarkus 曾全量迁移又被退役（[[ADR-002-退役Quarkus交付改以Spring为默认]]），Spring Native 体积偏大（UPX 后约 84 MB）。在 AI 辅助改写大幅降低迁移成本的前提下重新评估。依据：`openspec/changes/archive/2026-10-14-restore-quarkus-full-delivery/proposal.md`、`design.md`。

## 候选方案

### 方案 A：以历史 Quarkus 树（`4598a68f`）为基线，向当前 core 对齐

优点：已有 Phase 3–4 与 Native 经验，成本低于重写。

缺点：基线早于 Git 源码管理等能力，需大量补齐；双栈并存有心智负担。

### 方案 B：从零重写 Quarkus

缺点：成本更高，未采纳。

### 方案 C：绞杀式 AI 侧车（只把部分能力放 Quarkus）

缺点：与「全量 Quarkus / 界面一致」目标不符，否决。

### 方案 D：Git 能力以 Spring 微服务形式运行时调用

缺点：违背单交付二进制目标，否决。

## 最终决定

- 恢复 `cat2bug-platform-quarkus-{common,framework,system,admin}`，通过 Maven `-Pquarkus` 接入，Quarkus 平台版本 3.15.7，模块版本 1.1.0。
- 与 Spring 主线**功能与 UI 契约完全一致**（API 路径、鉴权、安装向导、H2/MySQL、Git、Excel 等）；Excel 功能不得裁减，底层库自选（最终全部改为 FastExcel）。
- 分 Gate A（JVM 人工验收）/ Gate B（Native）两阶段，见 [[ADR-004-Native交付分两道门并接受约38MiB]]。
- Spring admin 不删代码，降级为「对照 / 紧急回滚」；`CLAUDE.md`、`README` 默认改为 Quarkus（`dev.sh`、`deploy/run-quarkus-jar.sh`、`deploy/build-embedded-quarkus.sh`）。
- 提交 `6115f189`（恢复并全量对等）、`59f6a23e`（排序、CSV、Git 管理员权限等补齐）。

## 原因

见背景：启动/内存/体积诉求，以及 AI 辅助下迁移成本可控；以历史树为基线复用已验证的 Native 经验。

## 副作用

- 两条后端并存：Spring 侧修复不会自动同步到 Quarkus，反之亦然（例如 [[系统管理员在Git模块无权限]] 目前只在 Quarkus 修了）。
- Quarkus 移植中暴露的对等问题见 [[计划项defect_ids是CSV却用了JSON处理器]]、[[排序列不在SELECT导致SQL报错]]。
- 回滚路径：继续用 Spring `cat2bug-platform-admin` JAR/Native；整仓时间点可切回 `backup/core-before-rollback-20261014`。（2026-10-14 起失效：Spring 线已删除，`cat2bug-platform-admin` 现为 Quarkus 入口，回滚改用 tag `spring-final-1.1.0`，见 [[ADR-010-移除Spring后端并统一模块命名]]。）
- 待确认：当前 `core` 相对 `backup/core-before-rollback-20261014` 做过回滚，回滚分支中的部分功能（如 [[ADR-007-文档正文存每项目隐藏Git仓库]]）在当前代码里未找到。
