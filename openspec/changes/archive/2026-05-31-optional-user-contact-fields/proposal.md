## Why

当前公开注册、个人中心基本资料、团队/项目成员创建等流程均强制填写手机号码与电子邮件，与部分用户仅使用用户名登录、在通知设置中单独配置 IM 渠道的实际用法不一致，也增加了不必要的注册与维护门槛。将手机与邮箱改为选填，可在不破坏登录与核心业务的前提下，降低账号创建成本，并与通知模块「按平台单独配置收件信息」的设计对齐。

## What Changes

- 公开注册（`/register`）：不展示、不收集手机号码；用户可在个人中心后续补充
- 个人中心基本资料：手机号码、电子邮件改为选填；空值在信息卡展示为「未设置」
- 团队/项目成员创建与成员管理对话框：手机号码、电子邮件改为选填
- 后端注册、个人资料更新、团队创建成员、系统用户增改：移除手机非空硬校验；空字符串规范为 `null` 再入库；有值时才校验唯一性
- 通知设置（接收平台 Tab）：当个人资料缺少手机或邮箱时，展示提示引导用户在各平台单独配置
- i18n：补充「未设置」、选填占位符、通知提示等文案（含多语言）
- 共享前端校验工具：选填场景下「有值才校验格式」

## Capabilities

### New Capabilities

- `user-contact-fields`: 用户手机与邮箱在注册、个人资料、成员创建等场景下的选填规则、空值展示、后端规范化与唯一性校验，以及通知设置中的缺失提示

### Modified Capabilities

（无：现有 `openspec/specs/` 中无覆盖用户注册/个人资料联系方式必填的独立 spec）

## Impact

- **前端**：`register.vue`、`user/profile/userInfo.vue`、`user/profile/index.vue`、`CreateTeamMember.vue`、`member/index.vue`、`notice/option/index.vue`；新增 `utils/user-contact-rules.js`；i18n 各语言文件
- **后端**：`SysRegisterService`、`SysProfileController`、`SysUserController`、`SysTeamServiceImpl`
- **数据库**：`sys_user.phonenumber` / `email` 已为可空；MySQL `phonenumber` 唯一索引允许多个 `NULL`，须避免写入空字符串
- **通知**：邮件/企微/飞书等默认收件仍来自个人资料，未填写时需在通知平台配置中手动填写，不影响登录与缺陷等业务流程
- **测试**：`SysRegisterServiceTest` 增加无手机号注册用例
