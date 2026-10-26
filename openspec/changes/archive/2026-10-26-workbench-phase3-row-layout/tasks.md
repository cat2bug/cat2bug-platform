## 1. 配置模型与预设

- [x] 1.1 定义 v3 配置类型/规范化函数（`rows` / `cols` / `ratio` / `height` / `instanceId`），读入非 v3 时套当前角色预设
- [x] 1.2 改写 `presets.js`：tester / developer / manager / blank 输出 `version: 3` + `tabs.work.rows`；移除 `tabs.board` 默认
- [x] 1.3 更新 `alignLegacyConfig`：剥离看板与自由栅格路径；`<3` 或无 rows → 重置预设并清旧 `layout`/`widgets` 主路径

## 2. 去掉看板壳

- [x] 2.1 `index.vue` 移除看板 Tab、看板工具条与 `wb-proto is-board` 渲染分支
- [x] 2.2 `widget-registry`：原 `tabAffinity: 'board'` 改为延迟/隐藏（不进工作台目录），保留组件文件
- [x] 2.3 i18n 清理或保留「看板」文案键（壳层不再引用）

## 3. 行渲染查看态

- [x] 3.1 实现 `WorkbenchRows`（或等价）按 `rows` 渲染：比例分列、高度三态、小屏纵向堆叠
- [x] 3.2 按 `instanceId` 挂载注册表组件并传入 `props`；空槽在查看态隐藏或显示占位（产品选隐藏）
- [x] 3.3 删除或停用查看态对 `WorkbenchFreeGrid` / `wb-proto` 一期分区 / `packManagerWorkLayout` 的依赖

## 4. 行排版编辑器

- [x] 4.1 编辑态行工具条：增删行、列数（≤4）、比例（24 栅格下拉 + 拖拽整数吸附）、行高度模式（content / row / fixed+px）
- [x] 4.2 空槽：组件库拖入 + 槽内选择器指定类型；已填槽拖拽互换/移入空槽；移除变空槽
- [x] 4.3 列级高度覆盖；保存写回 `tabs.work.rows`（编辑器接受 `tabId`，默认 `work`）
- [x] 4.4 编辑态小屏可用性（可改结构；预览堆叠与查看态一致）

## 5. 文档钉选组件

- [x] 5.1 新增 `DocumentPin.vue` + 注册 `document-pin`；卡片展示标题/元数据（有则截断摘要）
- [x] 5.2 编辑态选文：项目文档列表仅 md/markdown；校验非 md 拒绝
- [x] 5.3 点击跳转 `HandleMarkdownDocument`；无文档/无权限空态；多实例不同 `docId`
- [x] 5.4 预设或组件库可选用（管理预设可选放一列空钉选或文档列表旁说明）

## 6. 验收与清理

- [x] 6.1 手测：四预设套用、行编辑保存刷新、拖+指定、高度三态、权限跳转预览/编辑
- [x] 6.2 移除死代码：自由栅格编辑主路径、看板专用逻辑、过时 preview 说明（或新增三期预览页）
- [x] 6.3 确认布局 API 仍按 JSON blob 读写；若有服务端 schema 校验则放宽至 v3

## 7. 回写长期知识

- [x] 7.1 更新 `docs/01-产品需求/` 中工作台相关描述（单表面 + 行排版 + 文档钉选；去掉看板叙事）
- [x] 7.2 `docs/08-版本记录` 当月一行提及本变更名
- [x] 7.3 确认无需新 ADR（design 已记「拟新增 ADR：无」）；有可复用踩坑则写入 `docs/06-踩坑记录/`，没有则勾掉并注明无
