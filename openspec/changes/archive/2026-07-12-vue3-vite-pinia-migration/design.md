## Context

Cat2Bug 前端（`cat2bug-platform-ui/`）基于 RuoYi 模板，当前栈为 Vue 2.7.16 + Element UI 2.15.13 + Vuex 3 + vue-router 3 + Vue CLI 4。代码规模：332 个 `.vue` 文件，40 个 keyboard/a11y mixins，23 个自定义 plugins（大量耦合 Element UI 内部 API 与 DOM class）。

项目有显著的键盘导航差异化能力（缺陷管理快捷键、DatePicker 键盘、CommandPalette、表单字段提示等），现有 19 个 keyboard 相关 unit tests，但 Playwright e2e 目录为空。

部署模式包含：`dist/` 生产构建、`build:embedded` 输出至 Spring Boot static、Docker 与 Spring Native 发布。

**已确认决策（来自 explore 阶段）：**
- 动机：性能 + 新生态
- 迁移期间接受 feature freeze
- 状态管理选 Pinia（非 Vuex 4）
- 键盘/a11y 体验不可降级
- 构建工具选 Vite（不做 Vue 2.7 + Vite 长期双栈）

## Goals / Non-Goals

**Goals:**

- 将前端升级至 Vite + Vue 3 + Pinia + Element Plus + vue-router 4 + vue-i18n 9
- 开发体验：dev 冷启动 <10s，HMR <500ms
- 生产性能：Element Plus 按需引入，bundle 体积较当前 Element UI 全量引入有 measurable 下降
- 键盘/a11y 行为零回归（unit + e2e 双门禁）
- 保留全部构建模式（prod/cloud/embedded/analyze）与 7 语言 i18n
- 14–18 周内完成（2 人全职）

**Non-Goals:**

- 业务 UI  redesign 或视觉改版
- Options API → Composition API 全面重写（迁移期保留 Options API，仅 mixins → composables 为键盘层）
- 后端 API 变更
- 移动端 cat2bug-app 同步升级
- Vue 2.7 + Vite 长期并行维护

## Decisions

### D1: 直迁 Vue 3，跳过 @vue/compat 长期依赖

**选择：** 直接迁移至 Vue 3 native API，不使用 `@vue/compat` 作为长期方案。

**理由：** 已接受 feature freeze；compat 模式有运行时开销且 Element UI 本身不支持 Vue 3，compat 无法解决 UI 库问题。

**备选：** @vue/compat 渐进迁移 — 适合不能 freeze 的团队；对本项目增加一个 Phase 但收益有限。

### D2: Pinia 替代 Vuex 4

**选择：** Pinia，6 个 store 1:1 对应现有 Vuex modules。

**理由：** 仅 6 modules + 19 getters + ~100 处引用，迁移成本可控；Vue 3 生态首选；API 更简洁。

**映射：**

| Vuex module | Pinia store |
|---|---|
| app | useAppStore |
| user | useUserStore |
| permission | usePermissionStore |
| tagsView | useTagsViewStore |
| settings | useSettingsStore |
| dict | useDictStore |

**关键路径：** `user.SwitchCurrentProject` → `permission.GenerateRoutes` 链必须在 Pinia 中保持相同时序（含 `routesRefreshing` flag）。

### D3: Vite 直接替代 Vue CLI（不经过 Vue 2.7 + Vite 中间态）

**选择：** Phase 1 直接搭建 Vite + Vue 3 项目壳。

**理由：** feature freeze 下不需要在 Vue 2.7 上继续交付功能；减少一次全量迁移。

**Vite 关键映射：**

| Vue CLI / Webpack | Vite |
|---|---|
| `devServer.proxy` | `server.proxy` |
| `devServer.before /docs` | `configureServer` middleware |
| `CompressionPlugin` | `vite-plugin-compression` |
| `CopyWebpackPlugin` | `vite-plugin-static-copy` |
| `IgnorePlugin views/tool` | 条件 dynamic import 或 `build.rollupOptions` |
| `svg-sprite-loader` | `vite-plugin-svg-icons` |
| `transpileDependencies` | `optimizeDeps.include` |

### D4: Element Plus 按需引入

**选择：** `unplugin-vue-components` + `unplugin-auto-import` 自动按需注册 Element Plus 组件。

**理由：** 当前 `Vue.use(Element)` 全量引入是 bundle 体积主要来源之一；按需引入是性能目标的关键手段。

**主题：** `element-variables.scss` 迁移至 Element Plus CSS 变量或 SCSS 覆盖；保留 `cat2bug.scss` 自定义样式。

### D5: Keyboard Adapter Layer 架构

**选择：** 三层分离：

```
Views/Pages
    ↓ uses
Composables (useDatePickerKbd, useListQueryNav, useFormFieldHints, …)
    ↓ calls
Adapters (src/adapters/element-plus/date-picker.js, dropdown.js, table.js, …)
    ↓ queries
Element Plus DOM / component internals
```

**理由：** 40 mixins + 23 plugins 中 Element 耦合是最大回归风险源；adapter 层使 Element Plus 内部变更可隔离修复。

**迁移顺序：** 先抽 adapter 接口 → 从 mixins 复制逻辑到 composables（行为不变）→ 替换 Element UI 调用为 adapter 调用 → 在 Element Plus 上实现 adapter。

### D6: Phase 0 在 Vue 2 上建 E2E 基线

