# 工作台 Close 图标与 ListDefectLog 重名导致编辑态崩溃

- 日期：2026-10-26
- 涉及：后台

## 现象

工作台顶栏「取消」按钮用了 `<Close />`（Element Plus Icons）后，一点「编辑工作台」页面就不进编辑态，或渲染异常。

## 根因

`unplugin-vue-components` 会按文件名自动注册 `ListDefectLog/close.vue` 为组件名 `Close`。模板里 `_resolveComponent("Close")` 拿到的是缺陷关闭对话框，不是图标；编辑态挂载取消按钮时把整页对话框当图标渲染，直接炸。

## 解决办法

不要在模板用裸名 `<Close />`。把图标放进 `data`（如 `CloseIcon: Close`），用 `el-button` 的 `:icon="CloseIcon"` 传组件引用。

## 以后怎么避免

- 全局自动导入与 `@element-plus/icons-vue` 易撞名（`Close`、`Edit`、`Delete` 等）时，优先 `:icon` / 别名，勿靠组件名解析。
- 改图标后用 Vite 编译结果确认 `_component_Close` 是否仍指向业务 `.vue`。
