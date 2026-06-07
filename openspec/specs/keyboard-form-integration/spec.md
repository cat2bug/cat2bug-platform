## ADDED Requirements

### Requirement: 抽屉表单直接组合键

缺陷新建/编辑等抽屉表单 SHALL 支持以下直接组合键（不经过引导键面板），由 `dialog-form-shortcuts` mixin 与 `defect-drawer-shortcuts` 单例在抽屉 `visible=true` 时绑定：

- `Cmd/Ctrl + Enter`：保存（`submitForm` 或 `shortcutSave`）
- `Esc`：关闭（`requestCloseDefectFormDrawer`）；有未保存修改时弹出确认

抽屉 MUST 设置 `:close-on-press-escape="false"`，由全局捕获监听统一处理 `Esc`。当下拉/日期等浮层仍打开时，`Esc` MUST 先交给浮层自身。

#### Scenario: 抽屉内保存

- **WHEN** 用户在新建缺陷抽屉内按下 `Cmd/Ctrl + Enter`
- **THEN** 触发保存逻辑

#### Scenario: 抽屉内关闭

- **WHEN** 用户在新建缺陷抽屉内按下 `Esc` 且无其它遮挡浮层
- **THEN** 关闭抽屉；若有未保存修改则先确认

#### Scenario: 有浮层时 Esc 不关闭抽屉

- **WHEN** 成员选择下拉已展开且用户按下 `Esc`
- **THEN** 关闭下拉，不关闭缺陷抽屉

### Requirement: 抽屉标题栏快捷键徽标

新建/编辑缺陷抽屉标题栏按钮的视觉提示 MUST 遵循：

- **关闭**按钮：任何情况下不显示快捷键徽标
- **保存缺陷**按钮：未按住 `Cmd/Ctrl` 时不显示徽标；按住 `Cmd/Ctrl` 时仅在按钮右下角显示 `↵`（表示 `Cmd/Ctrl+Enter`）

松开 `Cmd/Ctrl` 后 MUST 立即隐藏保存按钮 `↵` 徽标。

#### Scenario: 未按修饰键时保存按钮无徽标

- **WHEN** 用户打开新建缺陷抽屉且未按住 `Cmd/Ctrl`
- **THEN** 保存按钮不显示任何快捷键提示

#### Scenario: 按住修饰键时保存按钮显示回车

- **WHEN** 用户在抽屉内按住 `Cmd/Ctrl`
- **THEN** 保存按钮右下角显示 `↵`，关闭按钮仍无徽标

### Requirement: 表单字段 Cmd/Ctrl 字母聚焦

抽屉内表单 SHALL 支持按住 `Cmd/Ctrl` 显示字段字母/数字徽标，保持修饰键时再按对应键将焦点移至对应字段，并显示全站统一焦点环（不再使用临时荧光动画）。

组合组件（成员选择、交付物选择、文件/图片上传）MUST 聚焦外框而非内部 input，以避免双层焦点环。

**视口分配：** 快捷键 MUST 仅分配给当前滚动视口内可见的表单项（垂直可见 ≥ 8px）。滚动表单时，若仍按住 `Cmd/Ctrl`，徽标与键位映射 MUST 随视口刷新。字母跳转后 MUST 允许在仍按住 `Cmd/Ctrl` 时连续跳转下一字段。

**系统编辑组合：** `A`/`C`/`V`/`X`/`Z` MUST NEVER 分配给字段，且 MUST NOT 被字段聚焦逻辑拦截，以保留全选/复制/粘贴/剪切/撤销。

**滚动：** 按住 `Cmd/Ctrl` 时 `↑`/`↓` MUST 滚动表单属性区（若存在可滚动容器）。

**固定快捷键：** 「保存本次选项」复选框 MUST 使用 `O`（`getFixedFieldHints`），不占用标签徽标注入。

#### Scenario: 修饰键显示徽标

- **WHEN** 用户在新建缺陷抽屉内按住 `Cmd/Ctrl`
- **THEN** 当前可视区域内各字段标签显示分配的字母或数字徽标

#### Scenario: 字母跳转字段

- **WHEN** 用户保持 `Cmd/Ctrl` 并按下某可见字段对应字母或数字
- **THEN** 焦点移至该字段并显示统一焦点环

#### Scenario: 连续字母跳转

- **WHEN** 用户保持 `Cmd/Ctrl` 并连续按下不同字段对应键
- **THEN** 每次均成功跳转焦点，无需重新按下 `Cmd/Ctrl`

#### Scenario: 滚动后徽标更新

