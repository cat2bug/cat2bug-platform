# J2Cache 与 Native 缓存策略

**Change:** `spring-native-delivery`  
**相关代码:** `RedisCache.java`、`application-native.properties`、`Cat2BugJ2CacheAutoConfiguration`

## 行为摘要

| 部署形态 | 缓存实现 | 多实例会话共享 |
|----------|----------|----------------|
| JVM + `cat2bug.cache.type=local` | J2Cache L1（Caffeine） | 否 |
| JVM + `cat2bug.cache.type=redis` | J2Cache L1 + Redis L2 | **是** |
| **Native 单实例**（默认冒烟/嵌入式） | `RedisCache.NATIVE_FALLBACK` 进程内 `ConcurrentHashMap` | 否（单进程内有效） |
| **Native 生产多实例** | 同上（当前实现**始终**走 Native fallback） | **否** — 见下文限制 |

Native 运行时通过 `NativeImageSupport.isRunningNativeImage()` 判定；在 Native 下 `RedisCache` **不调用** J2Cache `CacheChannel`（避免 Caffeine 动态类/J2Cache 在 Graal 下的反射问题）。

单实例场景下验证码、`login token`、配置缓存等短生命周期数据在进程内可用，与 `native-api-smoke.sh` / `native-spa-smoke.sh` 验收一致。

## 生产多实例（Redis）配置

安装向导或 `config/install/application.yml` 中设置：

```yaml
cat2bug:
  cache:
    type: redis

spring:
  redis:
    host: redis.example.com
    port: 6379
    password: your-password
    database: 0
```

MySQL 安装模板见 `defaults/application-install-mysql.yml`。

**重要限制（2026-06-15）：** 即便 `cat2bug.cache.type=redis`，**Native 二进制仍会使用进程内 fallback**，多实例间**不会**通过 Redis 共享 token/验证码。水平扩展生产集群请：

- 使用 **JVM embedded JAR** + Redis（现有成熟路径），或
- 单机 Native 部署，或
- 待后续 OpenSpec 项实现「Native + Redis-only」并移除 unconditional fallback。

JVM 多实例 Redis 路径仍由 J2Cache + `spring.redis.*` 驱动；可用 `deploy/test/native-redis-smoke.sh` 验证 Redis 连通与 Setup 测试接口。

## 冒烟

```bash
# 1. 启动 Redis（脚本会自动拉起并清理）
docker run -d --name cat2bug-redis-smoke -p 6379:6379 redis:7-alpine

# 2. 实例已监听（Native 或 JVM）
./deploy/test/native-redis-smoke.sh http://127.0.0.1:2020

# 或一键（含 Redis 生命周期）
./deploy/test/native-redis-smoke.sh --with-redis http://127.0.0.1:2020
```

脚本验证：

1. `redis-cli PING` → `PONG`
2. `POST /setup/test/redis`（host=127.0.0.1, port=6379）
3. `GET /captchaImage` 两次 → uuid 不同（单实例 fallback 或 Redis 均可）

## 已知问题

- Native 无条件 fallback：多实例 sticky session 外无法共享登录态。
- `RateLimiterAspect` 仅在 `j2cache.L2.provider_class=redis` 时加载；Native fallback 下限流键亦在进程内。
- 嵌入式 H2 单文件部署无需 Redis；文档与 `application-native.properties` 注释已说明。
