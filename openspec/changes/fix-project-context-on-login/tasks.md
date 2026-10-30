## 1. 后端：成员谓词与配置闸

- [x] 1.1 新增可复用的项目成员判定（含系统超管豁免约定），供 config / getInfo / 项目读 / 缺陷读共用
- [x] 1.2 `SysUserConfigService.updateSysUserConfig`：非零 `currentProjectId` 非成员则拒绝；允许清零
- [x] 1.3 `getInfo`（或装配 config 处）：无效 `currentProjectId` 持久化为 0 后再返回
- [x] 1.4 移出项目成员时：若被移用户 `current_project_id` 等于该项目则清 0

## 2. 后端：项目进入与缺陷读闸

- [x] 2.1 修正按 projectId「可进入」语义（成员 INNER/过滤或先验成员）；path key 解析进项目同样 403 非成员
- [x] 2.2 缺陷列表/详情（复现路径）增加项目成员校验；确认工作台落地必需接口不向非成员泄数

## 3. 前端：切项目、着陆与侧栏

- [x] 3.1 `SwitchCurrentProject` 去掉 `getProject` 失败仍 `updateConfig` 的兜底；失败提示且不写脏 ID
- [x] 3.2 登录 / `GetInfo`+`GenerateRoutes` 后：无有效 `currentProjectId` 则 `replace` 到 `/team/project-list`（白名单页除外）
- [x] 3.3 确认侧栏在 `currentProjectId` 为空/0 时不显示工作台/缺陷；命名空间守卫对非成员 403 离开上下文

## 4. 回归与文档

- [x] 4.1 手工回归：账号1 进项目 A 登出 → 账号2（非 A 成员）登录不可见/不可读 A；无项目账号进项目列表且无工作台/缺陷菜单；正常成员切项目不受影响
- [x] 4.2 回写长期知识：`docs/03-技术方案/权限模型与鉴权链路.md`、必要时 `docs/05` 用户配置/项目一句；`docs/08-版本记录/2026-10.md` 一行；勾选本条
