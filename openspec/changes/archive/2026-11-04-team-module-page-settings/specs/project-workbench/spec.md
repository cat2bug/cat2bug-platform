## ADDED Requirements

### Requirement: Workbench hides widgets for team-disabled modules

工作台画布与组件目录 MUST 尊重所属团队的模块页面设置：当某开关键关闭时，声明依赖该开关键（或其映射业务域）的组件 MUST 不渲染在画布上，且 MUST NOT 出现在可添加目录中（或等价不可添加）。全部业务模块关闭时，依赖这些模块的组件 MUST 均不显示。

#### Scenario: Defect widgets hidden when defect module off

- **WHEN** 团队关闭缺陷模块且用户打开项目工作台
- **THEN** 已放置的缺陷类组件不显示，目录中亦不可再添加缺陷类组件

#### Scenario: Case and plan widgets follow case_plan switch

- **WHEN** 团队关闭 `case_plan`
- **THEN** 用例类与计划类工作台组件均不显示
