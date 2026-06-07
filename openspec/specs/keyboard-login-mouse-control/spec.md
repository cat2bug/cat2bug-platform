## ADDED Requirements

### Requirement: 登录页 Cmd/Ctrl 方向键 D-pad

登录页 SHALL 提供独立于全局快捷键引擎的老鼠键盘操控：用户按住 **Cmd/Ctrl** 时在左下角显示四向 D-pad，并可通过 **⌘/Ctrl + 方向键** 或 D-pad 触控移动装饰老鼠。

该能力 MUST NOT 注册到 `registerPage` 或键盘设置页。

#### Scenario: 按住 Cmd 显示 D-pad

- **WHEN** 用户在登录页按住 Cmd/Ctrl（非输入组合态）
- **THEN** 左下角显示四向 D-pad 控件

#### Scenario: 方向键移动老鼠

- **WHEN** 用户保持 Cmd/Ctrl 并按下方向键
- **THEN** 老鼠按对应方向奔跑移动

#### Scenario: D-pad 触控移动

- **WHEN** 用户按住 D-pad 某一方向按钮
- **THEN** 老鼠持续向该方向移动，直至松开

### Requirement: 松开方向键后高亮消失并坐立

用户松开方向键后，D-pad 对应按钮高亮 MUST 立即或在 keyup 丢失兜底超时内消失；当无任何移动键按下时，老鼠 MUST 播放坐立动画，**即使 Cmd/Ctrl 仍按住**。

再次按下方向键 MUST 从坐立态恢复键盘控制奔跑。

#### Scenario: 仅松开方向键

- **WHEN** 用户松开方向键但继续按住 Cmd/Ctrl
- **THEN** D-pad 高亮消失，老鼠进入坐立动画

#### Scenario: 再次按下方向键

- **WHEN** 老鼠处于坐立态且用户再次按下方向键（Cmd/Ctrl 仍按住）
- **THEN** 老鼠恢复奔跑移动

### Requirement: 手动控制不得越出左右屏幕

键盘、右键拖拽、D-pad 等**手动控制**模式下，老鼠整身（含精灵绘制宽度）MUST 保持在画布左右边界内。

自动奔跑模式 MAY 从屏外进入并在越界后折返，不受本约束。

#### Scenario: 贴左边界

- **WHEN** 用户持续向左移动且已到达左边界
- **THEN** 老鼠停止向左移动，不会露出屏幕左侧

#### Scenario: 贴右边界

- **WHEN** 用户持续向右移动且已到达右边界
- **THEN** 老鼠停止向右移动，不会露出屏幕右侧

### Requirement: macOS 方向键 keyup 兼容

在 macOS 上 ⌘+方向键可能丢失 keyup 时，系统 MUST 通过键盘焦点 sink、keyup 优先处理与 release poller 推断松键，保证 D-pad 高亮与老鼠移动状态一致。

#### Scenario: keyup 丢失时高亮清除

- **WHEN** 系统未投递方向键 keyup 但用户已物理松开方向键
- **THEN** 在合理超时内 D-pad 高亮清除且老鼠停止该方向移动
