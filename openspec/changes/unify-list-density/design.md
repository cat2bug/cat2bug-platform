## Context

控件高度 token `--cat2bug-control-height`（32px）已覆盖工具栏与表单；Element Plus `default`/`small` 编译高度同为 32px。列表行原用 EP 默认 cell 上下 padding（default = **8px**，单行约 8+23+8 ≈ **39px**）。裸 `el-table` 约占视图 40 页，`Cat2BugTable` 仅缺陷/用例/计划/文档等少数页。长期约定见 `docs/04-UI规范/组件与交互约定.md`。

**首轮实现已合入代码（任务 1–5 勾完），但验收失败（肉眼无感）：**

1. **表格轨增量过小**：把 padding-y 从 8 提到 12、min-height 提到 40，单行大约只到 **~47px**（相对改前约 **+8px**）。并排对比才看得出，单独看像「没变」。EP 的 `large` 也是 12px padding，我们等于刚踩到 large，远不够「松一点」的产品预期。
2. **暗色 plain 被盖成实心**：`theme-dark.scss` 把 `.el-button--primary.is-plain`（无 `cat2bug-btn-outline`）与实心 primary **画成同一种蓝底白字**。列表工具栏虽已标 `plain`，暗色下仍像实心蓝块，「与筛选同重量」在暗色验收页（OpenAI 账号）上等于 0。

因此本 design **修订表格轨数值与暗色工具栏约定**；首轮迁移（交付物 / OpenAI → Cat2BugTable + `.list-page-toolbar`）与树透传保留，不回滚。

## Goals / Non-Goals

**Goals:**

- 全站业务表格单行**肉眼可感**地更松（见 Decision 1 修订值），操作列垂直节奏跟表格轨。
- 列表工具栏主操作与筛选**在浅色与暗色**都呈同轨视觉重量：描边/轻底 primary，高度仍 32px；暗色不得再把工具栏 plain 画成实心。
- 全局样式继续惠及所有 `el-table`；首批交付物、OpenAI 账号已迁 `Cat2BugTable` + 标准工具条（保留）。
- 文档与 token 与修订后的表格轨 / 暗色工具栏约定对齐。

**Non-Goals:**

- 一次迁完全部约 40 个裸表页（后续批次另开或列为可选）。
- 改对话框/表单密度、改 32px 控件轨、改品牌主色。
- 改分页组件尺寸体系（可顺带对齐工具栏高度，但不重做 Pagination API）。
- 后端、权限、导出契约不变。
- 不为「视觉统一」把行内操作改成实心按钮。
- 不把全站所有 `primary plain`（含对话框外其它场景）一律改成描边——暗色实心 plain 的历史用法用「工具栏作用域 / `cat2bug-btn-outline`」局部纠正，避免误伤。

## Decisions

1. **双轨密度，不抬控件轨；表格轨上调到肉眼可感**  
   - 控件轨：继续 32px（`--cat2bug-control-height` / `--cat2bug-toolbar-control-height`）。  
   - 表格轨（**修订，取代首轮 40 / 12**）：  
     - `--cat2bug-table-cell-padding-y: **16px**`（相对 EP default 8px，每边 +8px）  
     - `--cat2bug-table-row-min-height: **56px**`（约 16 + 行高 23 + 16 ≈ 55，取整 56）  
     - 数据行最高仍 100px（`--cat2bug-table-row-max-height`）  
   - 验收标准：不并排对比也能感到「行更松」，而不是 Diff 级别的 4px。  
   - 否决：继续停留在 12/40——已实测无感。  
   - 否决：把控件抬到 36/40px——会冲掉已对齐的工具栏与旧 Element UI small 基线。  
   - 若 56 仍偏紧，优先再加 padding-y（如 18），而不是抬控件轨。

2. **全局 CSS 抬表格呼吸，再迁页**  
   - 在 `cat2bug.scss` 对业务表 `td`/`th` `.el-table__cell` 用上述 token 设上下 padding 与 body `min-height`；`Cat2BugTable` 与裸表同时受益。  
   - 操作列（`small-padding` / `cat2bug-operate-column`）cell 内 `align-items: center`，内层 min-height 随表格轨公式更新。  
   - 否决：只改 Cat2BugTable——旧页要等迁移才松。

