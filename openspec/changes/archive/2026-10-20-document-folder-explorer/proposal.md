## Why

文档管理只有「当前文件夹 + 上级」导航，缺少资源管理器式左侧文件夹树，跨目录浏览与批量整理成本高；删除文件夹的文案承诺级联删除，后端实际只删选中 id，批删一开风险更大。现在把左侧树、多选批处理、分页本地记忆与级联删除一并补齐。

## What Changes

- 文档列表左侧增加文件夹树（类 Windows 资源管理器）：树顶全项目模糊搜、点节点右侧列出该夹内容（夹+文件）、节点旁图标「新建文件夹」、树可显隐且状态本地持久化；交互壳参考缺陷页交付物树（multipane），数据用文档 API，不复用交付物业务组件。
- 列表与树双向同步：点树 / 点列表文件夹行 / 「上级目录」行均更新 `docPid` 并互相同步高亮；「上级目录」行保留。
- 列表启用多选；「新建文件夹」前增加批量移动、批量删除；删前确认；文件夹删除 MUST 真正级联删除子孙（与现有 `doc.is-delete-dir` 文案一致）。
- 分页「每页条数」写入全局本地键，刷新时先读本地再发起一次 list（与缺陷 tab 恢复「程序化 hydrate 不旁路重复 list」同调）。
- 树搜索：关键字匹配**全项目**文件夹名（非仅已展开节点）。

不在本次范围：文档 Markdown 编辑页改版；移动对话框大改（批移复用现有移动对话框即可）；按项目隔离 pageSize。

## Capabilities

### New Capabilities

- `document-folder-explorer`: 文档页左侧文件夹树、全项目树搜、导航同步、多选与批移/批删、pageSize 全局本地记忆，以及文件夹级联删除。

### Modified Capabilities

- （无）现有 `document-online-create` 行为不变；工具条仅在「新建文件夹」前插入批处理按钮，不改变 split「新建文档 / 上传文档」语义。

## Impact

- 前端：`views/system/document/index.vue`、新建 `DocumentFolderTree`（或等价组件）、分页/显隐本地缓存；对照缺陷 `list/table.vue` multipane + `defect_tree_module_visible`。
- 后端：`SysDocumentService` / `SysDocumentMapper` 删除路径改为级联（或先收集子孙再删）；列表接口沿用 `GET /system/document/list`（`docType=0` 且不传 `docPid` 可拉全项目文件夹）。
- 长期文档：回写 `docs/04` 文档页交互约定、`docs/05` 删除语义；不推翻 ADR-007 / ADR-019。
