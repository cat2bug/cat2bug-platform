## Why

一期仓库「管理」侧栏已预留「仓库成员管理」灰显入口，但权限仍纯按项目角色判定，无法按仓微调（降权项目管理员、邀请非项目成员）。需落地仓级 ACL，并让 REST / Smart HTTP / 管理 Tab 共用同一套 effective 权限。

## What Changes

- 新增仓级成员 ACL 存储（用户 × 仓库 × 角色三档：`read` / `write` / `admin`）
- 创建仓库时自动写入创建者为仓 `admin`
- **BREAKING（行为）**：`GitRepositoryAccessService` 改为按仓库计算 effectivePerm——有 ACL 行则完全覆盖项目角色；无行则回落项目角色；非项目成员仅在被 ACL 邀请后可访问
- 可邀请系统内非项目成员进仓
- 谁可改 ACL：effective manage（项目 create-by/admin 无降权行，或仓 `admin`）；禁止移除最后一位仓 `admin`
- 启用 settings「仓库成员」页（`.../settings/members`），去掉灰显
- 管理 Tab / settings 深链守卫改查 effective manage

## Capabilities

### New Capabilities

- `git-repo-members`: 仓级 ACL 模型、成员 CRUD API、创建时默认 admin、settings 成员 UI

### Modified Capabilities

- `project-git-repository`: 仓库生命周期 / manage 判定改为 effectivePerm（含仓 ACL 覆盖）
- `git-smart-http`: pull / push / force-push 判定改用同一 effectivePerm
- `git-repo-settings`: 侧栏「仓库成员」从占位变为可用路由页（若主规格尚未同步一期 delta，本 change 一并写清成员页需求）

## Impact

- 后端：`GitRepositoryAccessService`、Smart HTTP、仓库 create/import、新 ACL Mapper/表（Flyway h2+mysql，版本线 1.1.0）
- 前端：`settings/members`、侧栏启用、壳 Tab / 守卫改查 effective manage（可能需 API 返回当前用户仓权限）
- 无第三方依赖变更
