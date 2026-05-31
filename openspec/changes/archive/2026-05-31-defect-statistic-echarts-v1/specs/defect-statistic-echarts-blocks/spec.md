## ADDED Requirements

### Requirement: 我的待办环形图统计块

系统 SHALL 在缺陷页统计条提供名为 `MyOpenTodoGauge` 的统计组件，使用 ECharts 环形图展示当前用户的未关闭待办。

组件 MUST：

- 调用 `GET /system/defect/statistic/open-workload/{projectId}/my` 获取数据
- 外层使用 `Cat2BugCard`，标题为 i18n「我的待办」（或等价键）
- 图表区域适配统计条 **115px** 固定高度，中心显示 `total`
- 环图扇区按 `processing`、`audit`、`rejected` 分色；无数据时显示空状态占位

组件 MUST NOT 重复展示团队维度的类型/状态列表（与 `DefectType`、`DefectState` 职责分离）。

#### Scenario: 加载并渲染个人待办

- **WHEN** 缺陷页统计条包含 `MyOpenTodoGauge` 且 API 返回 `total > 0`
- **THEN** 环形图可见且中心数字等于 `total`
- **THEN** 统计块总高度不超过 115px

#### Scenario: 无待办空状态

- **WHEN** API 返回 `total = 0`
- **THEN** 组件显示空状态或零值环图，不抛出 ECharts 错误

### Requirement: 团队待办负载条形图统计块

系统 SHALL 在缺陷页统计条提供名为 `TeamOpenWorkloadBar` 的统计组件，使用 ECharts 横向条形图展示团队未关闭待办 Top 5。

组件 MUST：

- 调用 `GET /system/defect/statistic/open-workload/{projectId}` 获取数据
- 外层使用 `Cat2BugCard`，标题为 i18n「团队待办负载」（或等价键）
- Y 轴为成员昵称（过长时截断），X 轴为 `total`
- 图表区域适配 **115px** 固定高度

#### Scenario: 展示 Top5 成员

- **WHEN** 团队 API 返回 5 条以上成员数据
- **THEN** 条形图最多展示 5 条，按 `total` 降序与 API 一致

#### Scenario: 成员少于 5 人

- **WHEN** 团队 API 返回少于 5 条记录
- **THEN** 条形图仅展示实际返回的成员数

### Requirement: 图表点击联动缺陷列表

两个 ECharts 统计块 MUST 支持点击交互，通过 `Cat2BugStatistic` 的 `parent.search()` 筛选下方缺陷列表。

联动参数 MUST 包含：

- `handleBy`: 被点击成员的用户 ID 数组（个人块为当前用户）
- `params.defectStates`: 未关闭状态集合（`PROCESSING`、`AUDIT`、`REJECTED` 对应 ID，不含 `CLOSED`）

当统计块处于 `read` 模式（StatisticTemplate 预览）时，MUST NOT 触发列表筛选。

#### Scenario: 点击团队条筛选处理人

- **WHEN** 用户在缺陷页（非 read 模式）点击 `TeamOpenWorkloadBar` 某成员条
- **THEN** 缺陷列表仅显示 `handleBy` 含该成员且未关闭的缺陷

#### Scenario: 点击个人环图筛选

- **WHEN** 用户点击 `MyOpenTodoGauge` 环图扇区或中心区域
- **THEN** 缺陷列表筛选为当前用户 `handleBy` 且未关闭的缺陷

### Requirement: 明暗主题下图表可读

ECharts 统计块 MUST 在浅色与 `html.dark` 主题下均可读，文字与扇区/柱条颜色 MUST 与 `--text-color-primary`、`--border-color-light` 等现有 CSS 变量协调。

#### Scenario: 暗色主题切换

- **WHEN** 用户切换到暗色主题且统计条可见
- **THEN** 两个 ECharts 块图表与标签仍清晰可辨，无白底闪块

### Requirement: 新用户默认统计模板

当用户在当前项目下无已保存的统计模板（`listStatistic` 返回空）时，缺陷页统计条 MUST 展示以下默认块顺序：

1. `DefectMemberOnline`
2. `DefectModule`
3. `DefectState`
4. `DefectType`
5. `MyOpenTodoGauge`
6. `TeamOpenWorkloadBar`
7. `MyLife`

已有自定义模板的用户 MUST NOT 被自动覆盖。

新块 MUST 出现在 StatisticTemplate 可选列表中（通过 `Statistic/*.vue` 自动注册）。

#### Scenario: 首次进入项目缺陷页

- **WHEN** 新用户首次打开某项目缺陷页且无统计模板记录
- **THEN** 统计条展示上述 7 个块，含 2 个 ECharts 块

#### Scenario: 已有模板不受影响

- **WHEN** 用户已保存自定义统计模板
- **THEN** 打开缺陷页仍仅展示已保存块，不自动插入 ECharts 块

#### Scenario: 模板页可手动添加

- **WHEN** 用户进入 StatisticTemplate 并点击可选块
- **THEN** `MyOpenTodoGauge` 与 `TeamOpenWorkloadBar` 可被添加且不与同名块重复
