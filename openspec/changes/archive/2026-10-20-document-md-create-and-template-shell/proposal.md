## Why

文档列表的「新建文档」实际是上传文件，缺少「在线新建 Markdown 并进入编辑」；评审纪要模板页虽已用 `Cat2BugEditor`，纸面边框/间距与保存按钮位置仍与文档 Markdown 页不一致。现在把两条线对齐：创建路径拆清，编辑壳统一。

## What Changes

- 文档列表「新建文档」改为 split 下拉：主按钮与菜单第一项「新建文档」→ 当前文件夹下创建空 `.md`（默认名规则见 design）并打开 `/document/md/:docId`；菜单第二项「上传文档」→ 现有上传对话框。
- `POST /system/document`（`addDocument`）成功响应 **返回 `docId`**（或含 id 的数据），以便创建后跳转编辑器。
- 评审纪要模板页（`case-settings`）：编辑器 `paper` + `flat`、无内层边框与多余上下间隔；保存移入工具条（与 `HandleMarkdownDocument` 同模式），去掉编辑器下方独立保存行；继续共用 `Cat2BugEditor`。
- i18n：上传相关文案改为「上传文档」；「新建文档」专指在线新建。

不在本次范围：全局列表密度、操作列颜色扫尾；不新增独立 `upload` 权限串。

## Capabilities

### New Capabilities

- `document-online-create`: 文档列表 split 新建 Markdown（默认名、空内容、返回 docId、跳转编辑器）与上传分流。

### Modified Capabilities

- `case-review`: 项目评审纪要模板编辑页的视觉壳与保存入口与文档 Markdown 编辑页对齐（仍用 `Cat2BugEditor`，行为不变，交互壳变更）。

## Impact

- 前端：`views/system/document/index.vue`、`HandleMarkdownDocument.vue`（对照）、`views/system/project/case-settings/index.vue`、`Cat2BugEditor` 用法；缺陷/用例 split 模式与键盘 hint。
- 后端：`DocumentResource` / `SysDocumentService` 新增返回 `docId`；不改权限标识（仍 `system:document:add` / `edit`）。
- 长期文档：回写 `docs/05` 文档接口说明、`docs/04` 组件约定中 Cat2BugEditor 接入说明；不推翻 ADR-019。
