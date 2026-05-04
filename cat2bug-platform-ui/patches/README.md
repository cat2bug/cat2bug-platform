# npm 依赖补丁（patch-package）

本目录下的 `*.patch` 由 [patch-package](https://github.com/ds300/patch-package) 在 `npm install` 的 `postinstall` 阶段自动应用到 `node_modules/`。

## `vue-excel-editor+1.5.23.patch`

针对 `vue-excel-editor@1.5.23` 的 `src/VueExcelEditor.vue` 与 `src/VueExcelColumn.vue`：

- **`event.target` 为文本节点等非 Element 时**：原代码直接访问 `e.target.classList`，会抛出 `Cannot read properties of undefined (reading 'classList')`。补丁增加 `domEventTargetEl(e)`，在 `mouseDown`、`cellMouseMove`、`cellMouseOver`、`numcolMouseOver`、`colSepMouseDown`、`colSepMouseOver`、`colSepMouseOut`、`rowLabelClick`、`cellMouseOut` 等处统一归一化到最近 Element 再访问 DOM API。
- **`$refs.hScroll` / `$refs.vScrollButton` 可能不存在时**：`sbMouseDown` / `vsbMouseDown` 在访问 `classList` 前增加存在性判断，避免无底部横向条或纵向条场景下崩溃。
- **可选 `cellHtml` 列渲染**：`VueExcelColumn` 增加 `cellHtml` 函数 prop 并注册到 `fields`；`VueExcelEditor` 数据格在存在 `item.cellHtml` 时用 `v-html` 渲染返回值（宿主负责属性转义），并为 `td` 增加 `cell-html` class，供缺陷 Excel 图片列平铺缩略图等场景使用。
- **`domEventTargetTd(e)`**：在 `mouseDown`、`cellMouseMove`、`cellMouseOver`、`numcolMouseOver`、`cellMouseOut` 中从事件目标向上解析到 `TD`，避免 `cellHtml` 子节点（如 `img`）导致 `colPos` 为 -1、`fields[colPos]` 为 `undefined` 报错；`mouseDown` 中右侧热区判断改为相对 TD 的 `clientX`。
- **下拉/日期三角**：`mouseDown` 在 `cellRangeSelectArm` 之后若从右侧三角打开 `calAutocompleteList` / `showDatePickerDiv`，同一次点击的 `document` 捕获阶段 `mouseup` 会进入 `cellRangeSelectUp` → `moveInputSquare`，从而清空 `autocompleteInputs`、关掉日期面板（表现为一闪即关）。与 `listByClick` 分支一致，在打开面板前先调用 `cellRangeSelectCleanupListeners()`。
- **滚动性能**：`tableScroll` 用 `requestAnimationFrame` 合并为每帧最多一次；无矩形选区/拖选/填充柄拖拽时不调用 `updateCellRangeSelectionOverlay`（避免滚动中大量 `getBoundingClientRect`）；`calVScroll` 用 `$set` 更新 `vScroller` 各字段，去掉滚动路径上的 `$forceUpdate()`；`beforeDestroy` 取消未执行的 scroll rAF。
- **行选与格选互斥**：左侧行号列 `rowLabelMouseDown` / `rowLabelClick` 前调用 `exclusiveClearSheetFocusForRowSelect()`，清除单元格焦点、矩形区域选与填充柄态；在数据格 `mouseDown`、`cellDoubleClick`、填充柄 `fillHandleMouseDown` 时若存在多行选中则 `clearAllSelected()`；`clearAllSelected()` 同步去掉当前页 `tr.select` 样式。
- **行多选视觉与矩形选区一致**：`updateCellRangeSelectionOverlay` 在仅有行选中时，用每行 `tr.getBoundingClientRect()` 算外框（避免首列 `td` 与多行内容格合并 bbox 时底边缺角）；不连续行（Ctrl）拆成多块 `rowSelectOverlaySegments`；`.cell-range-overlay` 的 `z-index` 高于填充预览层，避免描边被挡；`rowLabelClickOnRow` 在 `clearAllSelected` 前用 `wasSelected` 再决定是否 `selectRecord`，避免点行号单选不上。
- **宿主页**：勿再给 `tr.select td` 铺整行淡蓝底，以免与叠层混色。

升级 `vue-excel-editor` 版本时：先改 `package.json` 中的精确版本并安装，再视上游是否已合并相同修复，决定删除本 patch 或重新执行 `npx patch-package vue-excel-editor` 生成新版本的补丁文件。
