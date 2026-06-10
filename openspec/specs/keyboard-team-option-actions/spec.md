## ADDED Requirements

### Requirement: 团队设置枢纽页

团队设置索引页 SHALL `registerPage('team-option', …)`，卡片 1–9 规则同 `keyboard-project-option-actions`。

### Requirement: 团队成员管理页

`team/option/member/index.vue` SHALL `registerPage('team-member', …)`：

| 键 | 动作 |
|----|------|
| S | 聚焦成员搜索 |
| E | 创建成员 |
| I | 邀请成员 |
| B | 返回 |
| U | 上一页（有分页时；与返回 B 错开） |
| P | 下一页（有分页时） |

列表页 MUST 接入 `list-query-keyboard-nav`：**S** 进入查询导航后 **← / →** 在搜索框与工具栏按钮间切换。

#### Scenario: I 邀请成员

- **WHEN** 用户有邀请权限且按 I
- **THEN** 打开邀请成员对话框

### Requirement: 成员子弹框

`CreateTeamMember`、`InviteTeamMember` SHALL 复用工具弹框套系：**Esc** 关闭（未保存确认）；**Cmd/Ctrl+Enter** 提交；按住 **Cmd/Ctrl** 显示字段字母徽标。弹框打开时成员列表页 **Esc** MUST 关闭弹框而非返回上级。

#### Scenario: 邀请成员弹框 Esc 关闭

- **WHEN** 用户在邀请成员弹框内按 **Esc** 且未修改表单
- **THEN** 关闭弹框并回到成员列表

### Requirement: 团队基本信息子页

`team-base-info` 表单 MUST 表单键盘集成 + **B** 返回；**Esc** MUST 与 **B** 相同（未保存时确认后返回团队设置枢纽）；**Cmd/Ctrl+Enter** 保存；按住 **Cmd/Ctrl** 显示字段字母徽标。

#### Scenario: Esc 返回团队设置

- **WHEN** 用户在团队基本信息页按 **Esc** 且未修改表单
- **THEN** 返回团队设置枢纽页
