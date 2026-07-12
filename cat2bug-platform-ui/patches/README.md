# npm 依赖补丁（patch-package）

当前无活跃 patch。`vue-excel-editor` 已内嵌至 `src/vendor/vue-excel-editor/`（含原 `vue-excel-editor+1.5.23.patch` 全部修改）。

若将来需对 npm 包打补丁：安装后执行 `npx patch-package <package>`，并在 `package.json` 保留 `postinstall: patch-package`。
