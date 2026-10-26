## 1. 页签操作区 UI

- [x] 1.1 在 `defect/index.vue` 自定义页签 label 中用悬停操作区（编辑 + 删除）替换常显 `legacy-icon` close；「全部」「已删除」不加操作
- [x] 1.2 样式对齐工作台：默认隐藏，`.el-tabs__item:hover` 显示；图标用 `EditPen` / `CloseBold`；`mousedown`/`click` stop，避免误切页签
- [x] 1.3 补充 i18n（编辑页签 title / 对话框编辑标题与保存文案）

## 2. 编辑对话框与 API

- [x] 2.1 `DefectTabDialog` 支持 create/edit：`open(tab?)` 预填；编辑提交 `updateTabs`；emit `update`
- [x] 2.2 父组件处理 `update`：替换 `config.tabs` 对应项；若为当前激活页签则走现有单次 apply 路径刷新列表
- [x] 2.3 确认删除逻辑仍可用且与新操作区接线正确（含删当前 / 删光自定义后落到「全部」）

## 3. 验证

- [x] 3.1 手动：悬停才见操作；编辑改名与改筛选；取消不写库；删除确认；系统页签无操作；编辑激活页签只 list 一次（代码路径核对：`tabUpdateHandle` → `selectDefectTabHandle` 单次 apply；无浏览器实测）

## 4. 回写长期知识

- [x] 4.1 在 `docs/04-UI规范/组件与交互约定.md` 补「列表自定义页签」悬停编辑/删除约定；无新 ADR / 踩坑
