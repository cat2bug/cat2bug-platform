## 1. 后端：add 返回 docId

- [x] 1.1 确认 `insertSysDocument` / Mapper 写入后实体带上生成的 `docId`（必要时补 `useGeneratedKeys`）
- [x] 1.2 `DocumentResource.add` 成功时返回含 `docId` 的 `AjaxResult`（不再仅 `toAjax(rows)`）
- [x] 1.3 手工或单测验证：空 MD 元数据可插入，响应可读到 id

## 2. 文档列表：split 新建 / 上传

- [x] 2.1 i18n：保留「新建文档」语义给在线新建；上传改为「上传文档」（中/英等现有 locale）
- [x] 2.2 `document/index.vue` 工具条改为缺陷同款 `el-dropdown` split：主按钮 +「新建文档」→ 在线新建；「上传文档」→ 现有上传对话框
- [x] 2.3 实现默认名「新建文档」/「新建文档n」与空内容 `addDocument`；成功用返回 `docId` 跳转 `HandleMarkdownDocument`；失败 toast 不跳转
- [x] 2.4 核对键盘 hint / `v-hasPermi` 仍覆盖整组；上传路径行为与改前一致

## 3. 评审纪要模板壳

- [x] 3.1 `case-settings/index.vue`：`Cat2BugEditor` 启用 `paper` + `flat` + `show-save`，`@save` 接现有保存；去掉底部独立保存行
- [x] 3.2 对照 `HandleMarkdownDocument` 视觉：无内层多余边框、无额外上下间隔条、背景与外层一致；只读时无保存

## 4. 验收与长期知识

- [x] 4.1 验收：列表 split 两项、默认名递增、进编辑器可保存；模板页工具条保存且壳对齐文档 MD 页
- [x] 4.2 回写 `docs/05` 文档 add 响应约定、`docs/04` Cat2BugEditor paper/flat/工具条保存约定；无新 ADR / 无踩坑则注明无
