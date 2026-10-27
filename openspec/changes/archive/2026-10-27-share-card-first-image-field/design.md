## Context

分享图卡 `DefaultDefectShareCard` 用 `params.imgUrls` + canvas `drawImage` 渲染；自定义 image 字段与系统「图片」共用缺陷属性管理的 `sortOrder`（见 `DefectBuiltinFieldRegistry` 中 `imgUrls` 类型为 `image`）。工作台 `DefectsPendingRepair.rowImages` 已按启用图片字段枚举并合并 URL，但语义是「全部合并」，与本次「第一个有可用图的字段」不同。

## Goals / Non-Goals

**Goals:**

- 按属性管理统一 `sortOrder` 选择第一个「已启用 + image 类型 + 至少一张可加载」的字段，渲染该字段成功图。
- 404 / 加载失败视为无图，继续下一字段；脏路径（如 `[object Event]`）过滤掉。
- 无可用图时不展示空黑块（隐藏图片区）。

**Non-Goals:**

- 不改分享匿名页 `DefectShardInfo` 的其它区块（除非同样硬编码 `imgUrls` 且与本次同逻辑强相关——默认不动）。
- 不改后端字段 API、不改上传存储。
- 不合并多个图片字段的图；不探测 file 类型字段。

## Decisions

1. **字段列表来源**  
   用项目已启用字段列表（`listEnabledFields` 或列布局中等价数据），过滤 `fieldType === 'image'`，按 `sortOrder` 升序、`fieldKey` 次序打平。系统 `imgUrls` 与自定义 key（如 `img`）同一队列。  
   *备选*：只扫 `customFields` 再特判 `imgUrls`——拒绝，因排序轴是统一的。

2. **取值**  
   - `fieldKey === 'imgUrls'` → `row.imgUrls`  
   - 其它 → `row.customFields[fieldKey]`，经 `urlListFromCustomFieldValue` + `resolveCommaSeparatedUrls`。

3. **「有图」判定**  
   对该字段 URL 列表逐张用 `Image()` 探测（或等价 onload/onerror）；至少一张成功即选中该字段，只把成功 URL 交给现有 canvas 绘制。全部失败 → 下一字段。  
   *备选*：仅看路径非空不探测——拒绝，已确认 404 算没图。

4. **禁用字段**  
   未启用（`enabled` 关）的 image 字段不参与竞争。

5. **放置位置**  
   抽小工具函数（如 `resolveShareCardImageUrls(row, imageFields)`）供 `DefaultDefectShareCard` 调用，便于与工作台逻辑对照单测/手工验证；不在 ShareCard 入口重复写。

6. **异步与 loading**  
   在 `refresh` → `drawImage` 路径上 await 探测；可与弹层 `apiLoading` 并行或紧随其后，结束后 `schedulePopperUpdate`。

## Risks / Trade-offs

- [多图字段逐张探测变慢] → 可并行探测同一字段内 URL，字段之间串行（保持「第一个」语义）。
- [跨域导致 onerror] → 同源 `/dev-api` 或已配置 CORS 的 profile 路径；失败则跳过该 URL，与 404 同等。
- [与工作台合并语义不一致] → 文档/注释标明分享为「赢者通吃」；若日后要统一再开变更。

## Migration Plan

纯前端行为；部署后刷新即可。无需数据迁移。回滚：恢复只读 `imgUrls`。

## Open Questions

- 无

## 相关长期文档

- `docs/01-产品需求/术语表.md`（缺陷分享）
- `docs/01-产品需求/产品定位与核心功能.md`（缺陷 / 自定义字段）
- 无新增 ADR
