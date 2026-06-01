## ADDED Requirements

### Requirement: 个人报时统计块注册

系统 SHALL 在缺陷页统计条提供名为 `PersonalRemindTimer` 的统计组件。

组件 MUST：

- 注册在 `PERSONAL_STATISTIC_NAMES`
- MUST NOT 加入 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`
- 外层高度遵守统计条 **115px** 约束（`Cat2BugCard`）
- 在统计模版页个人分组中可选添加

#### Scenario: 默认缺陷页不含报时块

- **WHEN** 用户使用未修改的默认统计模板打开缺陷页
- **THEN** 统计条不出现 `PersonalRemindTimer`

#### Scenario: 模版页添加后出现

- **WHEN** 用户在统计模版个人区添加「报时提醒」并保存
- **THEN** 缺陷页统计条出现该块

### Requirement: 多条报时规则与模版持久化

系统 SHALL 在单个 `PersonalRemindTimer` 块内支持多条报时规则，规则存储于该模版项的 `params.timers` 数组。

每条规则 MUST 包含：

- `id`：唯一标识
- `label`：展示名称（非空，长度上限实现时约束如 32）
- `enabled`：布尔，默认 true
- `sound`：音效标识，映射内置静态资源
- `schedule`：对象，含 `type` 为 `once` | `daily` | `weekly` | `monthly` 之一及类型所需字段

保存统计模版时 MUST 将 `params.timers` 一并持久化；重新打开缺陷页 MUST 恢复相同规则。

#### Scenario: 配置两条规则后刷新仍保留

- **WHEN** 用户在对话框新增两条报时并保存模版
- **THEN** 刷新缺陷页后仍显示两条且配置一致

### Requirement: 调度仅于缺陷统计条生效

报时调度 MUST 仅在 `PersonalRemindTimer` 组件已挂载且 `read !== true` 时运行。

组件销毁或用户离开缺陷页（组件 unmount）时 MUST 停止所有定时器且不再播放音效。

`read` 为 true 时（统计模版预览）MUST NOT 播放音效，且 MUST NOT 启动到点触发。

当浏览器页签 `document.hidden === true` 时 SHOULD 暂停调度；页签重新可见时 MUST 重新计算下次触发时间。

#### Scenario: 离开缺陷页不再报时

- **WHEN** 用户导航离开缺陷页导致组件销毁
- **THEN** 到达原设定时刻不再播放音效

#### Scenario: 模版预览不播报

- **WHEN** 用户在统计模版页预览含报时块的模板（read 模式）
- **THEN** 到点不播放音效

### Requirement: 周期与单次语义

| schedule.type | 行为 |
|---------------|------|
| `once` | 在指定 `date` + `time` 触发 **一次**；触发后 MUST 不再重复（禁用或标记已触发） |
| `daily` | 每天在 `time` 触发，循环 |
| `weekly` | 每 `weekday`（1–7）在 `time` 触发，循环 |
| `monthly` | 每月在 `day`（1–31）的 `time` 触发；若当月无该日，MUST 在该月 **最后一天** 触发 |

时间比较 MUST 使用用户浏览器本地时区。

#### Scenario: 单次报时仅响一次

- **WHEN** 规则类型为 `once` 且已过触发时刻
- **THEN** 次日同一时刻不再触发

#### Scenario: 每月 31 日在 2 月

- **WHEN** 规则为 `monthly`，`day=31`，当前月为 2 月
- **THEN** 触发时间为 2 月最后一天的规定时刻

### Requirement: 块内倒计时展示

块内 MUST 展示距下次触发的时间倒计时，按下次触发时间升序排列。

倒计时主数字（`HH:mm:ss` 或含天的数字段）MUST 使用电子管风格字体 **Nixie One**，CSS 类 `.statistic-countdown-nixie`，并在明暗主题下保持可读辉光效果。

当规则数大于 2 时，UI MUST 通过滚动或「+N 条」等方式在 115px 内保持可读，且最近一条 MUST 始终可见。

#### Scenario: 两条规则显示两个倒计时

- **WHEN** `timers` 含两条 enabled 规则且下次触发时间不同
- **THEN** 块内至少展示两条倒计时信息

### Requirement: 点击打开配置对话框

用户点击报时卡片内容区 MUST 打开配置对话框（`el-dialog`），用于增删改规则、启用/禁用、选择音效。

对话框 MUST 提供「试播」当前所选音效，试播由用户点击触发。

交互模式 SHOULD 与 `MyLife` 一致（`append-to-body`、`close-on-click-modal=false`）。

#### Scenario: 点击卡片打开对话框

- **WHEN** 用户点击报时卡片（非 read）
- **THEN** 配置对话框可见且可编辑规则

### Requirement: 内置音效

系统 MUST 提供至少 3 种内置短音效（静态资源，单文件建议小于 50KB）。

到点触发时 MUST 使用 HTML5 `Audio`（或等价）播放与规则 `sound` 对应的文件。

自动播放被浏览器拦截时 MUST NOT 向用户展示阻塞性错误弹窗。

#### Scenario: 到点播放音效

- **WHEN** 规则 enabled 且到达下次触发时刻，页签可见、组件 mounted
- **THEN** 播放对应音效一次

### Requirement: 国际化

报时块标题、对话框标签、调度类型、空状态、音效名称 MUST 支持项目要求的 7 种前端语言（zh_CN、zh_TW、en、ru、ja、ko、ar）。

#### Scenario: 切换语言后文案更新

- **WHEN** 用户切换界面语言
- **THEN** 报时块与对话框展示对应翻译
