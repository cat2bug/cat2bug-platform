## Why

项目工作台一期/二期用「组件列表 + 12 列自由栅格」定制布局，心智偏「贴磁贴」，难以自然表达「这一行几列、各占多宽、每列放什么」。管理场景还需要把指定 Markdown 文档钉在某一列，一点即进预览/编辑。功能尚未对外发布，适合一次性换成行槽位模型，并去掉已决策废弃的「看板」页签，为后续动态 Tab 复用同一套排版编辑器留接口形状。

## What Changes

- **BREAKING**：工作台配置升级为 `version: 3` 的 **行（rows）+ 列槽（cols）** 模型；废弃二期 `{ i, x, y, w, h }` 自由栅格与一期仅靠 `widgets[]` 顺序的查看态主路径（未发布，不做迁移）。
- **BREAKING**：去掉落地页「工作台 | 看板」双页签中的 **看板**；统计类组件保留在组件库，供日后动态 Tab 挂载，本阶段壳层只渲染工作台排版。
- 新增 **行排版编辑器**：可设每行列数与列宽比例；列内容支持 **从组件库拖入** 与 **槽内指定/下拉选择**；支持行/槽高度（文档高度 / 行高度 / 指定高度）。
- 新增组件 **文档钉选（document-pin）**：列内绑定一篇项目内 Markdown 文档；点击跳转现有 MD 文档页，按 `system:document:edit` 进入可编或只读预览。
- 角色预设（测试 / 开发 / 管理 / 空白）改为输出 `rows` 结构；去掉 `packManagerWorkLayout` 等对自由栅格的特判依赖。
- 配置持久化仍按用户×项目；API 形状可不变（JSON blob），仅客户端读写 v3 schema。
- 动态 Tab 本变更 **不做实现**，但 Tab 内布局字段与 `tabs.work.rows` **同构**，避免三期后再拆模型。

## Capabilities

### New Capabilities

- `workbench-row-layout`: 工作台 v3 行槽位配置、去掉看板壳、行排版编辑器（列数/比例/高度、拖拽与指定）、预设与查看态渲染。
- `workbench-document-pin`: 钉选指定 Markdown 文档的工作台组件、选文与跳转权限行为。

### Modified Capabilities

- （无）`openspec/specs/` 中尚未归档一期 `project-workbench` 主规格；本变更以新能力承接三期需求，不改现有主库 delta。

## Impact

- 前端：`cat2bug-platform-ui/src/views/system/dashboard/`（壳、`presets`、`widget-registry`、栅格/编辑器、`DocumentList` 旁新钉选组件）；i18n；可选预览页更新。
- 后端：工作台布局读写 API 一般无需改契约；若服务端有 schema 校验需放宽/改为 v3。
- 权限：沿用仪表盘查询与 `system:document:edit` / 文档模块权限。
- 文档：实现后回写 `docs/01-产品需求/` 工作台相关条目与 `docs/08-版本记录`；本变更不新增 ADR（无新运行时依赖；行模型决策记于 design）。
- 依赖：继续自研 CSS（flex/grid）行布局，不引入 `grid-layout-plus` 等新包。
