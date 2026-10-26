## 1. 全局灭野生焦点环

- [x] 1.1 在 `cat2bug.scss`（或等价全局入口）为动作控件补充 `:focus` / `:focus-visible` 灭 outline 规则，覆盖 EP `.el-button:focus-visible` 的 2px 蓝框；勿伤及表单聚焦环选择器
- [x] 1.2 清理 `theme-dark.scss` 中对按钮等动作控件再次强化的 `:focus-visible` outline，使暗色与浅色一致服从全局灭环
- [x] 1.3 核对 tabs / navbar / 侧栏 toggle 等是否仍有局部野生 outline；有则纳入同一套规则或定点覆盖

## 2. 回归受控焦点

- [x] 2.1 手动验证：点击工具栏按钮、页签、顶栏图标、侧栏项——无残留蓝框
- [x] 2.2 手动验证：聚焦输入框 / 选择器——`--cat2bug-field-focus-*` 环仍在
- [x] 2.3 手动验证：列表快捷键导航（如 `list-query-toolbar-nav-focused` / 缺陷 tab 键盘态）——class 高亮仍在

## 3. 回写长期知识

- [x] 3.1 更新 `docs/04-UI规范/组件与交互约定.md`：动作点击无残留野生焦点框；表单环与键盘导航 class 高亮保留；可交叉引用 `主题与配色.md` 聚焦环 token
- [x] 3.2 确认无需新增 ADR；若有可复用踩坑再写入 `docs/06-踩坑记录/`，没有则跳过
