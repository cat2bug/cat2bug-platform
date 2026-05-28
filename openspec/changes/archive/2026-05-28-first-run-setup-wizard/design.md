## Context

当前 Cat2Bug 启动流程：

- `spring.database-type` 决定加载 `application-h2.yml` 或 `application-mysql.yml`，在 Spring 上下文刷新前生效
- H2 空库时 `H2Config` 执行 `h2-schema.sql`，内含 `admin`/`cat2bug` 及 `sys_config` 种子（`captchaEnabled=false`、`registerUser=true`）
- 验证码开关 `sys.account.captchaEnabled` 同时控制 `SysLoginService` 与 `SysRegisterService`
- 验证码类型 `cat2bug.captchaType`（math/char）在 yml，需重启生效
- Flyway 默认关闭（`cat2bug.auto-update=false`）
- Ollama 在 `cat2bug.ai.enabled=true` 时可能启动即 pull 模型

目标用户以**单机 JAR 首次部署**为主；Docker 部署可通过环境变量预配置并跳过向导。

**产品决策（已确认）**：

| 项 | 决策 |
|----|------|
| 新用户默认密码 | 不在向导中配置 |
| 验证码范围 | 仅登录；注册不校验验证码 |
| 验证码默认 | 关闭 |

## Goals / Non-Goals

**Goals:**

- 未安装时自动进入 Setup 向导，完成基础设施与安全配置
- 安装时创建管理员账号，**默认用户名 `admin`、密码 `cat2bug`**（与现有文档一致，用户可修改，**不禁止**使用 `cat2bug`）
- 持久化配置到 `config/install/application-install.yml`，重启后生效
- 提供数据库、Redis、Ollama、日志路径、文件存储路径的连接/可用性测试（Redis 与数据库一样支持向导内「测试连接」）
- 登录验证码独立配置，默认关闭；注册永不因该开关要求验证码
- 安装完成标记不可被未授权重复触发

**Non-Goals:**

- 向导中配置 `sys.member.initPassword`
- 嵌入式 Redis 实现（仍使用外部 Redis 或 H2 无 L2）
- 无重启热切换数据库类型（安装阶段明确提示重启）
- 替换 Docker Compose 为主安装方式（Docker 可跳过 wizard）
- 将 `captchaType` 迁入 `sys_config`（保持 yml，默认 math）

## Decisions

### 1. 安装状态检测

**决策**：以 `sys_config` 键 `cat2bug.install.completed` = `true` 为主；辅以启动时检测 **`sys_user` 表中是否存在用户名为 `admin` 的记录** 作为 legacy 兜底（现有 SQL 种子/Flyway 均会写入 `admin` 用户）。

**未安装判定**：`cat2bug.install.completed` ≠ `true` **且** `sys_user` 中无 `admin` 用户 → 进入 Setup 向导。

**理由**：与现有 `sys_config` 缓存机制一致，安装后可在参数设置中审计（只读或隐藏）。

**备选**：纯文件 `config/.installed` —— 与 DB 状态可能不一致，不采用。

### 2. Setup 模式 vs 两阶段 JAR

**决策**：**单 JAR、双模式**——未安装时 Spring 仍完整启动，但通过 `SetupFilter` / 拦截器将非 setup API 与前端路由重定向到 `/setup`；Setup API 在 `SecurityConfig` 中 `permitAll`。

**理由**：避免维护独立 setup 迷你应用；H2 可在首次启动即用嵌入式库完成安装写入。

**备选**：独立 setup profile 仅加载少量 Bean —— 开发与 H2Config/Flyway 集成成本高。

### 3. 配置持久化

**决策**：安装提交时写入 `config/install/application-install.yml`（路径可配置），内容包含：

```yaml
spring:
  database-type: h2|mysql
  datasource: ...        # 按 profile 结构
  redis: ...             # 仅 cat2bug.cache.type=redis 时
cat2bug:
  cache:
    type: local|redis    # 独立于 database-type
  profile: ...
  ai:
    enabled: ...
    host: ...
logging:
  file:
    path: ...            # 或通过 logback-spring.xml 外部化
  install:
    completed: true      # 同步写入 sys_config
```

主 `application.yml` 增加：

```yaml
spring:
  config:
    import: optional:file:./config/install/application-install.yml
```

**理由**：与 Spring Boot 2.4+ 配置导入一致，Docker 仍可用 env 覆盖。

### 4. 数据库初始化顺序

**决策**：

1. Setup 阶段使用当前 profile（默认 h2）连接数据库
2. 若空库：执行 Flyway（安装时强制 `cat2bug.auto-update=true` 一次）或精简 bootstrap migration，**不**再灌入带默认 admin 密码的完整 `h2-schema.sql` 管理员行
3. 安装 API 创建管理员用户（BCrypt）；表单**预填** `admin` / `cat2bug`，用户可修改
4. 若用户选择 MySQL：先连接测试，写 install yml，`database-type=mysql`，**提示重启**后由 Flyway mysql 迁移初始化

**理由**：统一 schema 来源为 Flyway，逐步废弃 `H2Config` 整文件灌入（安装完成后 `H2Config` 仅在 legacy 无 Flyway 时保留兼容）。

### 5. 缓存方式（与数据库解耦）

**决策**：缓存与数据库为**两个独立维度**，向导分步配置：

| 维度 | 配置键 | 可选值 |
|------|--------|--------|
| 数据库 | `spring.database-type` | `h2` / `mysql` |
| 缓存 | `cat2bug.cache.type` | `local`（仅 Caffeine L1）/ `redis`（Caffeine + Redis L2） |

**组合示例**（均合法）：

