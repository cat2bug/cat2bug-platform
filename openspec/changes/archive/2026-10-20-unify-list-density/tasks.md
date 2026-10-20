## 1. Token 与全局表格轨

- [x] 1.1 在 `cat2bug.scss`（`:root` / 必要时暗色）新增 `--cat2bug-table-row-min-height`（约 40px），并实现业务表 `th`/`td` cell 上下 padding，使单行达到表格轨最小行高；保持 `--cat2bug-table-row-max-height` 100px
- [x] 1.2 操作列 link/text 在单元格内垂直居中，与表格轨行高协调（含 `small-padding` / `cat2bug-operate-column`）
- [x] 1.3 暗色下抽查表格分割线与字色仍可读，无额外实心钮回归

## 2. 工具栏视觉同轨

- [x] 2.1 约定并改列表工具栏主新建/添加为 `type="primary" plain`（至少覆盖交付物、OpenAI 账号及同模式 `project-option-list-tools` / `module-tools` 触及页）
- [x] 2.2 去掉上述列表工具栏上无意义的 `size="small"`（高度已由 32px 强制）

## 3. Cat2BugTable 能力（若缺）

- [x] 3.1 确认 `Cat2BugTable` 能否透传 `row-key`、`tree-props`、`default-expand-all`、`max-height`；缺则补透传或文档化 fallback
- [x] 3.2 树表场景下 expand/折叠与表头手柄不冲突的冒烟说明写入实现注释或 tasks 备注

## 4. 首批页面迁移

- [x] 4.1 OpenAI 账号列表改为 `.list-page-toolbar`（或等价）+ `Cat2BugTable`，保留筛选、新建、测试/修改/删除与分页
- [x] 4.2 交付物列表改为标准工具条；表迁 `Cat2BugTable`（树透传）或按 design fallback 仅工具条+全局行高，保留展开/折叠与名称筛选
- [x] 4.3 浅色/暗色对照用例或缺陷列表：工具栏同高同重量、单行更松、操作列居中

## 5. 回写长期知识

- [x] 5.1 更新 `docs/04-UI规范/组件与交互约定.md`：表格轨最小行高、工具栏 plain primary、业务列表迁 Cat2BugTable + list-page-toolbar；注明 `small-padding` 非 EP size
- [x] 5.2 `docs/08-版本记录/2026-10.md` 增加本变更一行；拟新增 ADR：无
- [x] 5.3 可复用踩坑：无则勾掉注明；若实现中发现全局 padding 误伤特殊表再补踩坑

## 6. 验收修订（design 修订后）

- [x] 6.1 将 `--cat2bug-table-cell-padding-y` 调为 16px、`--cat2bug-table-row-min-height` 调为 56px；操作列内层 min-height 随公式更新
- [x] 6.2 列表工具栏主按钮加 `cat2bug-btn-outline`；暗色为 `.list-page-toolbar` / `module-tools` / `project-option-list-tools` 内 `primary.is-plain` 兜底描边，不再实心蓝底
- [x] 6.3 更新 `docs/04-UI规范/组件与交互约定.md` 为 16/56 与暗色描边约定；版本记录补一句；写入暗色 plain 实心化踩坑
- [x] 6.4 浅色/暗色再验 OpenAI 账号与交付物：行高可感、主钮非实心（代码侧：token 16/56 + 工具栏 outline 覆盖；请本地硬刷新后再看一眼）

备注（6.2）：浅色全局也会把无 outline 的 primary plain 画成实心，因此浅色同样加了工具栏作用域兜底。

备注（6.4 补丁）：验收仍无感是因为 `Cat2BugTable` scoped 写死 `padding: 5px 0`，盖掉全局表格轨；已改为引用 `--cat2bug-table-cell-padding-y`。见踩坑 [[Cat2BugTable写死cell-padding盖掉表格轨]]。
