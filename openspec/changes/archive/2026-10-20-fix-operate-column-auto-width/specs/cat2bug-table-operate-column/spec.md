## ADDED Requirements

### Requirement: 操作列按内容自动宽度且不反馈放大

当 `operateColumnAutoWidth` 为真时，`Cat2BugTable` SHALL 根据当前页操作列内容的**固有宽度**（克隆节点或可见子控件累加，含间距与水平 padding）计算列宽，并将结果限制在 `operateColumnMinWidth` 与 `operateColumnMaxWidth` 之间（默认 88～450px）。测宽 MUST NOT 使用已被当前列宽撑开的容器 `scrollWidth` / `getBoundingClientRect().width` 作为内容宽度的上界输入，以免反馈放大到上限。

#### Scenario: 短操作列按内容收窄

- **WHEN** 操作列仅含「修改」「删除」等少量 link 按钮（如用例列表）
- **THEN** 应用后的操作列宽明显小于 `operateColumnMaxWidth`，且与按钮内容贴合（允许 padding 与 gap），不得常驻接近 450px 的空白列

#### Scenario: 测宽不读撑开后的 live 宽

- **WHEN** 操作列 CSS 占位或上次布局已使单元格宽接近上限
- **THEN** 下一次 `syncOperateColumnWidths` 仍按内容固有宽计算；若内容固有宽 + padding 未超上限，列宽 MUST 收到该值，不得因 live 容器宽而保持上限

### Requirement: 未测出内容前使用短占位

在尚未成功测出有效操作内容宽度之前，`--cat2bug-operate-col-applied-width`（及等效 DOM 列宽）MUST 使用不超过 `operateColumnMinWidth` 量级的占位（默认与下限一致），MUST NOT 默认占满 `operateColumnMaxWidth`。

#### Scenario: 首帧不顶满上限

- **WHEN** 表格刚挂载、自动宽尚未写出最终值
- **THEN** 操作列视觉宽度接近下限占位，而非以 450px 作为默认 applied 宽

### Requirement: 超上限才封顶换行

当内容固有宽 + 水平 padding 超过 `operateColumnMaxWidth` 时，系统 SHALL 将列宽设为上限，并设置 `data-operate-at-cap="true"`，允许操作工具区内换行；未超过时 MUST 保持单行且 `data-operate-at-cap` 为 false（或等价未封顶状态）。

#### Scenario: 多按钮操作列可到上限

- **WHEN** 缺陷列表等操作列按钮较多，固有宽超过上限
- **THEN** 列宽等于 `operateColumnMaxWidth`，且允许操作区在封顶状态下换行

#### Scenario: 未超上限保持单行

- **WHEN** 内容固有宽 + padding 未超过上限
- **THEN** 操作工具区保持单行不换行，列宽为内容所需宽度（夹在上下限之间）
