# team-defect-page-settings Specification

## Purpose

团队缺陷页面设置：视图开关、存活/驳回色阶、分享开关，以及缺陷功能模块显示与名称。
## Requirements
### Requirement: Team stores defect page settings

系统 MUST 按团队持久化缺陷页面设置，至少包含：启用的缺陷内容视图集合、存活时间色阶阈值与颜色、驳回次数色阶阈值与颜色、是否允许分享缺陷。缺省 MUST 为四视图全开、存活阈值 3 天 / 7 天（绿 / 橙 / 红）、驳回阈值 3 / 7（绿 / 橙 / 红）、允许分享。阈值 MUST 可被团队管理员修改；保存时 MUST 校验绿档上限严格小于橙档上限，且至少启用一个视图。

#### Scenario: Default config for existing team

- **WHEN** 团队尚未保存过页面设置
- **THEN** 读接口返回与上述缺省等价的配置

#### Scenario: Reject inverted thresholds

- **WHEN** 管理员提交存活时间绿档上限天数大于或等于橙档上限
- **THEN** 保存失败且配置不变

### Requirement: Team and project APIs expose page settings

系统 MUST 提供团队写路径 `PUT /system/team/{teamId}/defect-page-settings`（及对应 GET），调用方 MUST 具备团队缺陷字段管理权限且可管理该 `teamId`。系统 MUST 提供项目读路径，使项目成员能取得所属团队的页面设置（无需打开团队设置页）。仅项目挂职、无团队设置菜单的用户 MUST 仍受该配置约束。

#### Scenario: Team admin updates settings

- **WHEN** 团队管理员保存关闭看板并调整色阶阈值
- **THEN** 该团队下任意项目随后读取到相同配置

#### Scenario: Project member reads without team menu

- **WHEN** 仅项目挂职用户打开缺陷列表
- **THEN** 其视图开关与色阶/分享行为仍遵循所属团队配置

### Requirement: Enabled views filter defect content switcher

缺陷页内容视图切换（表格 / Excel / 甘特 / 看板）MUST 仅展示团队启用的模式。快捷键循环 MUST 只遍历启用项。若本地缓存的视图已被禁用，系统 MUST 降级到仍启用列表中的第一项（优先表格）并更新本地缓存。

#### Scenario: Kanban disabled hides button

- **WHEN** 团队关闭看板且用户打开缺陷页
- **THEN** 工具条不出现看板选项，且无法通过缓存停留在看板组件

#### Scenario: Cached disabled view falls back

- **WHEN** 本机曾缓存 `DefectKanban` 但团队已关闭看板且表格仍启用
- **THEN** 页面以表格视图展示并纠正缓存

### Requirement: Metric number colors follow team thresholds

表格名称列下与详情固定统计行中，存活时间与驳回次数的**数字** MUST 按团队色阶上色；标签文案 MUST NOT 使用该色阶。存活用「天」与绿/橙上限比较：小于绿上限为绿，小于橙上限为橙，否则为红；驳回用次数做同样三段比较。

#### Scenario: Old open defect turns red

- **WHEN** 存活时长 ≥ 橙档上限天数（默认 7 天）且使用默认色
- **THEN** 存活时间数字显示为红色

#### Scenario: Label stays uncolored

- **WHEN** 用户查看带色阶的存活时间
- **THEN** 「存活时间」标签文字颜色不随色阶变化，仅数字变色

### Requirement: Disallowing share blocks create and anonymous open

当团队 `allowShare` 为 false 时，系统 MUST 隐藏缺陷工具条分享入口，MUST 拒绝创建分享（POST），且 MUST 拒绝匿名访问已有分享链接（GET），响应表明分享已关闭。系统 MUST NOT 因关闭分享而强制删除历史分享记录行。

#### Scenario: Create share rejected when disabled

- **WHEN** 团队关闭分享且用户尝试创建缺陷分享
- **THEN** 请求失败且不新建有效分享

#### Scenario: Old share link blocked

- **WHEN** 团队关闭分享后访问既有匿名分享 URL
- **THEN** 无法查看缺陷内容，并提示分享已关闭

### Requirement: Defect page settings include module visibility and name

团队缺陷页面设置 UI 与持久化路径 MUST 允许配置缺陷功能模块的「显示模块」开关与「模块名称」（写入团队模块页面设置中的 `defect` 开关键）。关闭显示后 MUST 按团队模块硬闸门禁用缺陷能力；名称非空时 MUST 覆盖缺陷相关给人看的文案。现有视图开关、色阶与分享配置 MUST 继续有效，且与模块开关独立存储或独立字段组。

#### Scenario: Disable defect from page settings

- **WHEN** 团队管理员在缺陷页面设置中关闭显示模块并保存
- **THEN** 该团队下项目不再展示缺陷业务入口，缺陷业务 API 被拒绝，且视图/色阶/分享配置仍保留可再次编辑

#### Scenario: Rename defect label

- **WHEN** 管理员将缺陷模块名称设为「问题单」并保存
- **THEN** 侧栏与缺陷相关面向人文案显示「问题单」

