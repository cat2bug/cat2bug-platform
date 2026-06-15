## ADDED Requirements

### Requirement: Spring Boot AOT 与 RuntimeHints 主策略

Spring Native 构建 MUST 以 **Spring Boot 3 AOT 自动生成** 为主；对 AOT 未覆盖的 classpath 资源与反射 MUST 通过 **`RuntimeHintsRegistrar`**（如 `Cat2BugNativeRuntimeHints`）或等价 `@ImportRuntimeHints` 注册。手工 `META-INF/native-image/*.json` 仅作兜底，MUST 在代码注释或 `readme/spring-native-delivery/` 说明引入原因。

#### Scenario: 静态与 Mapper 资源注册

- **WHEN** 执行 `-Pnative` 构建 embedded 产物
- **THEN** native-image 包含 `classpath*:static/**`、`classpath*:mapper/**/*.xml`、`classpath*:db/migration/**`
- **AND** 运行时 `GET /static/` 下文件可读取

#### Scenario: AOT 处理失败时有文档化兜底

- **WHEN** 某第三方库 AOT 失败且需手工 json
- **THEN** 变更 PR 或 Phase 文档记录库名、json 路径与移除条件

### Requirement: MyBatis 与 PageHelper Native 可达性

全量 MyBatis Mapper 接口与 XML MUST 在 Native 镜像中可加载；分页（PageHelper 或等价拦截器）MUST 在 Native 下列表接口返回正确 `TableDataInfo`。

#### Scenario: Mapper XML 运行时加载

- **WHEN** Native 进程启动并调用任意依赖 `mapper/**/*.xml` 的 Service
- **THEN** 不出现 `BindingException` 或 XML 找不到错误

#### Scenario: 分页列表 Native 冒烟

- **WHEN** 对已安装实例 `GET` 缺陷或用户分页列表 API
- **THEN** 返回 `rows` 与 `total` 且与 JVM 版同条件抽样一致

### Requirement: Spring Security 与 Filter 链 Native

JWT 鉴权、`SecurityFilterChain`、公开路径白名单（login、setup、upgrade、static、captcha）MUST 在 Native 下与 JVM 行为一致；`EmbeddedSpaFallbackFilter` MUST 在存在 `classpath:/static/index.html` 时生效。

#### Scenario: 未认证访问静态资源

- **WHEN** Native 进程运行且 `GET /static/js/*.js` 无 Authorization
- **THEN** 返回 200（非 401）

#### Scenario: 受保护 API 未授权

- **WHEN** `GET /system/user/list` 无有效 Token
- **THEN** 返回 401 或与 JVM 版相同拒绝语义

### Requirement: J2Cache Native profile

Native profile MUST 定义可运行的缓存策略：优先 **Redis**（`cat2bug.cache.type=redis`）；若 Native 下禁用 J2Cache local 层，MUST 在 `application-native.properties` 与部署文档中说明 **Native 包对 Redis 或等价配置的依赖**。

#### Scenario: Redis 缓存 Native 启动

- **WHEN** Native 配置 `cat2bug.cache.type=redis` 且 Redis 可达
- **THEN** 应用启动成功且字典/参数缓存接口可用

### Requirement: Flyway 与双数据库驱动

Native 镜像 MUST 包含 H2 与 MySQL 驱动及 `db/migration/{h2|mysql}/**` 脚本资源，以支持安装向导与 Legacy 升级；Flyway MUST 仍仅由 Setup/Upgrade 向导触发（非启动自动 migrate），与 JVM 语义一致。

#### Scenario: Setup 选 H2 完成安装

- **WHEN** 未安装实例经 Native 完成 H2 安装 submit
- **THEN** 写入 `config/install/application-install.yml` 且重启后可登录

#### Scenario: Setup 选 MySQL 完成安装

- **WHEN** 未安装实例经 Native 完成 MySQL 安装 submit
- **THEN** 连接外部 MySQL 成功且 Flyway 由向导执行

### Requirement: WebSocket 与 IM Native

`/websocket/{memberId}/message`（或项目现行 WebSocket 端点）MUST 在 Native 下可建立连接并收发与 JVM 版相同格式的消息（Phase 2 parity 验收项）。

#### Scenario: WebSocket 握手成功

- **WHEN** 已登录客户端连接 Native 提供的 WebSocket 端点
- **THEN** 连接建立且无 Graal 反射类初始化错误

### Requirement: Quartz 或等价调度 Native

`cat2bug-platform-quartz` 中已启用的 Job MUST 在 Native 下启动不报错；若 Native profile 改用 `@Scheduled` 替代部分 Job，MUST 在 `PHASE-2.md` 列出差异且 **不影响核心业务定时语义**。

#### Scenario: Native 启动无 Quartz 初始化失败

- **WHEN** Native 进程启动且 quartz 模块在 classpath
- **THEN** 日志无 Quartz Scheduler 致命错误导致进程退出

### Requirement: Excel Native 无 POI

Native profile MUST **不包含 Apache POI** 运行时依赖；Excel 导入导出 MUST 使用 Graal 友好实现（如 FastExcel，可 cherry-pick Quarkus 线）。JVM profile 可保留 POI 直至迁移完成，但 **Native 构建 MUST 排除 POI**。

#### Scenario: Native 构建无 POI 类

- **WHEN** 检查 embedded Native 二进制或使用 `native-image` 报告
- **THEN** 不包含 `org.apache.poi` 主要类或 Native 构建显式 `-Pnative` 将 POI 设为 `provided`

#### Scenario: Excel 导出 Native 冒烟

- **WHEN** 调用已迁移的 Excel 导出 API
- **THEN** 返回有效 xlsx 字节流且 HTTP 200

### Requirement: Captcha 无 AWT

验证码生成 MUST 不依赖 AWT/`libawt.so`；MUST 使用纯字节 PNG 渲染（如 `CaptchaPngRenderer`）。Native 二进制 MUST 不包含 `libawt.so`（或等价图形库）。

#### Scenario: captchaImage Native

- **WHEN** `GET /captchaImage`  on Native
- **THEN** 返回 PNG 图片且进程未加载 AWT native 库

### Requirement: Native profile 生产依赖裁剪

profile **`native`**（及 Release 组合）MUST 排除以下运行时依赖：`spring-boot-devtools`、`cat2bug-generator`、Knife4j/Swagger **UI** starter；与 `backend-prod-packaging-slim` 生产语义一致。

#### Scenario: Native 二进制不含 generator

- **WHEN** 检查 Native 构建 classpath 或运行 `/tool/gen/**`
- **THEN** 生产 Native 不提供代码生成 API（404 或未注册）

#### Scenario: Native 不含 swagger-ui 页面

- **WHEN** `GET /doc.html` 或 `/swagger-ui/**` on Native Release
- **THEN** 不提供服务或返回 404（OpenAPI 注解编译所需最小依赖除外）
