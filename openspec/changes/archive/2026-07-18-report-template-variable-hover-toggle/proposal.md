## Why

报告模版可视化编辑区里，内置 `$…{}` DSL（含 `${…}`、`$table`、`$card`、`$image`）目前以始终可见的 chip/块边框呈现，既不像生产 Markdown 渲染效果，也不便于在「看解析/预览内容」与「看原始 token」之间切换。编辑时需要默认与最终报告一致，又要在需要时快速核对变量身份。

## What Changes

- 将「变量」定义为内置 `${…}` 与 `$xxx{…}` 单元：`${}`、计划 `${api.plan…[n]}`、`$table`、`$card`、`$image`
- 上述单元在可视化浏览区统一为「默认无 chrome、悬停出编辑壳」
- 默认（未悬停）仅显示当前态内容（解析值或表/图/卡预览），无虚线边框、无两态按钮、无标题，对齐生产渲染
- 悬停时显示虚线边框、左上角两态切换按钮、标题行；正文在「内容」与「原始 token」之间切换
- 工具栏「全部展开 / 全部折起」覆盖上述全部单元；默认全展开（内容态）
- **不**把计划统计大表等 raw 保留块当作单一变量单元（表内 `${}` 仍各自处理）
- 不改变源码模式与最终报告序列化/渲染

## Capabilities

### New Capabilities

- `report-template-variable-preview`: 报告模版编辑器中 `${}` / `$xxx{}` 变量单元的默认生产对齐显示、悬停 chrome、内容/token 两态切换，以及工具栏全局展开/折起

### Modified Capabilities

- `report-template-milkdown-editor`: `${}` / `$table` / `$card` / `$image` 不再以始终可见的 chip/块边框作为默认展示；默认态须与生产文档一致，编辑 chrome 仅在悬停时出现

## Impact

- 前端：`ReportTemplateEditor` / Milkdown NodeViews（`cat2bug-views.js`）、`render-preview-*`、样式（`milkdown-crepe-theme.scss`）、工具栏（`ReportTemplateToolbar` / `toolbarMenuLoader`）
- 预览解析：复用 `report-preview-data` 与既有 table/image/case 预览
- 无后端 API / DSL 语法变更
- i18n：工具栏与悬停标题相关文案键
