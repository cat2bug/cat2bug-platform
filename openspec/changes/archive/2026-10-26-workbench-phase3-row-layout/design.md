## Context

- 承接已完成的 `project-workbench` 一期（轻定制 + 预设 + 双页签）与二期（12 列自由栅格）；产品尚未对外发布，允许 **BREAKING** 换配置模型。
- 探索结论：用户需要「按行设列数与列宽比例 → 每列指定/拖入组件」的编辑心智；看板页签废弃；文档列钉选单篇 MD；高度支持文档高度 / 行高度 / 指定高度；动态 Tab 延后但布局字段同构。
- 现状代码中心：`cat2bug-platform-ui/src/views/system/dashboard/`（`index.vue`、`workbench/presets.js`、`grid-layout.js`、`WorkbenchFreeGrid.vue`、`widget-registry.js`）；MD 打开路径已有 `HandleMarkdownDocument` + `system:document:edit`。
- 不推翻已有 ADR；不新增运行时依赖。

## Goals / Non-Goals

**Goals:**

- 工作台以 `version: 3` 的 `rows[]` 为唯一真相源；查看态与编辑态均按行渲染。
- 提供行排版编辑器：列数、比例、高度模式；槽位拖入 + 指定组件。
- 去掉看板 Tab；统计类 widget 仍可注册，默认不出现在工作台预设。
- 落地 `document-pin`：绑定 `docId`（仅 md），列内卡片展示，点击进预览/编辑。
- 预设与默认布局全部改写为 rows；删除对二期 `layout[]` / `packManagerWorkLayout` 的依赖路径。

**Non-Goals:**

- 不实现动态 Tab UI（仅预留「每 Tab 一份 rows」形状）。
- 不做 v1/v2 → v3 配置迁移与双写。
- 不在列内提供完整 Markdown 编辑器（编辑走文档页）。
- 不引入 `grid-layout-plus` 等新 npm 依赖。
- 不改文档存储（仍走现有文档模块 / ADR-007 体系）。

## Decisions

### D1. 配置 schema：`version: 3` + rows/cols/instance

```json
{
  "version": 3,
  "preset": "manager",
  "tabs": {
    "work": {
      "rows": [
        {
          "id": "r1",
          "height": { "mode": "fixed", "px": 280 },
          "cols": [
            {
              "id": "c1",
              "ratio": 24,
              "height": null,
              "widget": {
                "instanceId": "i1",
                "type": "team-weekly-report",
                "props": {}
              }
            }
          ]
        }
      ]
    }
  }
}
```

- **行**：`id`、`height`、`cols[]`。
- **列**：`id`、`ratio`（**24 栅格整数 span**，同行总和恒为 24，单列 ≥ 2）、可选 `height` 覆盖行默认、`widget` 可空（空槽）。
- **组件实例**：`instanceId`（槽内唯一）+ `type`（注册表 id）+ `props`；允许同 `type` 多实例（钉多篇文档）。
- **废弃**：`widgets[]` 扁平列表与 `layout[]` 栅格不再写入；读到旧 version 时直接套当前角色预设（不迁字段）。
- **备选（否）**：继续 12 列自由栅格 + 编辑器伪装成行 → 双向同步成本高，且与「比例一等公民」冲突。

### D2. 高度三态

| mode | 含义 |
|------|------|
| `content` | 按槽内内容自然高度（文档列按卡片/摘要策略）；溢出由组件内部处理 |
| `row` | 同行各槽对齐到该行最高内容高度（默认拉伸填满） |
| `fixed` | 指定 `px`（或编辑器提供的 S/M/L 映射到 px）；槽内滚动 |

- 行设默认；列 `height` 非空则覆盖该列。
- **备选（否）**：仅固定行高 → 文档列体验差。

### D3. 编辑器交互

- 编辑态替换现有「勾选 + 自由拖尺寸」主路径为 **行编辑器**。
- 行工具：增删行、设列数（**上限 4**，超过需先拆行）、比例（**下拉选 24 栅格预设**如 `12:12` / `14:10`；拖拽列缝按整数格吸附，总和保持 24）、行高度模式。
- 空槽：左侧（或抽屉）组件库 **拖入**；或点槽 **指定** 类型（文档类型再选 md `docId`）。
- 已填槽：拖到其他空槽/互换；移除回空槽。
- 小屏：行内列纵向堆叠，忽略比例；编辑仍可改结构。
- **备选（否）**：只保留拖或只保留下拉 → 与产品「两者都要」不符。

