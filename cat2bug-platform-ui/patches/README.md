# npm 依赖补丁（patch-package）

本目录下的 `*.patch` 由 [patch-package](https://github.com/ds300/patch-package) 在 `npm install` 的 `postinstall` 阶段自动应用到 `node_modules/`。

## `vue-excel-editor+1.5.23.patch`

针对 `vue-excel-editor@1.5.23` 的 `src/VueExcelEditor.vue`：

- **`event.target` 为文本节点等非 Element 时**：原代码直接访问 `e.target.classList`，会抛出 `Cannot read properties of undefined (reading 'classList')`。补丁增加 `domEventTargetEl(e)`，在 `mouseDown`、`cellMouseMove`、`cellMouseOver`、`numcolMouseOver`、`colSepMouseDown`、`colSepMouseOver`、`colSepMouseOut`、`rowLabelClick`、`cellMouseOut` 等处统一归一化到最近 Element 再访问 DOM API。
- **`$refs.hScroll` / `$refs.vScrollButton` 可能不存在时**：`sbMouseDown` / `vsbMouseDown` 在访问 `classList` 前增加存在性判断，避免无底部横向条或纵向条场景下崩溃。

升级 `vue-excel-editor` 版本时：先改 `package.json` 中的精确版本并安装，再视上游是否已合并相同修复，决定删除本 patch 或重新执行 `npx patch-package vue-excel-editor` 生成新版本的补丁文件。
