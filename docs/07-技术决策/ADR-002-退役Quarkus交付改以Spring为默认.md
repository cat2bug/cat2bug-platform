# ADR-002 退役 Quarkus 交付，改以 Spring 为默认

记录 2026-07 曾经正式退役 Quarkus 交付线的决定，便于理解后来为什么又恢复。

- 日期：2026-07-22
- 状态：已废弃，被 [[ADR-003-恢复Quarkus为默认后端-Spring保留为回滚线]] 取代

## 背景

[[ADR-001-Spring-Native作为单文件交付线]] 之后，Quarkus 模块只剩对照用途；新功能（Git 源码仓库、报告预置模版等）只在 Spring 上实现。仓库里残留的 Quarkus 文档、脚本容易让人误以为它是交付路径。依据：`openspec/changes/archive/2026-07-22-preset-report-templates-retire-quarkus/design.md` D7、`openspec/specs/retire-quarkus-delivery/spec.md`。

## 候选方案

当时未记录，待补充。

## 最终决定

- 文档与脚本默认后端改为 `cat2bug-platform-admin`（Spring Boot）；删除或改写 `deploy/*quarkus*`、`build-embedded-quarkus`、`CLAUDE.md` Quarkus 段、前端 `build:embedded-quarkus`。
- Maven 默认构建不依赖 Quarkus 模块；报告预置资源只打包到 Spring admin。
- 提交 `fd49e2f2`（2026-07-20）移除未使用的 Quarkus 对照模块。

## 原因

Quarkus 线不再被维护，功能已落后主线；保留会造成「双栈」误解。

## 副作用

- 后续恢复 Quarkus 时，需要以历史分支 `4598a68f` 为基线重新接入，并把 Git 等 Spring 独有能力补齐。
- 该规格（`retire-quarkus-delivery`）已被 `restore-quarkus-full-delivery` 用 delta 推翻。