- **WHEN** 用户按住 `Cmd/Ctrl` 并滚动表单（滚轮或 `Cmd/Ctrl+↑/↓`）
- **THEN** 徽标与键位映射更新为当前视口内字段

#### Scenario: 系统复制粘贴不受阻

- **WHEN** 用户在抽屉内输入框聚焦时按下 `Cmd/Ctrl+C` 或 `Cmd/Ctrl+V` 或 `Cmd/Ctrl+A`
- **THEN** 执行浏览器默认复制/粘贴/全选，不触发字段跳转

#### Scenario: 属性过多时视口外无徽标

- **WHEN** 表单自定义属性数量超过单次可分配键位，且某字段位于视口外
- **THEN** 该字段不显示徽标；滚动至可见区域后参与分配

### Requirement: Tab 方向控制

系统 SHALL 提供全局 Tab 方向插件：

- 默认 `Tab`：正向切换焦点
- `↑` 按住 + `Tab`：反向切换焦点
- `↓` 按住 + `Tab`：显式正向切换

在对话框/抽屉内 MUST 仅在当前模态容器内切换可聚焦元素。命令面板打开时不介入。

#### Scenario: 反向 Tab

- **WHEN** 用户在缺陷抽屉内按住 `↑` 并按下 `Tab`
- **THEN** 焦点移至上一个可聚焦字段

### Requirement: 组合组件单 Tab 停靠点

以下组合组件 MUST 在 Tab 序列中仅占用一个停靠点，按一次 `Tab` 即可进入或离开该字段：

- `SelectProjectMember`：外框 `.select-project-member-input`
- `SelectModule`：外框 `.select-module-input`
- `el-switch`：内部 `.el-switch__input`（根节点不参与 Tab）
- `FileUpload` / `ImageUpload`：外框 `.cat2bug-upload-focus-target`

内部按钮、`.el-upload`、图片卡片等 MUST 设为 `tabindex="-1"`；上传区在 `Tab` 捕获阶段 MUST 一次跳出至相邻字段。

#### Scenario: 开关一次 Tab 离开

- **WHEN** 用户聚焦某开关后按一次 `Tab`
- **THEN** 焦点移至下一个表单字段，而非停留在开关内部第二停靠点

#### Scenario: 图片上传一次 Tab 离开

- **WHEN** 用户聚焦图片上传外框后按一次 `Tab`
- **THEN** 焦点移至下一个表单字段

### Requirement: 成员选择器下拉键盘

`SelectProjectMember` 下拉展开时 MUST 支持：

- `↑` / `↓`：移动当前选中成员
- `←` / `→`：切换角色 Tab
- `Enter` / 空格：选中当前成员
- `Esc`：关闭下拉并恢复外框焦点
- 外框获焦时直接输入字符：展开下拉并写入搜索词

空格选中成员时 MUST NOT 触发缺陷页「页面动作」面板。

#### Scenario: 空格选中成员

- **WHEN** 成员下拉已展开且用户按下空格
- **THEN** 选中当前高亮成员，不打开页面动作面板

### Requirement: 开关方向键

`el-switch` 获焦时：

- `←`：设为关
- `→`：设为开
- 空格：由原生 checkbox 切换

#### Scenario: 右箭头打开开关

- **WHEN** 用户聚焦开关后按下 `→`
- **THEN** 开关变为开启状态

### Requirement: 数字输入框焦点边框

`el-input-number` 聚焦边框 MUST 与普通 `el-input` 一致：暗色主题金色（`#FFC107` / 抽屉内 `#ffce3a`），浅色主题蓝色（`#1890ff`）。

#### Scenario: 抽屉内数字字段聚焦

- **WHEN** 用户在新建缺陷抽屉内 Tab 至数字自定义字段
- **THEN** 显示与其他输入框一致的聚焦边框颜色

### Requirement: 原生文件选择框键盘

打开系统文件选择框（图片/附件上传）时，实现 MUST 暂停页面内冲突的键盘与 focus 逻辑，使用户可在系统对话框内用方向键选择文件。

#### Scenario: 文件框内方向键选文件

- **WHEN** 用户在缺陷表单中点击上传并打开系统文件选择框
- **THEN** 方向键用于在系统文件列表中移动，不被 Excel 表格或上传区 focus 逻辑拦截

#### Scenario: 选文件期间 Esc 仍可用

- **WHEN** 用户打开系统文件选择框后按下 `Esc`（且无其它浮层）
- **THEN** 仍可关闭缺陷抽屉
