# Spec: Cat2BugTable 表头拖序与调宽手柄

## ADDED Requirements

### Requirement: 表头 hover 显示拖序与调宽手柄

`Cat2BugTable` 对支持自定义表头的数据列（`pinFixedToggle !== false` 且非操作列 / prepend 辅助列）SHALL 在表头 **hover** 时显示两个控件：**拖序手柄**（圆角矩形内六点图标，位于 pin **左侧**）与 **调宽竖条**（圆角竖条，位于列 **右缘**）。未 hover 时两控件 MUST 不可见且不可交互（`opacity: 0` 与 `pointer-events: none` 或等效实现）。

#### Scenario: hover 显示手柄

- **WHEN** 用户将指针移入某数据列表头单元格
- **THEN** 该列显示拖序手柄与调宽竖条，且 pin 与标题仍可见

#### Scenario: 离开表头隐藏手柄

- **WHEN** 指针离开该列表头且未处于拖序或调宽操作中
- **THEN** 拖序与调宽手柄恢复不可见且不可交互

#### Scenario: 操作列无调宽手柄

- **WHEN** 列为 append 操作列（`cat2bug-operate-column`）或 `operateColumnAutoWidth` 管辖列
- **THEN** 表头不显示调宽竖条，列宽仍由现有自动列宽逻辑决定

#### Scenario: prepend 辅助列无手柄

- **WHEN** 列为 `prepend` 槽插入的固定辅助列（如侧栏展开列）
- **THEN** 不显示拖序与调宽手柄

### Requirement: pin 与手柄布局顺序及位置稳定

表头控件从左到右顺序 MUST 为：拖序手柄 → pin 图标 → 列标题 → 排序箭头（若可排序）→ 调宽竖条（叠在右缘）。**pin 点击切换左固定** 的行为 MUST 与变更前一致（`header-left` / `header-right` 图标）。

在未 hover 与 hover 之间，**pin、列标题、排序箭头的水平位置 MUST 不得发生位移**。拖序手柄 MUST 通过固定宽度占位（未 hover 时透明）实现；调宽竖条 MUST 使用绝对定位贴列右缘，不参与 flex 挤压标题与排序区。

#### Scenario: hover 前后 pin 位置不变

- **WHEN** 用户对同一列表头先后观察未 hover 与 hover 状态
- **THEN** pin 图标左缘与标题文字起点的水平像素位置一致

#### Scenario: hover 前后排序箭头位置不变

- **WHEN** 列为可排序列且用户观察未 hover 与 hover 状态
- **THEN** 排序箭头相对列标题的位置不因调宽竖条出现而水平移动

#### Scenario: pin 点击仍切换固定

- **WHEN** 用户点击 pin 图标（非拖序手柄）
- **THEN** 该列 `fixed` 状态切换，列顺序在固定区与普通区规则下重排，且 **一次点击即生效**

### Requirement: 拖序仅通过手柄触发

列顺序调整 MUST 仅能通过拖序手柄发起。Sortable（或等效）SHALL 将 `handle` 限定为拖序手柄元素，且拖序 MUST NOT 与 pin 点击、列排序点击冲突。

#### Scenario: 拖序手柄改变列顺序

- **WHEN** 用户按住拖序手柄将列拖到同区内新位置并释放
- **THEN** 列顺序更新，固定区与普通区分区规则与变更前一致，且顺序写入本地 field-list 缓存

#### Scenario: 拖动标题不触发列序变更

- **WHEN** 用户在列标题文字区域按下并拖动（非拖序手柄）
- **THEN** 列顺序不改变

### Requirement: 调宽手柄改变列宽并持久化

用户通过调宽竖条调整列宽时，系统 SHALL 更新该列 `width`（像素整数），且列宽 MUST 不小于 **60px**。调宽结果 MUST 与 `visible`、`fixed`、列顺序一并写入同一 field-list 本地缓存（debounce 写入，避免高频刷盘）。无用户 `width` 时，系统 SHALL 回退列 defaults 中的 `width` 或 `width_<locale>` 或组件默认宽度。

#### Scenario: 拖宽增加列宽

- **WHEN** 用户向右拖动调宽竖条使列宽增加并释放
- **THEN** 该列显示宽度增加，刷新页面后仍保持新宽度

#### Scenario: 列宽下限

- **WHEN** 用户试图将列宽拖至小于 60px
- **THEN** 列宽停留在 60px

#### Scenario: 调宽写入缓存

- **WHEN** 用户完成一次调宽操作
- **THEN** field-list 缓存中对应列条目包含更新后的 `width` 字段

### Requirement: 固定列双表头与表格基础设施兼容

左固定列复制表头时，拖序/调宽手柄与占位规则 MUST 在主表与 `.el-table__fixed-header-wrapper` 中一致。调宽或拖序后 MUST 触发 `doLayout` 及现有固定列行高同步、自定义横向滚动条更新逻辑，不得导致固定列与主表体错位。

#### Scenario: 固定列调宽后布局一致

- **WHEN** 用户对左固定区某列调宽
- **THEN** 主表与左固定表该列宽度一致，行高无错位

#### Scenario: 横向滚动条仍可用

- **WHEN** 多列总宽超出视口且用户调宽或拖序后
- **THEN** 自定义横向滚动条（若启用）仍正确反映可滚动范围

### Requirement: 全站 Cat2BugTable 启用

所有使用 `Cat2BugTable` 的列表页（缺陷、用例、计划、文档、报告及计划抽屉内嵌列表）SHALL 默认启用上述表头手柄，无需每页单独开关。

#### Scenario: 缺陷列表启用手柄

- **WHEN** 用户在缺陷列表表格模式 hover 数据列表头
- **THEN** 可见拖序与调宽手柄（排除操作列与 prepend 列）

### Requirement: 暗色主题下手柄可见

暗色主题下拖序手柄与调宽竖条 MUST 使用与表头一致的 CSS 变量/token，保证可见性与可点击性。

#### Scenario: 暗色主题 hover 手柄

- **WHEN** 系统处于暗色主题且用户 hover 数据列表头
- **THEN** 拖序与调宽手柄可见且与表头背景对比清晰
