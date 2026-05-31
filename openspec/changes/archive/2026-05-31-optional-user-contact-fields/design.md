## Context

Cat2Bug 用户账号以 `userName` + 密码登录；`sys_user.phonenumber` 与 `email` 在库中已为可空字段（MySQL 对 `phonenumber` 有 UNIQUE 约束，允许多个 `NULL`）。当前前端在注册、个人中心、团队成员创建等入口对手机/邮箱做 `required` 校验；后端 `SysRegisterService` 对手机做非空硬校验。个人资料更新与系统用户 API 已在有值时才做唯一性校验，但团队创建成员 `SysTeamServiceImpl.insertSysUser` 对手机无条件调用 `checkPhoneUnique`。通知模块（IM）在首次创建用户通知配置时，会用个人资料中的 email/phone 作为邮件、企微、飞书的默认收件，但各平台仍可单独覆盖。

## Goals / Non-Goals

**Goals:**

- 注册、个人中心、团队成员创建/编辑中，手机与邮箱均为选填
- 有值时保持格式校验与全局唯一性校验
- 空值入库为 `NULL`（非空字符串），避免 UNIQUE 冲突
- 个人中心信息卡空联系方式显示「未设置」
- 通知设置中，个人资料缺手机或邮箱时给出明确提示

**Non-Goals:**

- 不新增手机/邮箱登录、找回密码、短信验证码
- 不修改系统管理员用户管理页（已为选填 + 格式校验）
- 不强制用户补全联系方式；不阻断无联系方式用户使用通知以外的功能
- 不修改数据库 schema 或 Flyway 迁移

## Decisions

1. **前后端双层校验，选填 + 条件格式**
   - 前端：移除 `required`；使用共享 `optionalPhoneRule` / `optionalEmailRule`，仅在有值时校验
   - 后端：移除注册手机非空；所有写入口 `StringUtils.trimToNull` 后持久化
   - *备选*：仅改前端 — 拒绝，后端仍会拒绝注册

2. **唯一性校验仅在非空时执行**
   - `SysRegisterService`、`SysTeamServiceImpl` 与现有 `SysProfileController` / `SysUserController` 对齐
   - *备选*：空手机也走 `checkPhoneUnique` — 拒绝，空串可能误判重复

3. **共享工具模块 `user-contact-rules.js`**
   - 统一选填校验、`normalizeContactFields`、`formatContactDisplay`
   - *备选*：各页面复制 rules — 拒绝，易漂移

4. **通知提示放在「接收平台」Tab 顶部**
   - 打开通知设置时 `getUserProfile` 判断 phone/email 是否缺失
   - 缺一则展示 `el-alert`，不阻断保存
   - *备选*：写入 IM 默认配置 — 拒绝，用户可能从未打开通知设置

5. **i18n 键**
   - `member.not-set`、`member.phone-number-optional-placeholder`、`member.email-optional-placeholder`、`notice.contact-profile-hint`
   - 覆盖 zh-CN、zh-TW、en-US、ru、ja-JP、ko-KR、ar

## Risks / Trade-offs

- **[Risk] 多个用户均无手机，通知单发渠道无默认收件** → 通知设置提示 + 各平台表单仍可手动填写；发送失败时 IM 层已有 warn 日志
- **[Risk] 前端提交空字符串导致 UNIQUE 冲突** → 提交前 `normalizeContactFields` + 后端 `trimToNull`
- **[Risk] 历史文档（user-register.md）仍写手机必填** → 可在 tasks 中更新 readme，非本变更阻塞项
- **[Trade-off] 注册页不收集手机** → 用户可在个人中心或通知设置中后续填写；注册流程仅保留账号、姓名、密码

## Migration Plan

- 纯应用层变更，无数据库迁移
- 部署前后端即可；已有用户数据不受影响
- 回滚：恢复前后端校验逻辑（无数据回滚需求）

## Open Questions

（无）
