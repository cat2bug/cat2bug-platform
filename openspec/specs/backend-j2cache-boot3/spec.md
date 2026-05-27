## ADDED Requirements

### Requirement: J2Cache Spring Boot 3 自动配置

项目 MUST 提供适用于 Spring Boot 3 的 J2Cache 集成（自研 starter 或等价自动配置），替代 **`j2cache-spring-boot2-starter`**，并继续依赖 **`j2cache-core`** 提供 `CacheChannel` Bean。

#### Scenario: 应用启动注入 CacheChannel

- **WHEN** 使用默认 `application.yml` 与 `h2-j2cache.properties` 启动 `cat2bug-admin`
- **THEN** Spring 上下文中存在可用的 `CacheChannel`，`RedisCache` 可正常注入

### Requirement: 配置文件契约不变

J2Cache 配置 MUST 继续通过 `j2cache.config-location` 加载 `classpath:${spring.database-type}-j2cache.properties`；现有键名（如 `j2cache.L1.provider_class`、`j2cache.L2.provider_class`、`j2cache.serialization`）MUST 保持兼容，无需业务代码修改。

#### Scenario: H2 开发模式（无 L2）

- **WHEN** `spring.database-type` 为 h2 且 `h2-j2cache.properties` 中 `j2cache.L2.provider_class=none`
- **THEN** 应用启动成功，仅使用一级缓存，不因缺少 Redis 而失败

#### Scenario: MySQL 生产模式（Caffeine + Redis）

- **WHEN** 使用 `mysql-j2cache.properties` 且 Redis 可用
- **THEN** L1 为 Caffeine、L2 为 Redis（`SpringRedisProvider`），与迁移前语义一致

### Requirement: RedisCache 业务契约

`com.cat2bug.common.core.redis.RedisCache` 的公共方法签名与各 `*_CACHE_REGION` 常量 MUST 保持不变；依赖 `RedisCache` 的组件（如 `TokenService`、`SysLoginService`、`SysConfigServiceImpl`、防重复提交拦截器）MUST 无需修改调用方式即可工作。

#### Scenario: 登录 Token 缓存

- **WHEN** 用户登录成功
- **THEN** Token 写入 `LOGIN_TOKEN_CACHE_REGION`；后续请求携带 Token 可解析出 `LoginUser`

#### Scenario: 验证码

- **WHEN** 请求验证码并提交登录
- **THEN** 验证码在 `VERIFY_CODE_CACHE_REGION` 中校验后删除

#### Scenario: 字典与参数配置缓存

- **WHEN** 读取系统配置或字典
- **THEN** 命中 `CONFIG_CACHE_REGION` / `DICT_CACHE_REGION` 缓存逻辑与迁移前一致

### Requirement: 条件装配的 Redis 辅助配置

当 `j2cache.L2.provider_class` 为 Redis 实现类时，`RedisConfig`（`RedisTemplate`、限流脚本）与 `RateLimiterAspect` 的 **`@ConditionalOnProperty`** 条件 MUST 继续生效。

#### Scenario: L2 为 Redis 时限流可用

- **WHEN** 配置为 MySQL + Redis L2
- **THEN** `RedisTemplate` Bean 存在且限流切面可加载（若启用相关接口）