**选择：** 迁移任何框架代码之前，先在当前 Vue 2 代码上编写 Playwright 键盘 e2e。

**理由：** 没有 e2e 基线则「键盘不可降级」无法验收；Vue 2 基线测试在 Vue 3 迁移后仍应 pass。

### D7: 业务模块分批迁移顺序

**选择：** 低键盘复杂度 → 高键盘复杂度：

1. 系统管理 + 监控（user/role/dict/config/menu/dept/post/monitor）
2. 通知/IM 配置
3. 项目/团队/成员
4. 计划/用例/模块
5. 缺陷核心（index/table/form/drawer — 最高风险）
6. 缺陷 Excel 视图（独立 spike，可能最后）
7. AI/Cloud Case/报告/文档/统计
8. tool/gen 生成器模板

**理由：** Phase 1–2 完成壳层 + adapter 骨架后，用低复杂度模块验证 Element Plus 迁移模式，积累经验后再攻坚缺陷模块。

### D8: 第三方库替换策略

| 库 | 策略 |
|---|---|
| vue-native-websocket | 自封装 `useWebSocket` composable |
| vue-meta | @unhead/vue |
| mavon-editor | md-editor-v3 |
| vue-excel-editor | @vue3-excel/editor 或同类（需 spike） |
| v-hotkey | 评估移除（已有自研 shortcut 引擎） |
| vue-multipane | 先尝试现有 vendor patch 兼容 Vue 3，不行则换 splitpanes |

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| 键盘行为大规模回归 | Phase 0 e2e 基线 + adapter 层 + 19 个现有 unit test 不可降级 |
| Element Plus 内部 API 与 Element UI 不同 | 全部耦合点收敛到 adapter；禁止在 view 层直接访问组件实例内部字段 |
| excel.vue 2000+ 行 + vue-excel-editor 替换 | 独立 spike task；安排在所有其他模块之后 |
| embedded 构建破坏 Spring Boot 部署 | Phase 1 第一个 milestone 即验证 `build:embedded` |
| 14–18 周 freeze 期间业务压力 | 严格 phase gate；只有 e2e 全绿才进入下一 phase |
| 7 语言 i18n 回归 | 每种语言至少抽 1 个页面做手动 + 自动验证 |
| vue-multipane vendor patch 不兼容 Vue 3 | 预研 splitpanes 替代；layout 页面优先验证 |
| 代码生成器遗漏更新 | Phase 4 专门 task；生成代码做 snapshot 对比 |

## Migration Plan

```
Phase 0 (Week 1-2): 基线
  ├── Playwright 键盘 e2e 套件（Vue 2 上）
  ├── bundle analyze 基线报告
  └── 依赖 spike（excel editor, multipane, mavon 替代）

Phase 1 (Week 2-4): Vite + Vue 3 空壳
  ├── vite.config.ts（proxy/docs/embedded/alias）
  ├── createApp + router 4 + pinia + i18n 9
  ├── Element Plus 全局主题
  ├── layout + login + 权限路由
  └── ★ Milestone: 登录 → 侧栏 → 空页面；build:embedded 成功

Phase 2 (Week 4-6): 框架层 + Adapter 骨架
  ├── Pinia 6 stores 完成
  ├── Keyboard adapter 目录 + date-picker/dropdown 首个适配
  ├── useWebSocket composable
  ├── 全局组件/方法/globalProperties
  └── ★ Milestone: 系统管理任一 CRUD 页 + 键盘 e2e 部分通过

Phase 3a (Week 6-9): 低复杂度业务
  ├── 系统管理 + 监控 + 通知配置
  └── ★ Milestone: Phase 3a e2e 全绿

Phase 3b (Week 9-13): 核心业务
  ├── 项目/团队/成员/计划/用例
  ├── 缺陷 index/table/form/drawer + 全部 keyboard composables
  └── ★ Milestone: 缺陷全流程 e2e 全绿

Phase 3c (Week 13-15): 长尾
  ├── AI/Cloud/报告/文档/统计
  ├── mavon/markdown/excel editor 替换
  └── ★ Milestone: 全模块 e2e 全绿

Phase 4 (Week 15-16): 收尾
  ├── tool/gen 模板更新
  ├── legacy API 清理（.sync/$set/slot-scope）
  ├── 7 语言 i18n 回归
  ├── Spring Native / Docker embedded 回归
  └── 移除 Vue CLI / element-ui / vuex 依赖
```

**Rollback 策略：** 每个 Phase 在独立 git worktree 分支开发；Phase 1 完成前不影响 main 分支生产构建。若 Phase 3b 缺陷模块遇 blocker，可延迟 excel 视图，先交付 table/form 视图。

## Open Questions

1. **vue-excel-editor 替代方案：** 需在 Phase 0 spike 中对比 `@vue3-excel/editor` vs 自研轻量方案 vs 保留 iframe 方案
2. **v-hotkey 是否移除：** 已有自研 shortcut 引擎覆盖大部分场景；需 audit 剩余 v-hotkey 使用点
3. **vxe-table 实际使用范围：** package.json 有依赖但 src 中引用极少，可能可移除而非升级
4. **Pinia 是否引入 persistedstate plugin：** 当前 Vuex 无 persist plugin，暂不引入；dict cache 行为需验证
5. **TypeScript 是否本次引入：** 建议 Non-Goal（保持 JS），避免扩大 scope；Vite 配置可用 `.ts` 但 src 保持 `.vue` + `.js`
