## 1. 后端

- [x] 1.1 `SysRegisterService`：注册流程不读取、不校验、不写入 `phoneNumber`
- [x] 1.2 `SysProfileController` / `SysUserController`：保存前 `trimToNull` phone/email
- [x] 1.3 `SysTeamServiceImpl.insertSysUser`：仅有值时校验手机/邮箱唯一性；保存前 `trimToNull`
- [x] 1.4 新增 `SysRegisterServiceTest.register_succeedsWithoutPhoneNumber`

## 2. 前端共享与表单

- [x] 2.1 新增 `utils/user-contact-rules.js`（选填校验、normalize、展示格式化）
- [x] 2.2 `register.vue`：移除手机号表单项与相关校验；保留账号、姓名、密码
- [x] 2.3 `user/profile/userInfo.vue`：手机/邮箱选填、提交前 normalize
- [x] 2.4 `CreateTeamMember.vue`、`member/index.vue`：手机/邮箱选填、提交前 normalize

## 3. 个人中心与通知 UX

- [x] 3.1 `user/profile/index.vue`：空手机/邮箱显示「未设置」及次要样式
- [x] 3.2 `notice/option/index.vue`：缺联系方式时在接收平台 Tab 展示提示

## 4. 国际化

- [x] 4.1 补充 `member.not-set`、选填占位符、`notice.contact-profile-hint`（zh-CN / zh-TW / en-US / ru / ja-JP / ko-KR / ar）

## 5. 验证

- [x] 5.1 公开注册页无手机号字段；注册 → 登录 → 个人中心可后续填写或留空
- [x] 5.2 团队管理员创建无手机/邮箱成员成功
- [x] 5.3 填写手机/邮箱时格式错误仍被拒绝；重复手机号/邮箱仍被拒绝
- [x] 5.4 通知设置：资料缺项时见提示；资料完整时不显示
- [x] 5.5 `mvn test -pl cat2bug-platform-framework -Dtest=SysRegisterServiceTest` 通过
