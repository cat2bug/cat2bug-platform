## ADDED Requirements

### Requirement: REST API 契约对 Vue2 前端兼容

在后端升级至 Spring Boot 3.3.x 后，现有 **Vue2 前端** MUST 在不修改代码的前提下完成核心业务流程：登录、项目/团队切换、缺陷与用例等主要模块的增删改查。

#### Scenario: 登录与会话

- **WHEN** 使用默认账号通过现有登录页登录
- **THEN** 获得与迁移前相同形态的 Token/会话；后续 `/prod-api` 请求鉴权成功

#### Scenario: 统一响应结构

- **WHEN** 调用返回 `AjaxResult` 的接口（成功或业务失败）
- **THEN** JSON 结构（`code`、`msg`、`data` 等字段）与迁移前一致，前端不出现解析错误

### Requirement: JWT 与 Header 不变

Token 的传递方式（Header 名称）、签发与校验逻辑 MUST 与迁移前对前端可见的行为一致；不得要求前端修改 Token 存储或请求拦截器。

#### Scenario: 刷新页面保持登录

- **WHEN** 用户已登录并刷新浏览器
- **THEN** 在 Token 有效期内仍保持登录状态

### Requirement: WebSocket 消息兼容

现有 WebSocket 端点与消息 JSON 格式（如通知推送）MUST 与 Vue2 客户端 `vue-native-websocket` 集成兼容。

#### Scenario: 登录后 WebSocket 连接

- **WHEN** 用户登录且后端 WebSocket 已配置
- **THEN** 客户端可建立连接并接收 `code==200` 的业务推送（与迁移前一致）

### Requirement: 嵌入式静态资源与代理

`npm run build:embedded` 产物嵌入 `cat2bug-admin` static 后，通过单 JAR 访问前端路由与 `/prod-api` 代理行为 MUST 与迁移前一致。

#### Scenario: 单 JAR 访问

- **WHEN** 仅启动 `cat2bug-admin.jar` 并访问根路径
- **THEN** 可加载前端 SPA；API 请求指向同一进程后端

### Requirement: 无前端协同发布

本变更 MUST NOT 包含 `cat2bug-platform-ui` 的 Vue3 或依赖大版本升级；回归以 **当前主干 Vue2 前端** 为准。

#### Scenario: 前端仓库无提交

- **WHEN** 审查本变更的 Git 范围
- **THEN** 不包含对 `cat2bug-platform-ui/package.json` 中 `vue` 主版本升级的提交（文档链接除外）
