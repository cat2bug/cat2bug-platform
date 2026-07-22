# 仓库成员邀请（链接 / 直接添加 / 从仓库邀请）设计

日期：2026-07-22  
分支：`core`  
状态：待实现

## 背景与目标

仓库 settings「成员」页目前仅支持搜索用户直接添加 ACL。产品需要对齐常见托管平台的邀请体验，支持：

1. **链接邀请**（复制链接 + 二维码，固定 3 天有效）
2. **直接添加**（沿用现有）
3. **通过仓库邀请成员**（从同项目其他仓批量导入）

本期不做管理员审核；不做 AI 队友。

## 已确认决策

| 项 | 决策 |
|----|------|
| 范围 | 三种方式都做；无审核 |
| 未登录 | 先登录/注册，再回跳接受邀请 |
| 链接有效期 | 仓邀请固定 **3 天**；团队/项目邀请保持现有 **1 小时** |
| 从仓邀请源 | 仅当前项目下、操作者有 **effective manage** 的其他仓库 |
| 可选角色 | `read` / `write` / `admin`（三种入口一致） |
| 表模型 | **扩展**现有 `sys_member_invite`，不新建并行邀请表 |
| 团队/项目 | 本期不新做 UI；继续用现有 invite；表结构支持 `REPO` 以便统一 |

## 现状（须复用）

- 表：`sys_member_invite`（`TEAM` / `PROJECT`，`role_ids` JSON，`revoked`，`expires_at`）
- API / 服务：`ISysMemberInviteService`、`SysMemberInviteController`
- 落地页：`/invite/:token`（`views/invite/index.vue`）+ `preview` / `accept`
- 仓 ACL：`sys_git_repository_member` + members CRUD + `my-access`
- UI 组件参考：`MemberInviteLinkPanel.vue`（团队/项目已有链接面板）

## 数据模型

扩展 `sys_member_invite`（Flyway h2 + mysql，版本线读根 `pom.xml` 的 `cat2bug.version`）：

- 新增可空列 `repository_id BIGINT`
- 索引：`idx_sys_member_invite_repository (repository_id)`
- `scope` 增加常量 `REPO`
- `team_id` / `project_id`：创建仓邀请时从仓库所属项目回填（与 PROJECT 邀请一致，便于上下文展示）；`repository_id` 必填
- **仓角色**：`role_ids` 存 JSON 字符串数组，元素为 ACL 角色字面量，例如 `["write"]`（与团队/项目存数字角色 ID 的约定按 `scope` 区分解析）

重生链接：旧记录 `revoked=1`，再插入新 token。

## 权限

| 操作 | 条件 |
|------|------|
| 创建/列表/吊销仓邀请链接 | 目标仓 effective manage |
| 直接添加、从仓批量添加 | 目标仓 effective manage |
| 列出「可作源」的仓库 | 同 `project_id`、非本仓、调用方对该源仓 effective manage |
| preview | 可匿名（与现有一致） |
| accept | 已登录；token 有效且未过期未吊销 |

接受仓邀请 **只** 写入 `sys_git_repository_member`，不自动加入项目/团队（与现有「可邀请非项目成员进仓」一致）。

## 用户流程

### 设置页

路径：`.../code/:slug/settings/members`

- 成员列表保留
- 「添加成员」打开对话框（或同页面板），三个 Tab：
  1. **链接邀请**：选角色 → 展示/创建当前有效链接 → URL、复制、二维码、剩余有效说明；「重新生成」= revoke + create
  2. **直接添加**：现有搜索用户 + 角色 + POST members
  3. **通过仓库邀请**：选源仓 → 多选成员 → 统一角色 → 批量写入本仓 ACL

### 接受页

复用 `/invite/:token`：

- 未登录：保存回跳后去登录/注册
- preview 增加仓库名（`scope=REPO`）
- 成功后优先跳转该仓代码页；已是成员走 already-member

## API

### 仓管理（新建，挂在 git repo 或 member-invite 下，实现时与现有风格对齐）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/system/git/repo/{repositoryId}/invites` | 当前有效 REPO 邀请（通常 0～1 条） |
| POST | `/system/git/repo/{repositoryId}/invites` | body: `{ role }` → 创建，expires = now+3d |
| POST | `/system/git/repo/{repositoryId}/invites/{inviteId}/revoke` | 吊销 |
| GET | `/system/git/repo/{repositoryId}/invite-sources` | 可选源仓列表 |
| GET | `/system/git/repo/{repositoryId}/invite-sources/{sourceId}/members` | 源仓成员（供勾选） |
| POST | `/system/git/repo/{repositoryId}/members/from-repo` | body: `{ sourceRepositoryId, userIds, role }` |

直接添加：现有 `POST .../members`。

### 扩展现有 member-invite

- `preview` / `accept`：识别 `REPO`，accept 调成员服务插入 ACL
- 错误：过期、已撤销、无效 token、无权限、源仓非法、重复成员（单人提示 / 批量跳过并汇总）

## 前端要点

- 成员页 Tab 化邀请 UI；二维码可用现有依赖或轻量库生成（与团队邀请面板一致则优先复用）
- i18n：zh-CN / en-US
- 落地页扩展仓库展示与跳转
- 管理 Tab / settings 守卫仍依赖 effective manage（已有 `my-access`）

## 测试

- 单元：REPO 邀请创建（3 天）、吊销后不可 accept、accept 写 ACL、已是成员、过期
- 批量 from-repo：跨项目拒绝、无源仓 manage 拒绝、重复跳过
- 前端：未登录回跳；三种 Tab 主路径手工验收

## 非目标（本期）

- 邀请审核 / 强制审核
- 可配置有效期
- 团队/项目邀请 UI 改造（仅仓侧接入同一表）
- 协作者数量上限
- AI 队友

## 风险与注意

- `role_ids` 在 TEAM/PROJECT 与 REPO 语义不同，解析必须按 `scope` 分支，避免把 ACL 字符串当 Long 角色 ID
- 仓邀请 3 天 vs 团队/项目 1 小时：文档与 UI 文案写清楚，避免混用常量
- 后端重启后才有新 Mapping；本地验证前需完整重启 Spring Boot
