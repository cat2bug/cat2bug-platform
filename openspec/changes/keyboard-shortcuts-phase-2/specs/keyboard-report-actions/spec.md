## ADDED Requirements

### Requirement: 报告列表动作面板

项目报告页 SHALL `registerPage('report', …)` + `page-action-hints`。

### Requirement: 报告列表默认字母

| 键 | 动作 |
|----|------|
| S | 聚焦标题查询 |
| E | 新建报告（触发模板选择创建） |
| D | 批量删除（已勾选） |
| B | 上一页 |
| P | 下一页 |

> **N** 在 `PAGE_ACTION_RESERVED` 内（⌘/Ctrl+N 新窗口），故新建报告使用 **E**，与其它列表页一致。

#### Scenario: E 新建报告

- **WHEN** 用户有 `system:report:add` 权限且按 E
- **THEN** 打开报告模板选择/创建流程

### Requirement: ViewReport

查看报告组件打开时 MUST 视为遮挡层；一级报告动作禁用。ViewReport 内 `Esc` MUST 关闭查看层。

#### Scenario: 查看报告时禁用列表动作

- **WHEN** ViewReport 可见
- **THEN** 空格不打开报告列表动作面板

### Requirement: 查询区左右键导航

报告列表查询条 MUST 支持 **← / →** 在「标题 → 时间范围 → 推送人」间切换。日期或成员下拉展开时 MUST NOT 切换查询项。

#### Scenario: 查询区内左右切换

- **WHEN** 用户聚焦报告标题且处于查询导航态，按 **→**
- **THEN** 焦点移至时间范围控件
