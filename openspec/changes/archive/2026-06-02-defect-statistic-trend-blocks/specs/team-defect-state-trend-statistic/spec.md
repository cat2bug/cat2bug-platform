## ADDED Requirements

### Requirement: 团队缺陷状态走势统计块注册

系统 SHALL 在缺陷页统计条提供名为 `TeamDefectStateTrend` 的统计组件。

组件 MUST：

- 注册在 `TEAM_STATISTIC_NAMES`
- MUST NOT 加入 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`
- 外层使用 `Cat2BugCard`，高度遵守统计条 **115px** 约束
- 块最小宽度 **320px**（与 `TeamPlanBurndown` 一致）
- 在统计模版页团队分组中可选添加

#### Scenario: 默认模板不含状态走势块

- **WHEN** 用户使用未修改的默认统计模板打开缺陷页
- **THEN** 统计条不出现 `TeamDefectStateTrend`

#### Scenario: 模版页添加后出现

- **WHEN** 用户在统计模版团队区添加「缺陷状态走势」并保存
- **THEN** 缺陷页统计条出现该块

### Requirement: 缺陷状态走势数据 API（缺陷统计域）

系统 SHALL 提供 `GET /system/defect/statistic/defect-state-line/{projectId}?timeType=day|month`。

实现 MUST 委托 `SysDashboardService.defectLine`，数据口径与 `GET /system/dashboard/{projectId}/defect-line` 一致：按 `sys_defect.update_time` 的日历日或月分组，并按 `defect_state` 计数。

响应 MUST 包含：

- `times`：时间轴字符串数组
- `types`：状态枚举列表（与 `SysDefectStateEnum` 一致）
- `data`：以状态枚举 **name** 为键、各时间点计数数组为值的对象

接口 MUST 使用权限 `system:defect:query`。

#### Scenario: 30 天模式返回日序列

- **WHEN** 已授权用户请求 `timeType=day`
- **THEN** `times` 为 `yyyy-MM-dd` 格式日期序列
- **THEN** 每个状态键对应与 `times` 等长的非负整数数组

#### Scenario: 12 个月模式返回月序列

- **WHEN** 已授权用户请求 `timeType=month`
- **THEN** `times` 为 `yyyy-MM` 格式月份序列

### Requirement: 时间粒度切换与导出

组件 MUST 在标题工具区提供 **30 天** 与 **12 个月** 切换（`timeType` 为 `day` 或 `month`），切换后重新请求并渲染折线图。

组件 MUST 提供 Excel 导出，调用 `POST /system/defect/statistic/defect-state-line/{projectId}/export?timeType=...`，导出结构与仪表盘 `defect-line/export` 一致。

`read` 为 true 时（统计模版预览）MUST NOT 触发导出下载。

#### Scenario: 切换为 12 个月后图表更新

- **WHEN** 用户点击「12 个月」
- **THEN** 图表 x 轴变为月份且数据重新加载

#### Scenario: 预览模式不可导出

- **WHEN** 组件处于 `read` 模式
- **THEN** 导出操作不可用或不发起请求

### Requirement: 紧凑折线图展示

组件 MUST 使用 ECharts 折线图展示各状态系列；图表区域 MUST 适配 115px 卡片内容区（压缩 grid/legend，与 `TeamPlanBurndown` 同类布局）。

组件 MUST 在浅色与 `html.dark` 主题下可读，并随主题切换重绘。

#### Scenario: 暗色主题下图表可读

- **WHEN** 用户切换到暗色主题且块可见
- **THEN** 坐标轴与折线仍清晰可辨

### Requirement: 点击联动缺陷列表（状态 + 更新时间）

用户在缺陷页（`read !== true`）点击折线图数据点或图例对应系列时，组件 MUST 调用 `parent.searchQuery`，且：

- `stack` 为 `false`
- `common.params.defectStates` 为被点击状态对应的缺陷状态 ID（与 `DefectState` 块映射一致）
- `common.params.beginUpdateTime` 与 `common.params.endUpdateTime` 覆盖被点击时间点：
  - `timeType=day`：该日 00:00:00 至 23:59:59（本地日历日）
  - `timeType=month`：该月首日 00:00:00 至该月末 23:59:59
- `common.params.delFlag` 为 `'0'`

`read` 为 true 时 MUST NOT 调用 `searchQuery`。

#### Scenario: 点击某日处理中折线点

- **WHEN** 用户点击 `PROCESSING` 系列上日期为 `2026-05-31` 的数据点
- **THEN** 缺陷列表筛选为状态含「处理中」且 `update_time` 落在该日范围内

#### Scenario: 预览模式不联动

- **WHEN** 用户在统计模版预览区点击图表
- **THEN** 缺陷列表查询条件不变

### Requirement: 刷新

`TeamDefectStateTrend` MUST 实现 `refreshData()`，在 `Cat2BugStatistic.refreshData` 中被调用以按当前 `timeType` 重新加载。

#### Scenario: 统计条刷新更新走势

- **WHEN** 用户触发统计条 `refreshData`
- **THEN** 状态走势块重新请求 API

### Requirement: 国际化

标题、时间粒度按钮、导出相关文案 MUST 支持项目要求的 7 种前端语言（zh_CN、zh_TW、en、ru、ja、ko、ar）；状态系列名 MUST 使用现有缺陷状态 i18n 键（如 `PROCESSING`）。

#### Scenario: 英文界面标题为英文

- **WHEN** 界面语言为 en
- **THEN** 卡片标题与工具按钮为英文翻译
