## ADDED Requirements

### Requirement: Members settings supports invite-by-link and invite-from-repo

仓库成员 settings 页在保留直接添加与成员列表的同时，SHALL 支持通过邀请链接与从其他仓库批量添加成员。相关写操作 MUST 要求目标仓 effective manage。从其他仓库导入时 MUST 遵守同项目与源仓 manage 约束。

#### Scenario: Direct add still works

- **WHEN** 管理员在成员页直接搜索用户并指定角色提交
- **THEN** 系统按既有 members API 写入 ACL

#### Scenario: From-repo API available to managers

- **WHEN** 具备 effective manage 的用户调用从仓导入成员接口并提供合法参数
- **THEN** 系统批量写入 ACL 并跳过已存在成员
