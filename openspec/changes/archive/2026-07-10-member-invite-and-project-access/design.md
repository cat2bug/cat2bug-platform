## Context

- **团队邀请**：`InviteTeamMember.vue` 通过 `GET /system/team/{teamId}/not-member` 从全局 `sys_user` 搜索，调用 `inviteMember()` 即时写入 `sys_user_team`。
- **项目添加**：`AddProjectMember.vue` 的 not-member 查询（`SysUserProjectMapper.xml`）**仅限同团队成员**，与 `sys_user_project` 可独立存成员的数据模型不一致。
- **邀请链接**：文档 `external.md` 有描述，代码中无 token/链接实现。
- **权限**：`SysMenuMapper.xml` 已 union 项目角色，项目权限可不依赖 `sys_user_team`。
- **约束**：双栈 Quarkus + Spring legacy；Flyway 三处同步；版本线当前为 1.0.1。

## Goals / Non-Goals

**Goals:**

- 团队/项目管理员可生成 **1 小时**、**多人共用**的邀请链接，并可撤销。
- 已注册用户通过 `/invite/{token}` 登录后接受邀请，获得对应团队或项目角色。
- 项目邀请**不**写入 `sys_user_team`。
- 项目添加成员候选池扩展为全局用户；仅项目成员获得方案 A 导航（团队名只读、无团队管理入口）。
- API 与 UI 在 Quarkus 主路径与 Spring legacy 行为一致。

**Non-Goals:**

- 邀请链接拉新用户自助注册（主路径为老用户登录后加入）。
- 可配置有效期、单次使用链接、邮件/钉钉自动发送邀请。
- 重构团队/项目权限模型或 RBAC 表结构。
- 批量导入成员、审批流、邀请记录审计报表。

## Decisions

### 1. 单表 `sys_member_invite` 承载团队与项目链接

| 字段（概念） | 说明 |
|---|---|
| `invite_id` | 主键 |
| `token` | URL-safe 随机串（≥32 字节熵，如 UUID 去横线 + 额外随机） |
| `scope` | `TEAM` \| `PROJECT` |
| `team_id` | 团队链接必填；项目链接填所属团队（上下文，非成员关系） |
| `project_id` | 项目链接必填 |
| `role_ids` | JSON 数组，团队或项目角色 ID |
| `expires_at` | `created_at + 1h`，生成时写死 |
| `revoked` | 布尔，撤销后不可接受 |
| `created_by` | 创建人 user_id |

**理由**：一类资源、一套 API；`scope` 区分接受逻辑即可。  
**备选**：分 `sys_team_invite` / `sys_project_invite` 两表 — 重复字段多，弃用。

### 2. 有效期固定 1 小时，API/UI 不提供 `expiresIn` 参数

服务端 `expires_at = now() + 3600s`；列表展示剩余时间。  
**理由**：产品决策，降低滥用窗口与配置复杂度。

### 3. 多人共用：不因接受而失效

同一 token 在 `expires_at` 前且未 `revoked` 时可被多名用户依次接受；已成员用户再次打开提示「已是成员」。  
**理由**：协作场景一次发链多人加入。  
**备选**：单次 token — 与决策不符。

### 4. 接受流程（幂等）

```
GET  /invite/{token}/preview  → 公开，返回 scope、团队/项目名、是否过期（不含敏感角色明细可选）
POST /invite/{token}/accept   → 需登录
```

接受时事务内：

- **TEAM**：若不在 `sys_user_team` 则插入 + 写入 `sys_user_team_role`；已在则仅补缺失角色（可选，默认跳过重复）。
- **PROJECT**：若不在 `sys_user_project` 则插入 + `sys_user_project_role`；**不**写 `sys_user_team`。

前端 `/invite/:token`：未登录 → 存 `redirect` → 登录页 → 回跳后自动 `accept`。

### 5. 管理 API（需权限）

| 方法 | 路径 | 权限 |
|---|---|---|
| POST | `/system/team/{id}/invite-link` | 团队成员管理 |
| GET | `/system/team/{id}/invite-links` | 同上 |
| DELETE | `/system/team/{id}/invite-link/{inviteId}` | 撤销 |
| POST | `/system/project/{id}/invite-link` | 项目成员管理 |
| GET/DELETE | 项目对称 | 同上 |

请求体含 `roleIds`；响应含完整 URL（`window.location.origin + '/invite/' + token`）。

### 6. 项目 not-member 查询改为全局

修改 `selectNotSysUserListByProjectId`：从 `sys_user` 排除已在 `sys_user_project` 的用户，**移除** `sys_user_team` 联结限制。  
团队 not-member 保持现状（全局用户减已入团）。

### 7. 仅项目成员导航 — 方案 A

- 后端：`GET /system/user/context` 或扩展现有用户信息，返回 `teamMember: boolean`（当前团队是否在 `sys_user_team`）。
- 前端：
  - `TeamSelect`：非团队成员时显示项目所属 `teamName`，禁用切换下拉。
  - 路由/菜单：过滤 `teamRouters`、`teamOptionRouters`（团队设置、成员管理等）。
  - 直链团队 URL：后端 `@RequiresPermissions` 或团队 membership 校验返回 403。
- `currentTeamId` 仍用项目 `team_id` 作上下文（缺陷列表等需要 team 维度时）。

### 8. 实现分期

- **阶段 1**：表 + 邀请 API + `/invite` 页 + 团队/项目管理 UI 链接触发。
- **阶段 2**：not-member SQL + `teamMember` 导航 + 后端拦截。

**理由**：邀请链接可独立交付价值；导航改动依赖「项目成员可无团队」数据路径稳定。

## Risks / Trade-offs

| 风险 | 缓解 |
|---|---|
| Token 泄露导致未授权加入 | 1h 短有效期 + 撤销；接受需登录；仅授予链接上预设角色 |
| 链接被爬虫预览 | preview 接口不返回 token 以外敏感信息；accept 必须认证 |
| 仅项目成员误点团队设置 | 前后端双重拦截 |
| 双栈实现漂移 | 共享 `cat2bug-platform-system` 服务层与 mapper；成对改 Quarkus Resource |
| 用户无团队却有多项目 | 项目列表仍按 `sys_user_project`；TeamSelect 显示当前项目所属团队名 |

## Migration Plan

1. Flyway：`V1_0_1_1__member_invite_link.sql`（h2 / mysql / quarkus-admin 三处）。
2. 部署后旧行为不变；新链接功能随 UI 发布启用。
3. 回滚：停发新链、撤销活跃链；表可保留（无破坏性 DDL 对旧数据）。
4. 阶段 2 SQL 变更仅放宽候选池，不删除数据。

## Open Questions

- 接受邀请时若用户已是成员但角色不同：默认**不降级不覆盖**，仅提示已是成员（实现简单）。是否在后续支持「链接角色覆盖」？
- 同一团队/项目是否允许多条未撤销链接并存：建议**允许**（不同角色组合），列表展示全部活跃链。
