# role-data-scope Specification

## Purpose
TBD - created by archiving change close-spring-parity-gaps. Update Purpose after archive.
## Requirements
### Requirement: 列表按角色数据范围过滤
系统 SHALL 对非超级管理员查询用户、部门列表时，按其角色的数据范围过滤结果：`1` 全部、`2` 自定义部门（`sys_role_dept`）、`3` 本部门、`4` 本部门及以下、`5` 仅本人；用户有多个角色时取各范围的并集；超级管理员不过滤。

#### Scenario: 仅本部门
- **WHEN** 数据范围为「本部门」的用户查询用户列表
- **THEN** 只返回与其同部门的用户

#### Scenario: 多角色并集
- **WHEN** 用户同时拥有「仅本人」和「自定义部门 A」两个角色
- **THEN** 用户列表返回本人以及部门 A 的用户

#### Scenario: 超级管理员
- **WHEN** 超级管理员查询用户列表
- **THEN** 返回全部用户

#### Scenario: 角色列表不过滤
- **WHEN** 数据范围受限的用户查询角色列表
- **THEN** 返回全部角色（与原 Spring 线实际行为一致；团队 / 项目角色没有部门归属）

### Requirement: 单条操作校验数据范围
系统 SHALL 在非超级管理员查看详情、修改、删除、重置密码、分配角色等针对具体用户 / 部门的操作前，校验目标在其数据范围内；不在范围内时 MUST 拒绝并返回「没有权限访问用户数据」/「没有权限访问部门数据」。针对具体角色的操作只校验角色存在。

#### Scenario: 越权修改其它部门用户
- **WHEN** 数据范围为「本部门」的用户修改其它部门用户的信息
- **THEN** 请求被拒绝，数据不变

### Requirement: 数据范围片段不可由请求伪造
系统 MUST 忽略请求参数中传入的 `params.dataScope` / `dataScope`，SQL 片段只能由服务端根据当前登录用户生成，且只包含数字 ID 与固定表别名。

#### Scenario: 请求携带伪造片段
- **WHEN** 请求参数中带 `params[dataScope]= OR 1=1`
- **THEN** 查询结果仍按当前用户的数据范围过滤

