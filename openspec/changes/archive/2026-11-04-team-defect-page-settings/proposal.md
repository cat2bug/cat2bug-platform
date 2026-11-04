## Why

缺陷页的四视图、存活/驳回数字样式、外部分享目前是全局默认行为，团队无法按协作习惯收敛（例如关掉看板、强调超龄缺陷、禁止外链分享）。配置应落在团队设置的缺陷能力下，与缺陷属性同级管理。

## What Changes

- 在团队「缺陷属性」设置卡片内增加「页面设置」入口（不新增第五卡片）
- 团队可配置：缺陷显示模式（表格 / Excel / 甘特 / 看板，默认全开）、存活时间色阶阈值与颜色（默认可改：&lt;3 天绿、&lt;7 天橙、≥7 天红）、驳回次数色阶阈值与颜色（默认 &lt;3 绿、&lt;7 橙、≥7 红）、是否允许分享缺陷
- 色阶只作用于「时间数字 / 驳回数字」，标签文案不上色；至少覆盖表格统计行与详情固定统计行
- 关闭分享时：**BREAKING** 对匿名已有分享链接：拒绝匿名 GET（与拒新建、UI 隐藏入口一并生效）
- 本机缓存了已被团队禁用的视图时，强制降级到仍启用的第一个视图并纠正缓存
- 配置按 `teamId` 持久化；团队下项目只读生效（含仅项目挂职成员）

## Capabilities

### New Capabilities

- `team-defect-page-settings`：团队缺陷页面设置（视图开关、指标色阶、分享开关）的读写、生效与校验

### Modified Capabilities

- `team-case-defect-settings`：缺陷设置卡片需提供页面设置入口（仍仅团队 create-by/admin 可管）

## Impact

- 前端：`DefectSettingsCard`、新设置子页、`defect/index.vue` 视图白名单、`table.vue` / 详情统计数字样式、`ShareCard`/`DefectTools`
- 后端：`sys_team` 配置列（或等价 JSON）、团队读写 API、`DefectResource` 分享创建与匿名 GET 校验
- 迁移：Flyway 加列 + 默认 JSON；菜单/权限可复用或扩展 `system:team:defect-field:*`
- 依赖：存活/驳回数字依赖已落地的周期派生字段（`fix-defect-life-reject-metrics`）
- 不推翻现有 ADR
