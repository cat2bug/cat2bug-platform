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

### Requirement: Spring Native 运行时满足既有 API 契约

在 Spring Native embedded 成为默认 Release 产物时，现有 `backend-api-compat` 所定义的全部 Scenario MUST 仍通过（登录、`AjaxResult`、`TableDataInfo`、WebSocket、Open API），**不得**要求 Vue2 前端修改请求/响应解析逻辑。

#### Scenario: Native 下登录与会话

- **WHEN** 使用 Spring Native embedded 二进制启动且 Vue2 登录页指向 `/prod-api`
- **THEN** 登录成功并获得与 JVM 版相同形态的 Token；后续请求鉴权成功

#### Scenario: Native 下 AjaxResult

- **WHEN** 调用任意返回 `AjaxResult` 的业务接口
- **THEN** `code`、`msg`、`data`/`token` 字段形态与 JVM 版一致

#### Scenario: Native 下 WebSocket

- **WHEN** 前端连接 `/websocket/{memberId}/message`
- **THEN** 消息格式与 JVM 版一致且 IM 通知可达

### Requirement: 嵌入式静态资源与单进程访问（Spring Native + JVM）

`npm run build:embedded` 产物 MUST 嵌入 **`cat2bug-platform-admin`** 的 `classpath:/static/**` 后，通过 **单 Spring Native 可执行文件** 或 **单 JVM embedded JAR** 访问前端路由与 `/prod-api` 行为一致。Docker nginx 分离部署行为不变（见 `backend-prod-packaging-slim`）。

#### Scenario: 单 Spring Native ELF 访问

- **WHEN** 仅启动 Spring Native embedded 二进制并访问根路径
- **THEN** 可加载前端 SPA；API 请求由同一进程处理

#### Scenario: 单 JVM embedded JAR 访问（开发/回滚）

- **WHEN** 启动 `mvn package -Pembedded` 产出的 `cat2bug-admin.jar`
- **THEN** SPA 与 API 行为与 Native 场景一致（并行回归）

#### Scenario: Docker 分离部署不变

- **WHEN** 使用 `api-only` JAR + nginx 前端
- **THEN** 行为仍符合 `backend-api-compat` Docker 场景；不强制 Native
