## Why

用例等「修改 + 删除」短操作列本应按内容收窄到约 150–180px，却常卡在 **450px** 上限，右侧大片空白。根因是 `Cat2BugTable` 操作列自动测宽：CSS 默认占位 450px，测宽又把「已被撑开」的 live 宽度吃进结果，形成反馈放大。缺陷列表按钮多、接近上限，问题不显眼；短操作列一曝光。

## What Changes

- 修正 `Cat2BugTable` 操作列自动宽度算法：测宽只认内容固有宽，禁止用已被列宽撑开的 DOM live 宽反馈放大。
- 未测出有效内容前，占位宽度改为接近下限（或保守短占位），不再默认顶满 450px。
- 保持既有契约：自动宽仍在 **88～450px**；超宽才 `data-operate-at-cap` 换行；缺陷多按钮操作列行为不退化。
- 回写 `docs/04-UI规范/组件与交互约定.md`（测宽原则一句）与当月版本记录；可复用踩坑写入 `docs/06-踩坑记录/`。

## Capabilities

### New Capabilities

- `cat2bug-table-operate-column`: Cat2BugTable 操作列自动宽度——按内容收窄、上限换行、测宽不得反馈放大。

### Modified Capabilities

- （无）`cat2bug-table-header-handles` 已约定操作列无调宽手柄、宽由自动逻辑决定；本次不改其需求，只补独立操作列宽规范。

## Impact

- 前端：`cat2bug-platform-ui/src/components/Cat2BugTable/index.vue`（`syncOperateColumnWidths` / 测宽 helper）；可能微调 `cat2bug.scss` 中 `--cat2bug-operate-col-applied-width` 默认占位。
- 受益页：用例列表及所有 append `cat2bug-operate-column` 短操作列；缺陷等多按钮页应仍可到 450 封顶。
- 无后端 / API / **BREAKING** 变更。