| 数据库 | 缓存 | 典型场景 |
|--------|------|----------|
| H2 | local | 本地开发、单机演示 |
| H2 | redis | 本地 DB + 共享 Redis 试验 |
| MySQL | local | 小团队单节点，无 Redis |
| MySQL | redis | 生产多实例 |

**J2Cache 配置来源**：将 `application.yml` 中 `j2cache.config-location` 从 `${spring.database-type}-j2cache.properties` 改为 `${cat2bug.cache.type}-j2cache.properties`（`local-j2cache.properties` / `redis-j2cache.properties`，可由现有 h2/mysql 文件重命名或别名）。

**向导行为**：

- Step 3「缓存」**始终展示**，不因 H2/MySQL 跳过
- 选择 `local`：无需 Redis 连接信息
- 选择 `redis`：必填 Redis host/port（及可选 password/database），提供**「测试连接」按钮**，调用 `POST /setup/test/redis` 执行 PING；**仅测试成功后才允许进入下一步或提交**（与数据库类型无关）

**Redis 测试 API**：使用 Jedis/Lettuce 对提交的 host/port/password 做短超时连接 + `PING`，返回 `{ success, message }`；不写入 install 配置，与 `POST /setup/test/database` 行为一致。

**理由**：用户明确要求数据库选择不关联缓存；MySQL 单节点、H2+Redis 等组合应支持。

**备选**：继续用 database-type 推导缓存 —— 与用户要求冲突，不采用。

### 6. Ollama / AI

**决策**：

- 向导提供「启用 AI」「Ollama 地址」「默认业务模型」
- 安装时默认 `cat2bug.ai.enabled=false`，避免启动 pull 公网模型
- 提供「测试连接」调用 Ollama `/api/tags`（超时 10s）
- 不在安装阶段自动 pull 模型

### 7. 日志与文件存储

**决策**：

- 文件存储：写入 `cat2bug.profile`、`cat2bug.temp`，安装前校验目录可写
- 日志：写入 `logging.file.path` 或 `LOG_PATH` 环境变量（logback-spring.xml 使用 `${LOG_PATH:./logs}`）
- 变更需重启；向导最后一步明确说明

### 8. 安全策略（sys_config）

| 键 | 向导默认 | 说明 |
|----|----------|------|
| `sys.account.captchaEnabled` | `false` | **语义变更**：仅控制登录验证码 |
| `sys.account.registerUser` | 用户选择（建议内网 true / 公网 false） | 不变 |
| `cat2bug.install.completed` | `true` | 安装完成 |

不在向导暴露 `sys.member.initPassword`。

### 9. 登录验证码与注册解耦

**决策**：

- `SysLoginService`：继续读取 `selectCaptchaEnabled()`
- `SysRegisterService`：**移除**验证码校验逻辑
- `CaptchaController`：`/captchaImage` 增加可选 query `scope=login`（默认），注册页不再请求验证码
- 前端 `register.vue`：移除验证码表单项与校验

**理由**：满足「目前只登录用」；避免新增配置键，减少迁移成本。参数设置文案改为「登录验证码」。

### 10. 前端向导结构

```
/setup
  Step 1  欢迎 + 环境预设（内网/公网，影响推荐值）
  Step 2  数据库（H2 / MySQL + 连接表单 + 测试）
  Step 3  缓存（本地 / Redis 独立选择；选 Redis 时填连接信息 +「测试连接」）
  Step 4  存储与日志（路径 + 可写检测）
  Step 5  AI（可选，默认关闭 + Ollama 测试）
  Step 6  管理员账号 + 安全（默认 admin/cat2bug，可改；注册开关、登录验证码默认关）
  Step 7  确认 + 提交 → 成功页「请重启应用」
```

`permission.js`：若 `GET /setup/status` 返回 `installed=false`，除 `/setup` 外重定向到 setup。

### 11. Docker 跳过安装

**决策**：若环境变量 `CAT2BUG_INSTALL_SKIP=true` 或检测到 `spring.datasource.druid.master.url` 已由 env 提供且 `cat2bug.install.completed` 已设，则跳过向导。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 未安装时 Setup API 无鉴权被扫描 | 仅监听内网提示；可选 `setup.token` 一次性密钥；安装完成即关闭 |
| MySQL 安装需重启，用户体验断裂 | 明确 UI 文案 + 文档；安装前用 JDBC 测试连接 |
| `captchaEnabled` 语义变更影响已有部署 | 发布说明标注；原依赖注册验证码的客户需另寻方案（本变更明确不做） |
| Flyway 与 `h2-schema.sql` 双轨 | 安装路径统一 Flyway；保留 H2Config 只读兼容一个版本 |
| install yml 含数据库密码明文 | 文件权限 600；文档建议生产用 env 覆盖敏感项 |
| Setup 期间与 H2Config 竞态 | `H2Config` 在 `cat2bug.install.completed` 未设置时不灌 seed admin |

## Migration Plan

1. 新安装：首次访问 → Setup → 写 install yml + sys_config → 重启 → 正常
2. 现有 H2/MySQL 已运行库：`cat2bug.install.completed` 缺省时，若 `sys_user` 中**已有 `admin` 用户**（SQL 种子默认即存在），则自动设置 `cat2bug.install.completed=true`，不进入向导
3. 升级后 `SysRegisterService` 不再校验验证码——注册流程变简单，无数据迁移
4. 回滚：删除 `config/install/application-install.yml`，清除 `cat2bug.install.completed`，恢复备份（若有）

## Open Questions

- 是否在 MVP 中支持 MySQL 在线安装，或 Phase 1 仅 H2 + 安全项，MySQL 仍文档 + env？（建议 Phase 1：H2 全向导；MySQL 连接测试 + 写 yml + 重启）
- `setup.token` 是否纳入 MVP？
- 安装完成后是否强制管理员首次登录改密（**否**——允许长期使用默认 `admin`/`cat2bug`）
