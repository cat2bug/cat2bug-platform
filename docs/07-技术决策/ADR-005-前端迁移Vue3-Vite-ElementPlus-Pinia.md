# ADR-005 前端迁移 Vue 3 + Vite + Element Plus + Pinia

记录前端从 Vue 2 / Vue CLI 整体迁移到 Vue 3 生态的决定。

- 日期：2026-07-12
- 状态：已采纳

## 背景

前端原为 RuoYi 模板：Vue 2.7.16 + Element UI 2.15.13 + Vuex 3 + vue-router 3 + Vue CLI 4，约 332 个 `.vue`、40 个键盘/a11y mixins、23 个自定义 plugins（大量耦合 Element UI 内部 API 与 DOM class）。动机是性能与新生态；迁移期间接受 feature freeze；键盘体验不可降级。依据：`openspec/changes/archive/2026-07-12-vue3-vite-pinia-migration/design.md`。

## 候选方案

### 方案 A：直迁 Vue 3 native API

优点：无 compat 运行时开销；一步到位。

缺点：需 feature freeze，一次性工作量大。

### 方案 B：`@vue/compat` 渐进迁移

优点：适合不能 freeze 的团队。

缺点：多一个阶段、收益有限，且 Element UI 本身不支持 Vue 3，compat 解决不了 UI 库问题。

### 方案 C：先 Vue 2.7 + Vite 中间态

缺点：多一次全量迁移；feature freeze 下没必要，未采纳。

### 状态管理：Vuex 4 vs Pinia

选 Pinia：仅 6 个 module、19 个 getter、约 100 处引用，迁移成本可控，是 Vue 3 生态首选。

## 最终决定

- Vite 直接替代 Vue CLI；Vue 3 + Pinia + Element Plus + vue-router 4 + vue-i18n 9。
- Element Plus 通过 `unplugin-vue-components` + `unplugin-auto-import` 按需引入。
- 键盘层做 adapter 三层分离，隔离 Element Plus 内部变更；迁移前先在 Vue 2 上建 Playwright 键盘 e2e 基线。
- 第三方库替换：`mavon-editor` → `md-editor-v3`、`vue-meta` → `@unhead/vue`、`vue-native-websocket` → 自封装 composable 等。
- 迁移期保留 Options API，不做全面 Composition API 重写。

当前 `cat2bug-platform-ui/package.json`：`vue ^3.5`、`vite ^6`、`element-plus ^2.9`、`pinia ^2.3`、`vue-router ^4.5`、`vue-i18n ^9.14`。

## 原因

性能（按需引入、冷启动/HMR）与生态；Element UI 不支持 Vue 3，必须整体换 UI 库。

## 副作用

- 迁移期 feature freeze。
- Webpack 特有写法要改：如 `require.context` → `import.meta.glob`（图标选择器曾因此空白）。
- vue-i18n 9 对 `$t('')` 空 key 抛 `Invalid argument`（曾在通知页出错）。
- 开发代理与 macOS IPv6 问题见 [[Vite代理用localhost解析到IPv6导致500]]。
- 注意：`CLAUDE.md` 中「前端 Vue 2.7 / Element UI / Vuex」的版本描述已过时（待确认是否由其它目录更新）。
