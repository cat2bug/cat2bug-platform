## 1. 注册表与分组元数据

- [x] 1.1 在 `widget-registry.js` 为每个目录可见 widget 增加 `group`，并定义 `CATALOG_GROUP_ORDER` / 分组聚合辅助函数
- [x] 1.2 约定预览资源解析（`preview` 字段或按 id 映射到 `assets/workbench/previews/{id}.*`），缺图可返回空供占位
- [x] 1.3 补充分组与「已添加」相关 i18n（zh-CN / en-US）

## 2. 预览真图资源

- [x] 2.1 建立 `cat2bug-platform-ui/src/assets/workbench/previews/`，统一 16:10 出图规范
- [x] 2.2 为当前 `listCatalogWidgets()` 全部可见类型提交真图文件（验收禁止仅占位）

## 3. 共享目录组件

- [x] 3.1 新增 `WorkbenchWidgetCatalog.vue`：按 group 折叠列表、预览框固定 16:10、`object-fit: contain`、缺图同比例占位
- [x] 3.2 根据传入的 used types（或 rows）显示「已添加」标记；保持 `catalog:<type>` 拖拽契约
- [x] 3.3 标记仅为提示：已添加项仍可拖；不在目录层做整页唯一拦截

## 4. 接入行编辑器

- [x] 4.1 `WorkbenchRowEditor` 左侧 aside 改用共享目录组件，从当前 `localRows` 计算 used types
- [x] 4.2 回归：拖放到空列/堆叠列、同列重复警告、列内下拉过滤行为与堆叠语义不变
- [x] 4.3 自定义 Tab 编辑路径目视确认同一目录 UI

## 5. 回写长期知识

- [x] 5.1 更新 `docs/01-产品需求/产品定位与核心功能.md` 工作台一行：组件库按业务域分组、真图保比例预览、本页已用提示
- [x] 5.2 在 `docs/08-版本记录/2026-10.md` 记本变更一行
- [x] 5.3 ADR：无新增；踩坑：无（实现顺利，无可复用坑）
