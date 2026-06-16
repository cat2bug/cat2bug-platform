## ADDED Requirements

### Requirement: 必填列表头与拖序调宽手柄共存

在启用表头拖序/调宽手柄后，必填列（`required: true`）的红色标题样式 MUST 仍生效。拖序手柄、pin、必填红色标题、排序箭头在未 hover 与 hover 之间 MUST 保持与「表头手柄布局稳定性」要求一致的水平位置；必填色 MUST 仅作用于标题文字（及现有 pin 必填同色规则），不得因手柄显隐而改变 pin 或排序箭头位置。

#### Scenario: 必填列 hover 手柄且标题仍红色

- **WHEN** 列配置 `required: true`（如缺陷名称）且用户 hover 该列表头
- **THEN** 拖序与调宽手柄按规则显示，标题文字仍为红色，pin 与标题位置不跳变

#### Scenario: 必填列调宽后样式保持

- **WHEN** 用户对必填列调宽并刷新页面
- **THEN** 该列宽度为用户设置值，标题仍为红色且无星号前缀
