## Why

报告详情目前是列表页上的单栏抽屉，只能通读正文，无法快速跳转到章节，也无法对正文做审阅批注。团队需要在「已生成报告」上围绕整行或选中文字讨论，并在有权限成员改正文后仍能保留评论线程。

## What Changes

- 将报告详情（`ViewReport`）改为左 / 中 / 右三栏：目录、正文、评论
- 左侧从正文标题生成目录，点击滚动到中间锚点，并支持当前节高亮
- 中间保留现有 markdown 渲染；支持整行批注与选中文字批注（含普通文字，不限表格）
- 右侧展示批注线程与扁平回复；正文变更后尽力重匹配锚点，失败则标记「原文已变更」并仍可阅读
- 批注删除权限：原文创建人、项目管理员、团队管理员；无通知 / @
- 范围仅限报告详情，不进入报告模板编辑器

## Capabilities

### New Capabilities
- `report-detail-layout`: 报告详情三栏壳（目录 / 正文 / 评论），含 TOC 与窄屏折叠
- `report-annotation`: 报告正文批注与扁平回复、锚点匹配 / orphaned 态、删除权限

### Modified Capabilities
- （无）现有 OpenSpec 能力不覆盖报告详情审阅交互；本次以新能力引入

## Impact

- 前端：`ViewReport.vue` 及报告详情周边组件；复用或扩展 `CommentInput` / `CommentView`
- 后端：扩展或旁路评论模型以支持 `moduleType=report`、父评论（扁平回复）、锚点字段；Flyway 迁移；删除鉴权对齐项目/团队管理员角色
- API：`/system/comment` 扩展或新增报告批注接口
- 导出 Word/PDF：仍只导出中间正文区域，不受侧栏影响
- 不改动报告模板编辑器（Milkdown）与模板相关 API
