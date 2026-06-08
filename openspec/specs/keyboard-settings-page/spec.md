## ADDED Requirements

### Requirement: 键盘设置入口与页面

系统 SHALL 在顶部「个人中心」头像下拉中新增「键盘设置」入口，点击进入键盘设置页。该页 MUST 分组展示全部快捷键（导航 / 缺陷页 / 统计模版页 / 系统文档页 / 通知 / 下拉 / Phase 2 业务页），并支持启用开关、引导键查看、单项编辑、恢复默认、不可绑定键说明。

#### Scenario: Phase 2 分组可见

- **WHEN** 用户打开键盘设置页
- **THEN** 可见测试用例、测试计划、交付物、项目报告、文档管理、项目设置、团队设置、个人中心动作分组

#### Scenario: 从个人中心进入设置页

- **WHEN** 用户点击头像下拉中的「键盘设置」
- **THEN** 打开键盘设置页并加载当前快捷键配置

### Requirement: 自定义快捷键与持久化

用户 MUST 能修改任一快捷键的触发字母，配置 MUST 持久化到 `localStorage`（键 `cat2bug.shortcuts.v1`）。系统 MUST 仅持久化用户改动的差异项，未改动项沿用代码默认。

#### Scenario: 修改并持久化

- **WHEN** 用户将「缺陷」导航键由 `D` 改为其它未占用字母并保存
- **THEN** 配置写入 `localStorage`
- **WHEN** 刷新页面
- **THEN** 新的快捷键生效

### Requirement: 冲突检测

在同一面板作用域内，编辑快捷键时若与已有项重复，系统 MUST 即时提示冲突并阻止保存该冲突项。若用户输入字母属于 `SETTINGS_NEVER_BIND`（见 `keyboard-reserved-keys`），系统 MUST 同样拒绝保存，并 MUST 提示该键为浏览器或系统保留键。

#### Scenario: 重复字母被拒绝

- **WHEN** 用户将某导航项字母改为同面板内已被占用的字母
- **THEN** 界面即时标红提示冲突且不予保存

#### Scenario: 保留字母被拒绝

- **WHEN** 用户将测试用例「查询」键改为 `T`
- **THEN** 界面提示不可绑定（浏览器新标签页保留），且不予保存

### Requirement: 恢复默认

设置页 MUST 提供单项与全局「恢复默认」。执行全局恢复 MUST 清空用户覆盖，使所有快捷键回到代码默认。

#### Scenario: 全局恢复默认

- **WHEN** 用户点击「恢复默认」
- **THEN** 清空 `overrides`，所有快捷键回到默认字母

### Requirement: 国际化

键盘设置页与命令面板的所有文案 MUST 走 i18n，覆盖项目支持的 7 种语言（zh_CN、zh_TW、en、ru、ja、ko、ar）。

#### Scenario: 切换语言后文案更新

- **WHEN** 用户切换界面语言
- **THEN** 键盘设置页与命令面板文案随之更新
