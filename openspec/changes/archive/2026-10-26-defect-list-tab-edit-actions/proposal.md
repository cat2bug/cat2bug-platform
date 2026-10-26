## Why

缺陷列表自定义页签的关闭按钮常显，且无法在页签上直接编辑筛选配置；工作台页签已统一为「悬停才显示编辑 / 删除」。两边交互不一致，缺陷侧也缺少改名与改筛选的入口（后端 `PUT /system/tabs` 已存在，前端未接上）。

## What Changes

- 自定义缺陷页签：悬停才显示操作区（编辑 + 删除）；默认隐藏关闭叉。
- 「全部」「已删除」系统页签：仍无编辑 / 删除。
- 新增编辑：打开与创建共用的页签对话框，预填名称与筛选配置；保存走已有 `updateTabs`，成功后更新本地 `config.tabs` 并重新应用当前页签查询（若正在编辑的是当前激活页签）。
- 删除行为保持确认后 `delTabs`；系统页签始终兜底，不引入「仅剩 1 个禁删」规则。
- 操作图标对齐工作台：`EditPen` / `CloseBold`（替换常显 `legacy-icon` close）。

## Capabilities

### New Capabilities

- `defect-list-tabs`：缺陷列表页签条交互（自定义页签悬停操作、编辑对话框、删除；系统页签无操作）

### Modified Capabilities

- （无）`defect-tab-list-fetch` 仅约束缓存与 list 次数，本变更不改其需求。

## Impact

- 前端：`views/system/defect/index.vue`（页签 label / 样式）、`DefectTabDialog.vue`（create/edit）、相关 i18n
- API：复用已有 `GET/POST/PUT/DELETE /system/tabs`，预计无后端契约变更
- 文档：归档前视情况回写 `docs/04-UI规范/组件与交互约定.md`（页签悬停操作约定）
