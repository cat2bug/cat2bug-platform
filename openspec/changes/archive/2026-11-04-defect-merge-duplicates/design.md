## Context

缺陷表格已有批量流转与串行删除（`defect-batch-operations`、`DefectBatchActionDialog`）。编辑抽屉为单栏 `EditDefectDialog` + `DefectFormOrderedFields`；删除为 `del_flag=2` 软删，不级联日志/计划关联。产品要求增加「合并重复缺陷」：右栏选定主缺陷整单写入左栏结果草稿，再按字段从源条覆盖，一次 API 更新主并软删从。

## Goals / Non-Goals

**Goals:**

- 表格批量 ≥2 条时提供「合并」；独立权限 `system:defect:merge`
- 双栏合并抽屉：左可编辑结果、右勾选列表；「主缺陷」整单覆盖（有内容则确认）；焦点字段联动 +「用此属性」单字段替换
- 单一后端接口事务内：校验 → 用提交体更新主缺陷 → 软删其余 → 写主缺陷 MERGE 日志；不可拆

**Non-Goals:**

- 深合并（迁移日志/评论、改写 `plan_item.defect_ids`、甘特 `plan_link`、分享 shard）
- 字段值拼接/并集（描述追加、处理人并集等）
- Excel / 看板 / 甘特入口（可后续复用抽屉）
- MCP 专用 merge 工具、Open API 对外文档大改（接口清单回写即可）
- 合并后禁止从回收站还原从缺陷（软删语义保持；产品不提供「拆分」流程即可）

## Decisions

### D1. 主缺陷 = 用户点的「主缺陷」按钮对应 id

- 打开抽屉时左栏为空草稿态（或仅占位提示），`masterDefectId` 未定；头部「合并」禁用。
- 点右侧「主缺陷」：若左栏已有任何用户/主缺陷写入内容，先确认「将覆盖当前左侧内容」；确认后将该源缺陷**全部可编辑表单字段**（与 `DefectFormOrderedFields` 一致，含 `customFields`、计划时间、图片/附件 URL 等）深拷进左栏，并设 `masterDefectId = 该 id`。
- 再次点另一条「主缺陷」同理整单覆盖并切换主 id。
- **备选否决**：默认勾选第一条为主——与「显式点主缺陷」冲突。

### D2. 「用此属性」= 单字段替换，非拼接

- 左栏字段获得焦点（或点击字段标签/控件）时，右栏每条卡片高亮**同名字段**并显示「用此属性」。
- 点击后仅用该源的该字段值覆盖左栏对应字段；其它左栏字段不变。
- 富文本/图片/附件/多选处理人：**整字段替换**，不做追加或并集。
- 状态字段：随「主缺陷」整单带入；「用此属性」若包含状态则同样可覆盖（与其它字段同一套）；不另做状态机校验（合并不是流转动作）。

### D3. API：`POST /system/defect/merge`

请求体示例：

```json
{
  "projectId": 1,
  "masterDefectId": 12,
  "sourceDefectIds": [12, 18, 21],
  "defect": { "...完整 SysDefect 可写字段，与 PUT /system/defect 网页提交同形..." }
}
```

- `sourceDefectIds` MUST 含 `masterDefectId`，且与打开时勾选集合一致（顺序无关）；长度 ≥2。
- 服务端校验：同 `projectId`、均 `del_flag=0`、调用者有 `system:defect:merge`、主 id 属于集合。
- 事务：`updateSysDefect`（完整对象，避免缺字段重置，见踩坑）→ 对其余 id 软删（复用现删除逻辑，可跳过逐条权限若 merge 已授权）→ 主缺陷插入日志 `DefectLogState.MERGE`（描述列出被合并的 `projectNum`）。
- 响应：更新后的主缺陷；失败整单回滚。
- **备选否决**：前端先 PUT 再逐条 DELETE——非原子，与「一个接口」不符。

### D4. 权限与菜单种子

- 新 F 权限：`system:defect:merge`，父菜单缺陷管理 `2019`，`menu_id` 取未占用号；Flyway 产品段下一空号（并行 `team-defect-page-settings` 若占 `V1_2_1_3` 则本变更用 `V1_2_1_4` 或顺延）。
- 默认仅赋给项目角色：`project.create-by`、`project.admin`、`project.tester`（`V1_2_1_4`/`V1_2_1_5` 写入；`V1_2_1_6` 撤回误授给团队创建人/团队管理员等）。
- 前端 `v-hasPermi` / `getBatchActionFlags` 增加 merge；**不**要求同时具备 edit+remove。

### D5. UI 结构

- 新组件如 `MergeDefectDrawer.vue`（宽抽屉 ≥80%），挂在 `defect/index.vue`；不硬改 `EditDefectDialog`。
- 左：`DefectFormOrderedFields` + 校验规则对齐编辑表单。
- 右：简单列表/卡片（编号、标题、状态徽标、「主缺陷」）；字段展示可用只读摘要，焦点字段高亮即可。
- 批量菜单项「合并」：选中 ≥2 且有 merge 权限且每条未删除 → 可用；**与状态混合无关**（不进入 `MIXED_STATE_BATCH_ACTIONS` 那套流转限制）。

### D6. 浅合并与不可拆

- 不迁移关联；从缺陷软删后日志仍挂旧 id（回收站可见）。
- 不提供「拆分合并」；还原从缺陷不自动回滚主缺陷字段（与普通还原一致）。文档/文案说明合并不可撤销字段层面的合并结果。

### D7. 不新增 ADR

权限点与软删均有先例；合并语义写入接口清单与术语表即可。

## Risks / Trade-offs

- [更新缺字段清空状态/自定义字段] → 接口要求 body 为完整缺陷对象；前端从主缺陷加载后再改；单测覆盖 customFields 保留  
- [计划项/甘特仍引用已删 id] → 浅合并接受；后续深合并另开变更  
- [用户误点主缺陷覆盖精细调整] → 二次确认  
- [未定主点合并] → 按钮禁用  
- [枚举末尾加 MERGE] → 确认 DB/前端日志文案映射；OpenAPI 枚举若暴露需同步  

## Migration Plan

1. Flyway 插入菜单与角色关联  
2. 部署后端（新接口）+ 前端；旧客户端无菜单项无影响  
3. 回滚：下线菜单与接口；已合并数据无法自动拆分（与软删一致）

## Open Questions

- 无（探索阶段已钉：主缺陷按钮、替换语义、独立权限、浅合并、≥2 不设上限）

## 相关长期文档

- `docs/05-接口文档/接口清单-缺陷与用例计划.md`（回写 merge 接口）
- `docs/01-产品需求/术语表.md`（可选补「缺陷合并」）
- `docs/06-踩坑记录/缺陷更新接口缺字段会重置状态并清空自定义字段.md`
- `docs/03-技术方案/计划项缺陷ID的CSV存储.md`（浅合并不改写的背景）
- `docs/04-UI规范/组件与交互约定.md`（抽屉/批量工具条约定）
- 无新增 ADR