### D4. 工作台 | 看板双页签（产品回补）

- 壳层保留页签「工作台 | 看板」：工作台用 v3 `rows`；看板用 `tabs.board.widgets`（项目级统计墙，勾选编辑）。
- `tabAffinity: 'board' | 'both'` 的组件进看板组件库；工作台行编辑器仍只列 work/both。
- 预设同时输出 `tabs.work.rows` 与 `tabs.board.widgets`。

### D5. `document-pin` 展示与跳转

- 列内：**卡片壳**（标题、可选更新时间、截断摘要或「Markdown 文档」徽标），不内嵌完整编辑器。
- 点击 → `HandleMarkdownDocument`；`canEdit = checkPermi(['system:document:edit'])` 决定可编/预览（复用现页）。
- 选文：项目文档列表过滤 `fileExtension in {md, markdown}`；无权限/已删 → 空态 + 重选。
- **备选（否）**：列内 Tiptap 全预览 → 高度与性能成本高，且与「点击跳转」叙事重复；若后续需要可加 `props.preview: 'embed'`。

### D6. 预设与默认行

- `tester` / `developer` / `manager` / `blank` 的 `buildConfigFromPreset` 输出 v3 `rows`。
- 管理顶区「周报 | 类型 KPI」用一行多列表达，删除 `packManagerWorkLayout` / `managerWorkLayoutBroken` 特判路径（或仅作一次性开发期对照后删除）。
- `alignLegacyConfig`：遇 `version < 3` 或无 `rows` → 重置为当前预设（确认提示可保留）。

### D7. 动态 Tab 预留

- 配置保持 `tabs.<tabId>.rows`；本阶段仅实现 `work`。
- 编辑器 API 接受 `tabId`，避免写死后难抽。

### D8. 依赖与 ADR

- 布局用 CSS flex/grid + 现有拖拽手势；不新增 npm。
- **拟新增 ADR：无**（决策记本 design）。

## Risks / Trade-offs

- [行模型表达力不如自由叠放] → 接受；产品明确行心智；复杂布局用多行。
- [同 type 多实例状态/刷新] → `instanceId` 作 key；组件 props 驱动数据。
- [去掉看板后统计入口变少] → 组件保留；动态 Tab 再挂；必要时工作台预设可选加少量统计行。
- [旧本地/服务端 JSON] → 未发布，直接覆盖为预设；日志可记 version mismatch。
- [文档摘要拉取成本] → 卡片可先只显示元数据，摘要按需或截断正文 API（若无则标题-only）。

## Migration Plan

1. 前端发布 v3 读写；服务端 JSON 原样存储。
2. 用户打开工作台：若配置非 v3，静默或轻提示后套预设并保存。
3. 回滚：回退前端版本即可（库内旧 JSON 可被再次覆盖）。

## Open Questions

- 文档卡片是否展示正文前 N 字：实现时若有现成内容 API 则做截断，否则标题+元数据即可（不阻塞）。
- 动态 Tab 的权限/命名：另开变更；本设计只保证 rows 同构。

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/01-产品需求/产品定位与核心功能.md`
- `docs/05-接口文档/接口清单-系统与监控.md`（`/system/workbench` v3）
- `docs/07-技术决策/ADR-007-文档正文存每项目隐藏Git仓库.md`（文档存储，本变更只消费）
- `docs/07-技术决策/ADR-019-可视化Markdown统一用Tiptap连续A4纸面.md`（MD 编辑页，跳转复用）
- `docs/07-技术决策/ADR-020-工作台v3行排版与24栅格.md`
- `docs/06-踩坑记录/工作台Close图标与ListDefectLog重名导致编辑态崩溃.md`
- `docs/06-踩坑记录/Flex百分比列宽叠加gap导致行右侧留白或溢出.md`
- `openspec/changes/project-workbench/design.md`（一期/二期上下文，只引用）
