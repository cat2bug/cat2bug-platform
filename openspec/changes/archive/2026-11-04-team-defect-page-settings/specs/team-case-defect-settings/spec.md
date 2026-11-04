## ADDED Requirements

### Requirement: Defect settings card links to page settings

团队设置中的缺陷属性卡片 MUST 在现有缺陷字段管理入口之外，提供「页面设置」导航，进入团队缺陷页面设置 UI。MUST NOT 为此新增独立的团队设置枢纽第五卡片。仅具备团队缺陷字段列表/管理权限的用户 SHALL 看到并进入该入口。

#### Scenario: Team admin opens page settings from defect card

- **WHEN** 团队管理员打开团队设置并进入缺陷属性卡片
- **THEN** 可以看到并打开页面设置，而枢纽页仍只有原有卡片分区（信息 / 用例 / 缺陷 / 成员）

#### Scenario: No fifth hub card

- **WHEN** 用户打开团队设置枢纽页
- **THEN** 不出现名为「页面设置」的独立顶层卡片
