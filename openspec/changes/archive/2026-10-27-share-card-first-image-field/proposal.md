## Why

缺陷分享卡（`DefaultDefectShareCard`）仍只读取系统字段 `imgUrls` 画图。项目已支持自定义图片字段，且系统「图片」可在缺陷属性管理中关闭或排到靠后；系统图 URL 404 时弹层只剩黑块，即使自定义字段已有可用图。需要按属性管理的统一排序，选「第一个有可用图的图片类型字段」来渲染。

## What Changes

- 分享图卡取图改为：按缺陷属性管理中系统+自定义共用的 `sortOrder`，筛选 `fieldType === 'image'` 且已启用的字段，依次解析 URL；过滤脏数据；对候选 URL 做加载探测，全部失败（含 404）则视为该字段无图并继续下一个；命中第一个有至少一张可加载图的字段后停止，仅渲染该字段下成功的图。
- 不再硬编码只读 `params.imgUrls`（系统 `imgUrls` 仍作为普通 image 字段参与排序竞争）。
- 取图过程可与现有分享弹层 loading 叠加，内容就绪后刷新弹层定位（沿用已有 `updatePopper`）。

## Capabilities

### New Capabilities

- `defect-share-card-image`: 缺陷分享图卡按属性排序选择第一个有可用图的 image 字段并渲染

### Modified Capabilities

- （无）本次为分享前端取图行为新增能力，不修改已有 openspec 主规格中的其它能力需求。

## Impact

- 前端：`ShareCard` / `DefaultDefectShareCard`；复用 `listEnabledFields`（或等价列布局 API）、`urlListFromCustomFieldValue`、`resolveCommaSeparatedUrls`；可参考工作台 `DefectsPendingRepair.rowImages` 的字段枚举，但语义为「赢者通吃」而非合并全部。
- 后端：无 API 变更（字段定义与缺陷行数据已有）。
- 文档：实现后按需回写 `docs/01` 分享相关一句事实；不新增 ADR。
