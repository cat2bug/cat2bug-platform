## Why

浅色主题同时存在 `#409eff` 与 `#1890ff` 两枚蓝，暗色主题把品牌色换成琥珀黄 `#FFC107`，页面里还有大量写死颜色。登录页已有的猫场景、Z 字气泡、可操控老鼠、像素验证码和语言图标是产品特色，必须保留。这次只把按钮、链接、菜单激活色收成同一枚蓝，不把界面换成通用后台模板。

## What Changes

- 浅色与暗色共用品牌蓝 `#1890ff`。暗色里链接和聚焦环用 `#69b1ff`，保证在 `#252529` 上可读。成功、警告、危险、收藏星、统计多色条等语义色不变。琥珀黄不再作为品牌色（品牌位），不等于清掉所有非蓝色。
- Element Plus 编译主色与 `--cat2bug-primary` 对齐为同一枚蓝。按钮悬停用主色的浅一档，浅色不再用金色悬停。
- 布局壳（侧栏、顶栏、页签）的表面色与激活色走同一套 token。控件高度保持 32px，圆角保持 4px。
- 登录页保持现有构图：猫场景（随浅色/暗色切换）、Z 字气泡、老鼠奔跑与方向键、像素验证码、语言图标、快捷键提示都保留。只把按钮、链接和焦点色换成同一枚品牌蓝。
- 缺陷列表作为验收页：表格、工具栏、弹窗只继承 token，不重写 `Cat2BugTable`。
- 不新增图标字体、插画包、Web 字体或第二套组件库。不改 Java 后端、权限、路由、接口和键盘行为。
- 帮助文档大图、`401.gif`、钉钉/微信说明截图的瘦身留到观感确认之后，不在本变更实施。

## Capabilities

### New Capabilities

- `admin-visual-system`：全站共用的品牌色、暗色可读强调色、布局壳激活态。登录页特色组件保持不动。

### Modified Capabilities

- `frontend-element-plus`：主题要求从「保留迁移时的视觉身份」改为「两套主题同一枚品牌蓝，暗色链接与聚焦环单独提亮」。

## Impact

- 前端样式：`cat2bug-platform-ui/src/assets/styles/theme-dark.scss`、`element-plus-variables.scss`，以及侧栏、顶栏、页签、登录页相关样式。
- 登录页：不删除 `CatIllustration`、`MouseRunner`、`MouseDpadControls`、像素验证码和语言切换。场景图与老鼠精灵继续打包。
- 验收页面：`views/system/defect/` 只检查是否继承 token，不改表格交互。
- 长期文档：更新 `docs/04-UI规范/主题与配色.md` 中的主色约定。不新增 ADR。不推翻 ADR-005（仍使用 Vue 3、Vite、Element Plus、Pinia）。
- 与进行中的 `replace-project-preset-icons`、`migrate-cloud-to-quarkus` 不共用同一批登录和主题文件；项目封面选择器不在本变更范围。
