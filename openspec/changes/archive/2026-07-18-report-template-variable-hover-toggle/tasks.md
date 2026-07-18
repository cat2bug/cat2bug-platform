## 1. Shared expand state

- [x] 1.1 新增编辑器级 `variablesExpanded` 会话态模块（默认 `true` = 内容态），支持 subscribe / setAll / 可选 per-node override 与 resetAll
- [x] 1.2 补充 path → 悬停标题文案映射（覆盖工具栏常见变量；未命中退回 path 末段）

## 2. `${}` Variable NodeView

- [x] 2.1 重构 `createLiveChipView`（或拆出变量专用 view）：未悬停仅渲染当前态纯文本；悬停挂虚线框 + 左上角两态按钮 + 标题行
- [x] 2.2 内容态继续走 `resolvePreviewVariable` / `resolvePreviewPlanVariable`；变量态正文固定为 `attrs.raw`
- [x] 2.3 两态按钮切换本地模式；订阅全局 `variablesExpanded`，工具栏 reset 时覆盖本地
- [x] 2.4 更新 `milkdown-crepe-theme.scss`：去掉默认 chip 边框样式；增加悬停 chrome / 两态按钮样式，保持行内文档流

## 3. Toolbar

- [x] 3.1 在报告模版工具栏菜单增加 `type: 'check'`「全部展开/全部折起」项，并补 i18n
- [x] 3.2 在 `ReportTemplateToolbar` 接通 check 回调，切换全局 `variablesExpanded` 并同步 active 图标

## 4. `$table` / `$card` / `$image` units

- [x] 4.1 用与 `${}` 相同的悬停/两态工厂覆盖 `$table`、`$card`、`$image` NodeView（默认无块边框 chrome）
- [x] 4.2 `$card` 内容态提供基于预览数据的用例列表预览；变量态显示原始 `$card{…}` token
- [x] 4.3 计划统计大表按 Markdown 表格预览（表内变量随展开/折起）；不作为单一变量单元；其它 raw 保留块保持只读 chrome

## 5. Verification

- [x] 5.1 单元测试：全局态切换、单节点 override、标题 fallback、raw token 显示
- [x] 5.2 验收：`${}` / `$table` / `$card` / `$image` 默认像生产；悬停出 chrome；两态切换；工具栏全开/全折；raw 保留块不受影响；保存内容不变
