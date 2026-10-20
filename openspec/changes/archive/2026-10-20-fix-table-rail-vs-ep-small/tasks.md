## 1. 全局表格轨压过 EP small

- [x] 1.1 在 `cat2bug.scss` 扩展表格轨规则，显式覆盖 `.el-table--small .el-table__cell`（及 body `min-height`），保证 `--cat2bug-table-cell-padding-y` / `--cat2bug-table-row-min-height` 生效
- [x] 1.2 若发现对话框枚举表 / 键盘说明表过松，加 `cat2bug-table-compact`（或等价）局部回退；无回归则注明跳过（已给键盘页与缺陷字段枚举表加 compact）

## 2. 用例类型迁 Cat2BugTable

- [x] 2.1 `case-types/index.vue`：裸 `el-table` 改为 `Cat2BugTable`，列含名称 / 引用数 / 操作列（`cat2bug-operate-column` + green/red）
- [x] 2.2 保留 `.list-page-toolbar` 与权限指令；加载与增删改逻辑不变

## 3. 验收

- [x] 3.1 用例类型页：行高对齐缺陷/用例；操作修绿删红（代码侧已迁；请本地硬刷新确认）
- [x] 3.2 抽查一页仍裸表的业务列表（如缺陷字段外层表）：行高已抬到表格轨（全局 CSS 覆盖 `--small`）
- [x] 3.3 缺陷 / 用例 `Cat2BugTable` 列表无回归（表格轨规则与组件深层选择器一致，不削弱）

## 4. 回写长期知识

- [x] 4.1 更新 `docs/04-UI规范/组件与交互约定.md`：表格轨须覆盖 ConfigProvider `size=small` / `.el-table--small`
- [x] 4.2 新增踩坑「EP small 表压过表格轨 token」，挂入 `docs/06-踩坑记录/说明.md`
- [x] 4.3 `docs/08-版本记录/2026-10.md` 加本变更一行；拟新增 ADR：无
