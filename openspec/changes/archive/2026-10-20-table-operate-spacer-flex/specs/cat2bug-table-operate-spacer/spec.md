## ADDED Requirements

### Requirement: 操作列前注入弹性空列填满剩余宽度

当 `Cat2BugTable` 存在操作列（`class-name` 含 `cat2bug-operate-column`）且可见业务列（含 prepend 辅助列）与操作列之外的主表区域仍有剩余宽度时，组件 MUST 在操作列之前渲染一列幽灵弹性列（spacer）。spacer 的宽度 MUST 等于该剩余宽度（允许与现有 flex 容差同量级的忽略阈值）。操作列 MUST 继续贴齐表格容器右缘（`fixed="right"` 时）。

#### Scenario: 拖窄业务列后操作列仍贴右

- **WHEN** 用户将可见业务列总宽拖至小于表格容器可用主区宽度
- **THEN** spacer 出现在操作列左侧并占满剩余空间，操作列贴齐容器右缘，容器右侧不得出现大块空白空洞

#### Scenario: 列总宽溢出时不占 spacer

- **WHEN** 可见业务列总宽已大于或等于主表可用宽度（需要横向滚动）
- **THEN** spacer 宽度为 0 或不产生可见占位，横向滚动与自定义横滚条行为不因 spacer 退化

### Requirement: spacer 不可配置且无业务内容

spacer MUST NOT 出现在列选择器中，MUST NOT 写入列配置本地缓存或 `columns-change` 载荷，MUST NOT 显示列标题文案、拖序手柄或调宽手柄，MUST NOT 渲染业务单元格内容。

#### Scenario: 列配置与选择器不受污染

- **WHEN** 用户打开显示字段选择器或触发列配置持久化
- **THEN** 选项与缓存中不含 spacer；仅含业务列与既有约定字段

### Requirement: 有 spacer 时仅 spacer 吃剩余宽度

当本轮布局判定需要 spacer 且剩余宽度大于容差时，组件 MUST 将剩余宽度全部分配给 spacer，MUST NOT 同时再把同一份剩余宽度按业务列 `flex: true` 再次分配。

#### Scenario: 不与业务 flex 双弹性打架

- **WHEN** 某业务列标记 `flex: true` 且用户已手动调过列宽，同时总宽仍小于容器
- **THEN** 剩余宽度由 spacer 吸收，该业务列保持用户宽度，不得被二次撑开

### Requirement: spacer 表头与业务列视觉对齐

spacer 列表头 MUST 与同表业务列表头在高度、底部分隔与背景上对齐；允许无标题文字，但不得呈现为「缺了一截表头」的断裂感。

#### Scenario: 表头一条齐平

- **WHEN** spacer 可见且宽度大于 0
- **THEN** 表头横贯至操作列左侧，spacer 表头区域与相邻业务列表头齐平对齐
