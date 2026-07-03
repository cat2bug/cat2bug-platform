# J2Cache 与 Native 缓存策略

**Change:** `native-redis-cache`（原 `spring-native-delivery` Phase 2.3）  
**相关代码:** `RedisCache.java`、`application-native.properties`、`Cat2BugJ2CacheAutoConfiguration`、`J2CacheNativeRuntimeHints.java`

## 行为摘要

| 部署形态 | 缓存实现 | 多实例会话共享 |
|----------|----------|----------------|
| JVM + `cat2bug.cache.type=local` | J2Cache L1（Caffeine） | 否 |
| JVM + `cat2bug.cache.type=redis` | J2Cache L1 + Redis L2 | **是** |
| **Native + `cat2bug.cache.type=local`**（默认） | `RedisCache.NATIVE_FALLBACK` 进程内 `ConcurrentHashMap` | 否 |
| **Native + `cat2bug.cache.type=redis`** | J2Cache **Redis-only**（L1=`none`，L2=`SpringRedisProvider`） | **是** |

Native 默认仍为 `cat2bug.cache.type=local`（单实例零依赖）。安装向导或 `config/install` 写入 `redis` 并重启后，Native 经 J2Cache `CacheChannel` 读写 Redis，**不再**因 `isRunningNativeImage()` 无条件走进程内 fallback。

单实例 `local` 场景下验证码、短生命周期 token 等在进程内可用，与 `native-api-smoke.sh` / `native-spa-smoke.sh` 验收一致。

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

Native 使用 `redis-native-j2cache.properties`（L1 禁用、L2=Redis）。JVM 路径仍用 `redis-j2cache.properties`（Caffeine L1 + Redis L2）。

MySQL 安装模板见 `defaults/application-install-mysql.yml`。

**Docker Compose：** Native 容器须与 Redis **同一 Docker network**，`spring.redis.host` 设为 compose 服务名（如 `redis` 或 `cat2bug-platform-redis`），勿用 `127.0.0.1`（容器内指向自身）。

## GraalVM Hints

`J2CacheNativeRuntimeHints` 注册 J2Cache Redis provider、Jedis、fastjson 反射及 `*-j2cache.properties` / `caffeine.properties` 资源。见 `Cat2BugApplication` `@ImportRuntimeHints`。

## 冒烟

```bash
# 1. 启动 Redis
docker run -d --name cat2bug-redis-smoke -p 6379:6379 redis:7-alpine

# 2. 实例已监听（Native 或 JVM）；local 模式可不查 Redis key
./deploy/test/native-redis-smoke.sh http://127.0.0.1:2020

# 一键（含 Redis 生命周期）
./deploy/test/native-redis-smoke.sh --with-redis http://127.0.0.1:2020

# 实例已配置 cache.type=redis 时，验证码后必须在 Redis 看到 key
./deploy/test/native-redis-smoke.sh --with-redis --expect-redis http://127.0.0.1:2020
```

脚本验证：

1. `redis-cli PING` → `PONG`
2. `POST /setup/test/redis`（host=127.0.0.1, port=6379）
3. `GET /captchaImage` 两次 → uuid 不同
4. （Redis 可达时）`redis-cli KEYS` 匹配 `*verifyCode*` / `*token*` / `j2cache*`

双实例跨节点验收（手动步骤 + 探测）：

```bash
./deploy/test/native-redis-multi-instance-smoke.sh
BASE_A=http://127.0.0.1:2020 BASE_B=http://127.0.0.1:2021 ./deploy/test/native-redis-multi-instance-smoke.sh --probe
```

## 已知问题

- `cache.type=local` 的 Native 仍无跨实例共享；多实例生产须显式选 `redis`。
- `RateLimiterAspect` 仅在 J2Cache L2 为 Redis 时加载；`local` fallback 下限流键在进程内。
- 嵌入式 H2 单文件部署无需 Redis；`application-native.properties` 默认 `local`。
