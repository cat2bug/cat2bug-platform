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

#### Scenario: I 邀请成员

- **WHEN** 用户有邀请权限且按 I
- **THEN** 打开邀请成员对话框

### Requirement: 成员子弹框

`CreateTeamMember`、`InviteTeamMember` SHALL 复用工具弹框套系。

### Requirement: 团队基本信息子页

`team-base-info` 表单 MUST 表单键盘集成 + B 返回。
