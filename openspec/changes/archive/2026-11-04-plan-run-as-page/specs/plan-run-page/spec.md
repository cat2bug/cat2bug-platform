## ADDED Requirements

### Requirement: 测试计划执行为项目内整页

系统 MUST 在路径 `/{teamKey}/{projectKey}/plan/{planId}/run` 以整页展示测试计划执行工作台（计划标题与元信息、统计指标、用例/缺陷列表与模块树），MUST NOT 再以覆盖计划列表的右侧执行抽屉作为名称或「执行」入口的主展示方式。

#### Scenario: 有执行权限时点击计划名称进入整页

- **WHEN** 用户具备 `system:plan:run` 且在计划列表点击计划名称
- **THEN** 路由导航到该计划的执行整页，且页面展示该计划的执行工作台内容

#### Scenario: 点击「执行」进入整页

- **WHEN** 用户在计划列表操作列点击「执行」
- **THEN** 路由导航到该计划的执行整页

#### Scenario: 无执行权限不可点名称进执行页

- **WHEN** 用户不具备 `system:plan:run`
- **THEN** 计划名称不以可导航链接呈现，且无法通过列表操作进入执行整页

### Requirement: 从执行页返回计划列表

系统 MUST 在执行整页提供返回能力；返回后 MUST 回到当前项目的测试计划列表，且侧栏仍高亮测试计划模块。

#### Scenario: 用户返回列表

- **WHEN** 用户在执行整页触发返回（标题返回或等价操作）
- **THEN** 导航到 `/{teamKey}/{projectKey}/plan` 列表页

### Requirement: 深链直达执行页

系统 MUST 支持通过执行页 URL 直接打开指定计划的执行工作台；对历史「列表 + `planId` query」深链，MUST 兼容并导向执行整页。

#### Scenario: 打开执行页 URL

- **WHEN** 用户访问 `/{teamKey}/{projectKey}/plan/{planId}/run` 且具备执行权限
- **THEN** 系统展示该 `planId` 对应执行工作台

#### Scenario: 旧 query 深链兼容

- **WHEN** 用户访问计划列表且 URL 带 `planId` query、并具备执行权限
- **THEN** 系统重定向到对应 `plan/{planId}/run` 执行整页
