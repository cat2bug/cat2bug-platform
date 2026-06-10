## ADDED Requirements

### Requirement: 个人中心页动作

个人中心（`/member/profile`）SHALL `registerPage('profile', …)` + `page-action-hints`。

### Requirement: 个人中心默认字母

| 键 | 动作 |
|----|------|
| J | 切换 Tab（基本信息 / 修改密码） |
| B | 返回上一页 |

#### Scenario: J 切换 Tab

- **WHEN** 用户在基本信息 Tab 按 J
- **THEN** 切换到修改密码 Tab

#### Scenario: 激活 Tab 右下角徽标

- **WHEN** 用户按住 ⌘/Ctrl 且位于基本信息或修改密码 Tab
- **THEN** 当前激活 Tab 标签右下角显示 **J** 徽标；再按 **J** 切换到另一 Tab

#### Scenario: J 不与字段冲突

- **WHEN** 子表单（userInfo / resetPwd）激活且用户按住 ⌘/Ctrl
- **THEN** **J** 不分配给表单字段，保留给 Tab 切换

### Requirement: 基本信息表单

`userInfo.vue` MUST 启用 `form-field-hints`；`Cmd/Ctrl+Enter` 触发保存。

### Requirement: 修改密码表单

`resetPwd.vue` MUST 同上。

#### Scenario: 改密 Cmd+Enter

- **WHEN** 用户在修改密码 Tab 按 `Cmd/Ctrl+Enter`
- **THEN** 提交密码修改

### Requirement: 与键盘设置页关系

个人中心页动作 MUST 在键盘设置页「个人中心」分组可配置；`/member/keyboard` 路由本身不注册 profile 动作（避免递归）。

#### Scenario: 键盘设置页无 profile 动作冲突

- **WHEN** 用户在键盘设置页
- **THEN** 不触发 profile 的 J/B 动作
