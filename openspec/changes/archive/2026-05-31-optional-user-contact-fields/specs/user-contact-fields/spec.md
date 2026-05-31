## ADDED Requirements

### Requirement: 公开注册不提供手机号码

系统 SHALL 在公开注册页面不展示、不收集手机号码；注册成功后 `phonenumber` 为 NULL。用户可在个人中心后续填写手机。

#### Scenario: 注册页无手机号字段

- **WHEN** 用户打开公开注册页面
- **THEN** 表单不包含手机号码输入项

#### Scenario: 无手机号注册成功

- **WHEN** 用户提交有效的用户名、姓名、密码
- **THEN** 系统创建账号成功且 `phonenumber` 为 NULL

### Requirement: 个人中心联系方式为选填

系统 SHALL 允许登录用户在个人中心基本资料中留空手机号码与电子邮件；填写时 MUST 校验格式与唯一性。

#### Scenario: 清空联系方式并保存

- **WHEN** 用户将手机与邮箱清空并保存
- **THEN** 系统持久化为 NULL 且保存成功

#### Scenario: 信息卡展示未设置

- **WHEN** 用户手机或邮箱为空
- **THEN** 个人中心左侧信息卡对应项显示「未设置」（或当前语言的等价文案）

### Requirement: 团队成员创建联系方式为选填

系统 SHALL 允许团队/项目管理员创建成员时不填写手机号码与电子邮件；填写时 MUST 校验格式与唯一性。

#### Scenario: 创建无联系方式成员

- **WHEN** 管理员填写必填账号信息且手机、邮箱为空
- **THEN** 成员创建成功

### Requirement: 联系方式空值规范化

系统 MUST 在持久化前将手机与邮箱的空字符串规范为 NULL；MUST NOT 将多个用户的 `phonenumber` 存为空字符串。

#### Scenario: 前端提交空字符串

- **WHEN** 客户端提交 `phoneNumber: ""` 或 `email: ""`
- **THEN** 数据库存储为 NULL 且不因 UNIQUE 约束失败

### Requirement: 通知设置缺失联系方式提示

系统 SHALL 在用户打开通知设置的「接收平台」时，若个人资料中缺少手机号码或电子邮件，展示非阻断式提示，说明需在下方各平台单独配置收件信息。

#### Scenario: 缺少手机或邮箱时显示提示

- **WHEN** 用户打开通知设置且个人资料中手机或邮箱至少一项为空
- **THEN** 接收平台 Tab 顶部显示提示信息

#### Scenario: 联系方式均已填写

- **WHEN** 用户个人资料中手机与邮箱均有值
- **THEN** 不显示该提示
