## ADDED Requirements

### Requirement: Defect page settings include module visibility and name

团队缺陷页面设置 UI 与持久化路径 MUST 允许配置缺陷功能模块的「显示模块」开关与「模块名称」（写入团队模块页面设置中的 `defect` 开关键）。关闭显示后 MUST 按团队模块硬闸门禁用缺陷能力；名称非空时 MUST 覆盖缺陷相关给人看的文案。现有视图开关、色阶与分享配置 MUST 继续有效，且与模块开关独立存储或独立字段组。

#### Scenario: Disable defect from page settings

- **WHEN** 团队管理员在缺陷页面设置中关闭显示模块并保存
- **THEN** 该团队下项目不再展示缺陷业务入口，缺陷业务 API 被拒绝，且视图/色阶/分享配置仍保留可再次编辑

#### Scenario: Rename defect label

- **WHEN** 管理员将缺陷模块名称设为「问题单」并保存
- **THEN** 侧栏与缺陷相关面向人文案显示「问题单」
