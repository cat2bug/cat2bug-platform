## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: 嵌入式静态资源与单进程访问

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
