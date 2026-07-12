## Why

Cat2Bug 前端基于 Vue 2.7 + Element UI + Vue CLI 4，已到达 Vue 2 生态终点（EOL 2023-12），构建慢、包体积大、无法使用 Vue 3 生态的新组件与工具。升级至 **Vite + Vue 3 + Pinia + Element Plus** 可显著提升开发体验（HMR、冷启动）与生产性能（Tree-shaking、更小 runtime），并为后续功能扩展打开生态空间。

迁移期间接受 **feature freeze**；**键盘/a11y 体验不可降级**，须以测试基线保障回归。

## What Changes

- **BREAKING**：前端框架从 Vue 2.7 升级至 Vue 3.x，应用入口从 `new Vue()` 改为 `createApp()`
- **BREAKING**：UI 库从 Element UI 2.x 替换为 Element Plus，全部 `el-*` 组件与样式体系迁移
- **BREAKING**：状态管理从 Vuex 3 迁移至 Pinia（6 个 store module）
- **BREAKING**：路由从 vue-router 3 升级至 vue-router 4
- **BREAKING**：i18n 从 vue-i18n 8 升级至 vue-i18n 9，Element locale 接入方式变更
- **BREAKING**：构建工具从 Vue CLI 4 / Webpack 替换为 Vite
- 332 个 `.vue` 组件逐批迁移（Options API 保留，legacy API 清理：`.sync`、`$set`、`slot-scope`、`beforeDestroy` 等）
- 40 个 keyboard mixins + 23 个 Element 耦合 plugins 重构为 **composables + Keyboard Adapter Layer**
- 第三方依赖替换：`mavon-editor`、`vue-excel-editor`、`vue-native-websocket`、`vue-meta`、`vue2-datepicker` 等
- 代码生成器（`tool/gen`）模板更新为 Vue 3 + Element Plus 输出
- 新增 Playwright 键盘/a11y E2E 回归套件（当前 `e2e/` 为空，迁移前在 Vue 2 基线上建立）
- 保留 `build:embedded` 产物输出至 Spring Boot `static/` 目录的部署路径

## Capabilities

### New Capabilities

- `frontend-vite-build`: Vite 开发与生产构建，含 dev proxy、/docs 静态服务、embedded 输出、gzip、环境变量兼容
- `frontend-vue3-shell`: Vue 3 应用壳层——createApp、vue-router 4、Pinia、vue-i18n 9、全局组件/方法、权限路由
- `frontend-element-plus`: Element Plus 集成——按需引入、主题变量、7 语言 locale、组件 API 差异适配
- `frontend-keyboard-a11y`: 键盘导航与 a11y 适配层——composables 从 mixins 抽出、Element Plus DOM/API adapter、现有 unit test 契约延续
- `frontend-e2e-keyboard`: Playwright 键盘/a11y E2E 回归——登录导航、缺陷列表/表单、DatePicker、CommandPalette、项目切换等最小集
- `frontend-lib-migration`: 长尾第三方库替换——WebSocket、mavon/markdown、excel editor、treeselect、datepicker、multipane 等

### Modified Capabilities

（无——`openspec/specs/` 尚无既有 capability spec，本次均为新建）

## Impact

- **代码范围**：`cat2bug-platform-ui/` 全目录（332 `.vue`、601+ src 文件），重点 `src/plugins/`、`src/mixins/`、`src/main.js`、`vue.config.js` → `vite.config.ts`
- **依赖**：`package.json` 大规模变更；移除 element-ui、vuex、vue-router@3、@vue/cli-*；新增 vite、element-plus、pinia、vue-router@4、vue-i18n@9 等
- **构建/部署**：`npm run dev`、`build:prod`、`build:embedded`、`build:cloud` 脚本与 CI 需同步更新；Spring Boot embedded SPA 与 Native 发布流程需回归验证
- **测试**：扩展 `tests/unit/` 键盘 utils 覆盖；新建 `e2e/` Playwright 套件；迁移期间以 e2e + unit 作为 phase 验收门禁
- **API/后端**：无后端 API 变更；WebSocket 连接方式变更仅影响前端封装
- **工期**：2 人全职约 14–18 周（含 feature freeze）；Phase 0 测试基线 → Phase 1 工具链/壳层 → Phase 2 适配层 → Phase 3 业务分批 → Phase 4 收尾
