## Context

团队设置已有缺陷属性卡片（`DefectSettingsCard` → `team-defect-fields`），缺陷页四视图由本地键 `defect.contentViewComponent` 硬编码全开；分享经 `/system/defect/{id}/shard` 且无团队开关；存活/驳回数字已可派生展示但无色阶。产品要求把「页面设置」挂进缺陷属性卡片，配置按团队生效。

## Goals / Non-Goals

**Goals:**

- 团队可配置视图开关、存活/驳回色阶（阈值可改）、是否允许分享
- 配置对团队下所有项目只读生效；关闭分享时拒新建与匿名打开（方案 B）
- 禁用视图时客户端强制降级；色阶只染数字

**Non-Goals:**

- 项目级覆盖团队页面设置
- 看板/Excel/甘特新增存活/驳回展示
- 改缺陷状态机或指标派生算法本身
- 独立第五张团队设置卡片

## Decisions

### D1. 持久化：`sys_team.defect_page_config` JSON 列

- **选择**：与 `defect_builtin_field_config` 同模式加 TEXT/JSON 列；Flyway 按 ADR-025 使用 `V1_2_1_3`（若归档时序号已占用则顺延同产品段）。
- **默认 JSON 示例**：

```json
{
  "views": { "DefectTable": true, "DefectExcel": true, "DefectGantt": true, "DefectKanban": true },
  "lifeTimeColors": {
    "greenBelowDays": 3,
    "orangeBelowDays": 7,
    "green": "#67C23A",
    "orange": "#E6A23C",
    "red": "#F56C6C"
  },
  "rejectCountColors": {
    "greenBelow": 3,
    "orangeBelow": 7,
    "green": "#67C23A",
    "orange": "#E6A23C",
    "red": "#F56C6C"
  },
  "allowShare": true
}
```

- **校验**：`greenBelow* < orangeBelow*`；天数/次数 ≥ 0；至少一个视图为 true（否则拒保存或强制表格开）。
- **备选**：独立表 → 否决（体量小，团队一行足够）。

### D2. API 与权限

- 写：`GET/PUT /system/team/{teamId}/defect-page-settings`，权限复用 `system:team:defect-field:list|edit`（同卡片管理员）。
- 读（项目侧）：`GET /system/project/{projectId}/defect-page-settings`（由 project→team 解析），`system:defect:list` 即可；便于缺陷页 bootstrap，不强迫打开团队设置。
- 服务端校验调用者可管该 `teamId`（写）或属该项目（读）。

### D3. 视图开关生效

- `defect/index.vue`：用团队配置过滤 radio 与 `DEFECT_CONTENT_VIEW_ALLOWED`；Cmd/Ctrl+O 只循环启用项。
- 缓存值不在启用列表：降级为启用列表中的第一项（优先 `DefectTable`），并写回本地键。

### D4. 色阶只染数字

- 表格 `table.vue` 统计值、详情 `DefectCustomFieldsDisplay` 的 `__lifeTime`/`__rejectCount` 数字包一层带 class 的 span。
- 分段：`value < greenBelow` → 绿；`value < orangeBelow` → 橙；否则红。存活用「天」比较（可用小时换算，&lt;3 天含不足 3×24h）。
- 颜色用配置色值；暗色主题仍用配置色（指标强调色，不强制 token 化三档）。

### D5. 分享关闭（方案 B）

- UI：`allowShare===false` 时不渲染 `ShareCard`。
- 后端：创建 shard 的 POST 返回 403；匿名 `GET .../shard/{id}` 在所属项目团队 `allowShare===false` 时返回 403，文案「分享已关闭」（不删历史 `sys_defect_shard` 行）。

### D6. 入口 UI

- `DefectSettingsCard` 增加「页面设置」链接 → 新路由 `team-defect-page-settings`（hidden + `activeMenu` 同团队设置）。
- 不新增枢纽第五卡片。

### D7. 不新增 ADR

团队 JSON 配置列已有先例；分享策略属产品行为，写入术语表/接口清单即可。

## Risks / Trade-offs

- [全关视图误操作] → 保存校验至少保留一个；空配置回退默认全开  
- [匿名分享 403 被当成 404] → 固定文案「分享已关闭」  
- [列表页多次拉配置] → 缺陷页进入时拉一次，Pinia/provide 或挂到现有 layout 缓存  
- [复用 defect-field 权限过宽/过窄] → 与卡片同管理员集合，避免新菜单种子复杂度；若需拆权限可后续增量  

## Migration Plan

1. Flyway 加列，存量行 `NULL` 或写入默认 JSON  
2. 部署前后端；旧前端忽略新字段，行为与今相同  
3. 回滚：忽略列即可；分享恢复为始终允许需回退后端校验  

## Open Questions

- 无（旧链 403 文案已定为「分享已关闭」）

## 相关长期文档

- `docs/01-产品需求/术语表.md`
- `docs/02-系统架构/数据库.md`（`sys_team`）
- `docs/04-UI规范/组件与交互约定.md`（缺陷视图切换）
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`（分享）
- `docs/07-技术决策/ADR-025-Flyway迁移版本与产品版本对齐.md`
- `docs/06-踩坑记录/缺陷属性团队配置后其他账号仍像默认.md`（teamId 一致性）
- 拟归档回写：术语表/数据库/接口清单；版本记录当月一行  
- 拟新增 ADR：无