3. **工具栏主按钮：plain + 暗色必须真描边**  
   - 列表工具栏「新建 / 主要动作」：`type="primary" plain`，高度 32px；禁止同排混用无 plain 的实心 primary。  
   - **暗色补丁（新增）**：仅标 `plain` 不够。须保证暗色下为描边/轻底，与输入框同重量。实现二选一（可并用）：  
     a. 触及页主按钮加既有 class `cat2bug-btn-outline`（暗色已有描边规则）；或  
     b. 在 `theme-dark.scss` 为 `.list-page-toolbar`（及等价 `module-tools` / `project-option-list-tools`）内的 `.el-button--primary.is-plain` 覆盖为描边样式，**不再**走「plain = 实心蓝底」那条全局规则。  
   - 推荐：**a + 必要时 b**——页面显式 outline，工具栏选择器兜底，避免漏加 class 又回到实心。  
   - 保留实心 primary 用于对话框确认、登录等非列表工具栏场景。  
   - 否决：只改标签为 `plain`、不碰暗色规则——已验收失败。

4. **清理列表工具栏 `size="small"`**  
   - 高度本已强制 32px；去掉标签减少「小号」误读。不强制改对话框内每个 `size`。（首轮已做，保持。）

5. **首批迁移：交付物 + OpenAI 账号（已完成，保持）**  
   - `.list-page-toolbar` + `Cat2BugTable`；交付物透传 `row-key` / `tree-props` / `default-expand-all` / `highlight-current-row`。  
   - 展开图标在首列，表头拖序/pin 在 th，不拦截树节点点击。  
   - 验收：浅/暗色下相对改前行高明显更松；暗色工具栏主钮为描边而非实心蓝块。

6. **拟新增 ADR：无**  
   - 密度与暗色工具栏约定写入 `组件与交互约定`（及必要时主题文档一句）；不单独占 ADR，除非后续要改 32px 控件轨。

7. **踩坑（本轮若落地则回写）**  
   - 候选：暗色下 `primary.is-plain` 被全局画成实心，列表工具栏「视觉同轨」失效——须 `cat2bug-btn-outline` 或工具栏作用域覆盖。实现确认后写入 `docs/06-踩坑记录/`。

## Risks / Trade-offs

- [一屏行数再减] → 56px 相对 39px 约少 30% 行数；产品已选「松一点」接受；最大行高 100px 不变。  
- [暗色 outline 与历史「黄/实心 plain」混用] → 用工具栏作用域或显式 `cat2bug-btn-outline`，不改全站所有 plain。  
- [操作列 `small-padding` 命名误导] → 文档注明；本次仍可不重命名 class。  
- [树表与 Cat2BugTable] → 首轮已透传；本轮无新风险。

## Migration Plan

**已完成（首轮）：** token 骨架、操作列居中、工具栏 plain 标签、两页迁 Cat2BugTable、文档初稿。

**本轮（修订验收）：**

1. 上调 `--cat2bug-table-cell-padding-y` → 16、`--cat2bug-table-row-min-height` → 56；同步操作列内层 min-height 公式。  
2. 暗色：列表工具栏主按钮真描边（`cat2bug-btn-outline` 及/或 `.list-page-toolbar` 覆盖）。  
3. 更新 `docs/04-UI规范/组件与交互约定.md` 数值与暗色约定；版本记录补一句或改原行；有踩坑则写踩坑。  
4. 浏览器浅/暗色再验 OpenAI 账号与交付物：行高可感、主钮非实心。  
5. 回滚：还原 token 数值与暗色工具栏规则即可。

## Open Questions

无。已按 **16 / 56** 落地；暗色与浅色均做了 **`cat2bug-btn-outline` + 工具栏作用域兜底**。若验收仍偏紧，再把 padding-y 提到 18。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`
- `docs/04-UI规范/主题与配色.md`
- `docs/00-项目总览.md`
- `docs/08-版本记录/2026-10.md`
- （本轮落地后）`docs/06-踩坑记录/` 暗色 primary plain 实心化（若确认）

拟新增 ADR：无
