## MODIFIED Requirements

### Requirement: Defect settings card links to page settings

团队设置中的缺陷属性卡片 MUST 在现有缺陷字段管理入口之外，提供「页面设置」导航，进入团队缺陷页面设置 UI。页面设置入口 MUST 挂在缺陷设置卡片内，MUST NOT 以名为「页面设置」的独立顶层枢纽卡片出现。仅具备团队缺陷字段列表/管理权限的用户 SHALL 看到并进入该入口。枢纽页 MUST 在信息、用例与计划、缺陷、成员之外，允许存在源码 / 交付物 / 文档等业务设置卡片（由团队模块页面设置能力定义）。

#### Scenario: Team admin opens page settings from defect card

- **WHEN** 团队管理员打开团队设置并进入缺陷属性卡片
- **THEN** 可以看到并打开页面设置，且不存在名为「页面设置」的独立顶层卡片

#### Scenario: Hub may include other module setting cards

- **WHEN** 用户打开团队设置枢纽页
- **THEN** 可以同时看到缺陷设置卡与其它业务模块设置卡（如源码、交付物、文档），且「页面设置」仍不是独立顶层卡片标题

## ADDED Requirements

### Requirement: Case settings card covers plan and page settings

团队设置中的用例相关卡片 MUST 呈现为「用例与计划设置」（或等价文案），MUST 保留用例类型与评审模板管理入口，并 MUST 提供「页面设置」导航以配置用例与计划的显示开关与两个显示名。

#### Scenario: Open case-plan page settings

- **WHEN** 团队管理员打开用例与计划设置卡
- **THEN** 可以进入页面设置，且仍可导航到用例类型与评审模板
