# Native Docker 登录时 J2Cache 未注册导致 NPE

记录 Spring Native 镜像使用 Redis 缓存时登录失败、提示为空的问题。

- 日期：2026-07-04
- 涉及：Native / Spring 后端 / 部署

## 现象

Spring Native 镜像在 Docker 中、安装配置选择 Redis 缓存时：登录直接失败，前端提示空白或 NPE；安装向导也报不出真实错误。

## 根因

- J2Cache 的 `CacheChannel` 在 AOT 阶段没有被注册进 Native 镜像（`Cat2BugNativeJ2CacheAutoConfiguration` 条件不满足），运行期拿不到缓存通道。
- `RedisCache` 读取缓存未命中时返回 null，`SysPasswordService` 校验密码错误次数时直接解引用 → NPE。
- 错误消息本身又依赖 i18n，i18n 同时不可用时提示为空（见 [[Native下i18n消息源加载失败显示原始key]]）。

## 解决办法

- `af489817`：AOT 阶段注册 J2Cache（调整 `cat2bug-platform-admin/pom.xml` 与 `Cat2BugNativeJ2CacheAutoConfiguration`），`RedisCache` 增加安全读取。
- `8daef11d`：`RedisCache` 读失败/未命中兜底；`SysLoginService`、`SysPasswordService` 保证登录失败总能返回可读消息。
- 说明文档：`readme/spring-native-delivery/J2CACHE-NATIVE.md`（2026-10-14 已随 Spring 线删除，需要时从 tag `spring-final-1.1.0` 查看；当前后端缓存见 [[整体架构]]）。

## 以后怎么避免

- Native 下任何「按配置条件装配」的 Bean，都要确认 AOT 时条件成立（AOT 在构建期求值）。
- 缓存读取一律按「可能为 null」处理。
- Native 冒烟覆盖 H2 本地缓存与 Redis 缓存两种安装配置。
