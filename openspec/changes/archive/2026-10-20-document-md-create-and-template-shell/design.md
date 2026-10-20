## Context

文档列表工具条「新建文档」（`doc.create-file`）当前打开上传对话框；在线 Markdown 编辑已存在（`HandleMarkdownDocument` + `Cat2BugEditor`，`paper`/`flat`/`show-save`）。`POST /system/document` 的 `add` 用 `toAjax(rows)`，成功不返回 `docId`，无法创建后直接进编辑页。

评审纪要模板页（`case-settings/index.vue`）已接 `Cat2BugEditor`，但未开 `paper`/`flat`/`show-save`，保存按钮在编辑器下方独立一行，视觉与文档 MD 页不一致。

约束：权限仍用 `system:document:add` / `system:document:edit` 与 `system:project:case-type:edit`；不推翻 ADR-019（Tiptap 连续 A4）；新建名规则已锁定为「新建文档」/「新建文档n」。

## Goals / Non-Goals

**Goals:**

- 文档工具条 split：主按钮 + 菜单第一项 = 在线新建空 `.md` 并进编辑器；第二项 = 上传（现有对话框，文案改为「上传文档」）。
- `addDocument` 成功响应携带 `docId`，前端据此 `push` 到 `HandleMarkdownDocument`。
- 默认文件名：当前文件夹下列名冲突时用「新建文档」，否则「新建文档2」「新建文档3」…（扩展名 `.md`；展示名不含扩展名时与现有文档名列一致）。
- 模板页与文档 MD 页同一套 `Cat2BugEditor` 壳：`paper` + `flat` + 工具条保存；去掉模板页下方保存行。

**Non-Goals:**

- 全局列表密度 / 操作列颜色统一扫尾。
- 新建独立 `upload` 权限码；不改文档存储后端模型（ADR-007）。
- 把模板页路由并入 `HandleMarkdownDocument`（数据源不同：文档 vs 项目评审模版 API）。
- 抽取第二层「编辑 MD 页面壳」组件（除非实现时发现 props/样式重复到必须抽；默认两页直接复用 `Cat2BugEditor`）。

## Decisions

1. **Split 交互对齐缺陷列表**  
   使用 `el-dropdown` + `split-button`（参考 `defect/index.vue` 新建下拉）。主按钮点击 = 在线新建；caret 展开两项。权限 `v-hasPermi` 仍挂在整组，两项都需 `system:document:add`。

2. **在线新建走现有 add API，不新增 endpoint**  
   前端构造 `docType=文件`、`fileExtension=md`（或与现网 MD 文件字段一致）、空内容/空附件 URL（按现有 MD 创建字段约定）、`projectId` + 当前文件夹父 id。后端 `insertSysDocument` 后返回 `AjaxResult.success(docId)`（或 `data` 含 id）。**备选**：新增 `POST .../markdown` —— 拒绝，避免双路径。

3. **默认名在前端算，服务端仍做唯一性校验**  
   前端根据当前列表（或已有同级名）生成「新建文档」/「新建文档n」；若并发冲突，toast 失败并不进编辑器。不在服务端硬编码该文案（i18n 在前端）。

4. **模板页只改壳，不改保存 API**  
   `getCaseReviewTemplate` / `saveCaseReviewTemplate` 不变；`:paper="true" :flat="true" :show-save="canEdit"`，`@save` 调现有 `saveTemplate`；删除 `.case-settings-tools` 底部按钮。

5. **「同一封装」= `Cat2BugEditor`**  
   不把模板编辑塞进文档路由；两页共享组件与 props 约定即可。若后续第三处再接 MD 编辑，再考虑抽 thin wrapper。

## Risks / Trade-offs

- [add 响应形态变更] → 仅成功时多返回 id；`toAjax` 调用方若只看 code 不受影响；前端新建路径必须读 `docId`/`data`。
- [同级重名竞态] → toast + 不跳转；用户可改名重试。
- [空 MD 无 fileUrl] → 与现有 MD 编辑保存路径对齐（先建元数据再写内容）；实现时对照 `HandleMarkdownDocument` 首次保存逻辑。
- [split 键盘 hint] → 对齐缺陷列表 caret / 主按钮 hint 类名，避免破坏现有 list-query 导航。

## Migration Plan

- 纯前向兼容：旧客户端忽略多余 data；新前端依赖 id。
- 回滚：恢复单按钮上传 + 模板页底部保存即可；DB 无迁移。

## Open Questions

无（命名、split 两项、返回 docId、模板壳对齐已锁定）。

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/04-UI规范/`（组件与列表工具条约定；实现后回写 Cat2BugEditor paper/flat/工具条保存约定）
- `docs/05-接口文档/`（文档 add 响应含 docId；实现后回写）
- `docs/07-技术决策/ADR-019-可视化Markdown统一用Tiptap连续A4纸面.md`（遵守，不新增 ADR）
- `docs/07-技术决策/ADR-007-文档正文存每项目隐藏Git仓库.md`（存储不变，仅引用）

拟新增 ADR：无
