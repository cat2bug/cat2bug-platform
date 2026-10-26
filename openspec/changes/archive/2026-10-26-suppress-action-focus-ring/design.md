## Context

平台前端已有两套「受控」焦点视觉：

1. **表单聚焦环**：`cat2bug.scss` 中 `--cat2bug-field-focus-*` / `cat2bug-unified-focus-ring`（输入、选择、开关、上传等）。
2. **快捷键导航高亮**：业务 class（如 `.list-query-toolbar-nav-focused`、`.is-keyboard-focus`、`.defect-tab-nav-focused`），由键盘 composable / mixin 挂载。

同时存在第三套「野生」环：Element Plus 默认 `.el-button:focus-visible { outline: 2px solid ... }`，以及 `theme-dark.scss` 对部分按钮再次写出的同等 outline。鼠标点击后若焦点仍留在按钮上，且浏览器判定仍满足 `:focus-visible`（尤其刚用过 Tab 后），就会残留蓝色外框——用户感知为「偶尔、不可控」。

操作列 link/text 按钮已有局部灭环先例（`cat2bug-table-operate-link-button-reset`），可推广到全站动作控件，但须避开表单与导航 class。

## Goals / Non-Goals

**Goals:**

- 鼠标点击动作控件后，不出现、不残留 EP/主题层的蓝色 outline 焦点框。
- 表单聚焦环与快捷键导航 class 高亮行为保持不变。
- 用全局样式收敛，避免每个页面各自 `blur()` / 局部 hack。
- 把约定写回 `docs/04-UI规范/组件与交互约定.md`。

**Non-Goals:**

- 不改键盘适配层、composable、快捷键注册逻辑（`frontend-keyboard-a11y`）。
- 不引入 what-input / focus-visible polyfill 等新依赖。
- 不要求纯 Tab（未进入快捷键导航态）在按钮上仍显示原生 2px outline；若日后需要，可另开变更用受控 1px 环补齐。
- 不为每个按钮加点击后 `blur()`（样式灭环即可；个别组件已有 blur 可保留）。

## Decisions

### D1：用 CSS 灭动作控件的野生 outline，不用全局 JS blur

- **选择**：在全局样式中对动作控件的 `:focus` / `:focus-visible` 设 `outline: none`（必要时同步清掉仅用于焦点的外扩 `box-shadow`），覆盖 EP 与 `theme-dark` 中的 2px 规则。
- **相对**：点击后 `el.blur()`——散落、易漏、影响读屏焦点顺序。
- **理由**：问题是视觉环，不是焦点本身；样式一次收敛成本最低。

### D2：动作控件范围

- **纳入**：`el-button`（含 plain/text/link/circle，但操作列已有规则可并存）、`el-tabs__item`、顶栏 `right-menu-item` / 可聚焦图标包装、侧栏菜单项与折叠触发器上等「点一下执行动作」的控件。
- **排除**：`el-input` / `el-textarea` / `el-select` wrapper / 日期编辑器 / 组合选择器 / 上传焦点壳等走表单聚焦环的节点；带导航 class 的元素在导航态仍由既有规则画环。

### D3：暗色主题同步改，避免「亮色灭了暗色还在」

- **选择**：删除或改写 `theme-dark.scss` 里对按钮 `:focus-visible` 的 2px outline 强化，使其服从全局灭环；导航 class / 表单环的暗色 token 不动。
- **理由**：探索时确认暗色是残留蓝框的主要来源之一。

### D4：文档约定优于新 ADR

- **选择**：更新 `组件与交互约定.md`，不新增 ADR（无架构分叉，属 UI 交互细则）。
- **理由**：与现有「聚焦环 token 见主题与配色」交叉引用即可。

## Risks / Trade-offs

- [纯 Tab 浏览按钮时无原生焦点环] → 接受为本次代价；快捷键导航 class 仍覆盖主流程；若反馈强烈再补受控环。
- [选择器过宽误伤表单] → 选择器限定动作控件 class；回归时点输入框确认仍有聚焦环。
- [特异性打不过某页局部 `!important`] → 全局用同等或更高特异性，并对已知 `theme-dark` 规则做定点清理。
- [与操作列已有灭环重复] → 可并存；全局规则作为兜底，局部 reset 不必删除。

## Migration Plan

1. 合并全局样式与暗色清理后硬刷新验证：点工具栏按钮、页签、顶栏图标——无残留蓝框；点输入框——仍有聚焦环；进入列表快捷键导航——仍有 class 高亮。
2. 回滚：还原样式与文档提交即可，无数据迁移。

## Open Questions

- 无阻塞项。侧栏折叠钮、树侧栏 toggle 一并纳入动作控件范围（与 D2 一致）。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`（拟增补动作焦点约定）
- `docs/04-UI规范/主题与配色.md`（聚焦环 token `--cat2bug-field-focus-*`）
- 无新增 ADR
