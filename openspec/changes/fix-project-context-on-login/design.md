## Context

当前项目上下文真相源是后端 `sys_user_config.current_project_id`（按用户），前端 Pinia `user.config` 来自 `GET /getInfo`。侧栏项目区显隐条件是「`currentProjectId` 非空 + `getRouters` 里 Project 子菜单非空」；团队角色 `team.default` 种子已挂工作台/缺陷等项目菜单，因此**只要 config 里还有项目 ID，团队成员就会看到项目模块**，不要求 `sys_user_project` 行。

已知缺口：

- `selectSysProjectByProjectId` LEFT JOIN `sys_user_project` 且不要求 `up.user_id is not null` → 非成员 `getProject` 成功。
- `SwitchCurrentProject` 在 `getProject` 失败时仍 `updateConfig({ currentProjectId })`。
- `PUT /system/user-config` 不校验目标项目成员。
- `getInfo` 不清洗无效 `currentProjectId`；移出成员不清 config。
- 缺陷列表等接口主要验功能权限 + 查询 `projectId`，缺成员闸。
- 登录成功后按 `redirect` 进 `/index` 等，**无**「无有效项目 → `/team/project-list`」编排。

不推翻既有 ADR；延续「项目业务数据以项目成员关系为边界」（与 MCP/Git 等模块一致）。系统超管（`SysUser.isAdmin`）是否豁免成员校验：与现有缺陷/项目读接口惯例对齐——若现网超管本就可管任意项目，本变更对超管保持豁免；普通用户与仅团队成员必须过成员闸。

## Goals / Non-Goals

**Goals:**

- 非项目成员不能把无效项目当成「当前项目」，也不能靠侧栏/URL 读到该项目工作台与缺陷数据（复现路径）。
- 无任何可访问项目时：`currentProjectId=0`，跳转 `/team/project-list`，侧栏不显示工作台/缺陷。
- 前后端一致：配置写入、GetInfo、切项目、命名空间进入、本变更覆盖的业务读接口共用同一「可访问项目」判定。

**Non-Goals:**

- 改 `team.default` 菜单种子（去掉项目菜单模板）。
- 全站每个模块一次性补成员校验（用例/计划/文档等可后续按同谓词扩展；本变更以配置闸 + 复现路径读闸为主）。
- 改甘特计划节点 ACL、邀请链接、仅项目成员无团队等已有能力语义。
- 用 localStorage 存 currentProjectId。

## Decisions

1. **单一服务端谓词 `requireProjectMembership(userId, projectId)`**  
   有 `sys_user_project` 行（且项目存在/未删）→ 通过；系统超管可豁免（与现网一致则保留）。用于：写 `currentProjectId`、GetInfo 清洗、按 ID「进入项目」读、缺陷列表/详情（及工作台落地必需接口）的成员闸。  
   **备选**：只改前端显隐 — 否决，可绕 API。

2. **GetInfo 主动清洗并持久化**  
   若 config 中 `currentProjectId>0` 且非成员 → 置 0 写回 DB，再返回。避免每次登录都带着脏 ID。  
   **备选**：仅响应里临时清零、不写库 — 否决，下次仍脏。

3. **`updateSysUserConfig` 拒绝非法项目**  
   `currentProjectId>0` 且非成员 → 抛业务异常 / 403，不写入。`currentProjectId=0/null` 允许（清空）。  
   前端 `SwitchCurrentProject`：**去掉** `getProject` 失败后仍 `updateConfig` 的 catch 兜底；失败则提示并保持原上下文。

4. **按 ID 取可进入项目**  
   业务「进入/切换」路径改为要求成员（`up.user_id is not null`，或先查成员再查项目）。若存在管理端「任意项目详情」需求，须用独立管理接口，不得与成员入口共用假成功。path key 解析后同样要求成员，否则 403。

5. **无项目着陆（前端）**  
   `GetInfo` + `GenerateRoutes` 之后：若 `currentProjectId` 为空/0（清洗后），且目标不是登录/安装等白名单，则 `replace` 到 `/team/project-list`（与 `TeamSelect` / 建团后一致）。侧栏已有 `projectId` 条件，清零后自然隐藏工作台/缺陷。  
   若用户仍有其他可访问项目但当前 ID 被清：可留在项目列表自行选择（本变更不强制自动切到「第一个项目」——减少惊喜切换）。

6. **移出成员时清 config**  
   `deleteMember`（及等价移除）后：若被移用户 `current_project_id == projectId`，更新为 0。不强制踢在线会话（下次 GetInfo/请求会清洗）；可选后续做会话权限重算。

7. **本变更业务读闸范围**  
   必做：缺陷列表/分页（复现「可查看」）、`GET /system/project/{id}` 成员语义、path 解析进项目。工作台若仅依赖成员项目上下文 + 组件内 projectId，在配置闸修好后多数卡片会空或失败；对明确的 dashboard 聚合接口若仍无成员校验则一并加上。用例/计划/文档等非复现主路径标为 follow-up，但谓词可复用。

## Risks / Trade-offs

- **超管豁免**：若误判超管范围，可能影响运维排障；须与 `SysUser.isAdmin` 现网用法一致。
- **收紧 getProject**：依赖「非成员 getProject 成功」的旧前端路径会失败——这是期望行为；需回归切项目、分享链接、命名空间守卫。
- **不自动选第一个项目**：无有效当前项目时只去列表，多一次点击；换得行为可预期。
- **未全量覆盖所有模块读接口**：残留越权窗口；tasks 中明确范围与后续债，避免假安全感。

## 相关长期文档

- `docs/03-技术方案/权限模型与鉴权链路.md`
- `docs/05-接口文档/接口清单-系统与监控.md`（user-config）
- `docs/05-接口文档/接口清单-项目团队成员.md`
- `docs/01-产品需求/术语表.md`（项目 / 用户配置）
