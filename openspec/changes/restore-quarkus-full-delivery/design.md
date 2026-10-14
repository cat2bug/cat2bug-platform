## Context

- 当前 `core` 在源码管理收尾点（约 `16b3383b`），默认交付为 **Spring Boot admin**，并具备 Spring Native 路径。
- 历史分支 `feature/quarkus-full-migration`（尖端 `4598a68f`）已有完整 Quarkus 四模块 + Native/RPM/embedded 脚本，但**早于**后续 Git 源码管理等能力；后经 `retire-quarkus-delivery` 退役。
- 产品决策：在 AI 辅助改写下再次以 Quarkus 为默认后端；要求与现 Spring 主线 **功能与 UI 契约完全一致**；Excel **功能不得少**（库选型自定）；Native ~20MB **在 JVM 人工验收通过后再做**。

## Goals / Non-Goals

**Goals:**

- 恢复 Quarkus 模块并成为默认可运行/可发布后端（JVM 优先）
- API 路径、鉴权语义、安装向导、双库、核心业务与前端操作路径与现 Spring 一致
- 补齐历史 Quarkus 缺失的 **Git/源码仓库** 等增量能力
- 预留 Native + embedded SPA 瘦身路径（~20MB），验收门禁后再实施

**Non-Goals:**

- 本 change 第一阶段不做 Native 体积优化与 UPX 压测（仅规划与脚本就位）
- 不强制删除 Spring admin 源码（可作对照），但不作为推荐发布入口
- 不强制 Excel 底层库与 Spring 相同（POI / FastExcel 等可选），但 **Excel 功能集合不得少于 Spring**
- 不借机做无关 UI 改版或产品功能新增

## Decisions

### D1: 以历史 Quarkus 树为基线，向当前 core 对齐（而非从零重写）

- **选择**：检出 `4598a68f` 的 `cat2bug-platform-quarkus-*`，在现仓库 `1.1.0` reactor 下以 `-Pquarkus` 接入，再按差距清单补 Git 等能力。
- **理由**：已有 Phase 3–4 与 Native 经验；重写成本更高。
- **备选**：绞杀式 AI 侧车 — 与「全量 Quarkus / 界面一致」目标不符，否决。

### D2: 分阶段门禁 — JVM parity → 人工验收 → Native

- **选择**：任务拆成 Gate A（JVM 功能）与 Gate B（Native 体积）。
- **理由**：用户明确要求先功能一致并人工确认，再做二进制。
- **备选**：并行 Native — 易在 parity 未稳时浪费构建时间，否决。

### D3: Git 能力迁入 Quarkus system/admin（契约对齐 Spring）

- **选择**：对照 `openspec/specs/project-git-repository` 等与 Spring Controller，在 Quarkus 增加 Resource/Service/Mapper（或共享可移植领域层），保证前端 `code/{slug}` 等路径可用。
- **理由**：历史 Quarkus 无 Git；当前 core 有 Git，parity 硬需求。
- **备选**：运行时调 Spring Git 微服务 — 违背单交付二进制目标，否决。

### D4: Excel 功能完整；库选型自定

- **硬约束**：凡 Spring 主线已提供的 Excel 导入/导出（含模板下载、批量导入校验提示等用户可见能力）在 Quarkus 上 MUST 全部具备，**禁止以“体积/Native 难度”裁功能**。
- **选型**：实现可选用 Apache POI、FastExcel 或组合方案；优先复用历史 Quarkus 已验证栈，若缺能力则补库或补代码直到与 Spring 功能对齐。
- **理由**：产品要求功能完全一致；库是工程细节，不是需求取舍。

### D5: 默认文档与脚本切换到 Quarkus

- **选择**：更新 `CLAUDE.md` / `README` / `deploy/*quarkus*`；明确 `mvn -Pquarkus` 与 `quarkus:dev` 为开发默认。
- **理由**：与推翻 `retire-quarkus-delivery` 一致。

### D6: 版本对齐

- **选择**：Quarkus 子模块 parent 使用根工程 `1.1.0`；Quarkus 平台版本沿用历史验证过的 `3.15.7`（除非构建强制升级）。
- **理由**：减少首次接入变量。

## Risks / Trade-offs

- **[Risk] 历史 Quarkus 与现 Spring 行为漂移** → 以 API 契约 + 关键用户路径冒烟清单做 parity 矩阵；优先登录、项目切换、缺陷/用例、文档、Git。
- **[Risk] Git 迁入工作量大** → 按前端实际调用的 API 切片迁移，先通浏览/列表再 PR/Webhook。
- **[Risk] Native 20MB 未必一次达标** → Gate B 单独迭代；Gate A 不绑定体积。
- **[Risk] 双栈并存混淆** → 文档标明默认 Quarkus；CI 主路径切 Quarkus JVM。
- **[Trade-off] 保留 Spring 模块** → 仓库体积与心智负担换取回滚对照。

## Migration Plan

1. 接入 Quarkus 模块与 `-Pquarkus`，JVM 编译通过  
2. 本地/嵌入式配置下启动，跑安装或既有数据登录冒烟  
3. 补齐 Git 等缺口直至 parity 清单打勾  
4. **人工验收**（用户）  
5. 再执行 Native + embedded + 体积优化  
6. 回滚：继续用 Spring `cat2bug-platform-admin` JAR/Native；或切回 `backup/core-before-rollback-*`（若需要整仓时间点）

## Open Questions

- Native 20MB 是否含 UPX 后体积（历史实践多为 UPX 后口径）— **默认按 UPX 后计**，在 Gate B 文档写明。
- Spring admin 是否在本 change 末从默认文档中完全移除运行说明 — **默认降级为「对照/紧急回滚」一节，不删代码**。
