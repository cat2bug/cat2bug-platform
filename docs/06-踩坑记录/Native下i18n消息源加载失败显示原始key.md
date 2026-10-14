# Native 下 i18n 消息源加载失败显示原始 key

记录 Spring Native（GraalVM AOT）下国际化消息不生效的一串问题。

- 日期：2026-07-04
- 涉及：Native / Spring 后端

## 现象

Spring Native 镜像在 Docker 里启动后：

- AOT 处理阶段直接失败，或启动时 `I18nConfig` 报错；
- 能启动后，登录页、安装向导的错误提示显示原始消息 key（如 `user.password.not.match`），而不是中文/英文文案；
- 中文 properties 出现乱码。

## 根因

1. `@EnableConfigurationProperties(MessageSourceProperties)`：Spring Boot 的 `MessageSourceProperties` 在 Native AOT 时不是可注册的 `@ConfigurationProperties` 类型。
2. `ReloadableResourceBundleMessageSource` 在 GraalVM 下按 basename 扫描资源不可靠，且 `i18n/messages*.properties` 没有注册为 Native 资源，运行时找不到。
3. MessageSource 未显式 UTF-8，properties 文件编码不统一。

## 解决办法

- `cd319dd0`：改用 `Binder` 从 `Environment` 绑定 MessageSource 配置。
- `af489817`：修正 GraalVM 下 basename 加载，`MessageUtils` 增加兜底。
- `98155f4c`：新增 `Cat2BugMessageSource`，启动时直接从 classpath 读取 `i18n/messages*.properties`；新增 `I18nNativeRuntimeHints` 注册资源；`MessagesEncodingTest` 校验编码。
- `e197871c`：MessageSource 显式 UTF-8，规范化 properties 编码。

Quarkus Native 同类问题：i18n、安装模板、报告预置模版须写进 `quarkus.native.resources.includes`（见 `openspec/changes/archive/2026-10-14-restore-quarkus-full-delivery/NATIVE-RELEASE.md`）。

## 以后怎么避免

- 新增 classpath 资源（i18n、模板、预置文件）时，同步登记 Native 资源清单。
- Native 冒烟必须覆盖一次「登录失败提示文案」，能第一时间发现显示原始 key。
- 不要依赖框架内部 `*Properties` 类做配置绑定，Native 下优先 `Binder`。
