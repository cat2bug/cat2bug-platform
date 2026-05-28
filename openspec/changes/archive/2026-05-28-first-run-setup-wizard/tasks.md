## 1. 安装状态与配置基础

- [x] 1.1 在 `application.yml` 增加 `spring.config.import` 导入 `config/install/application-install.yml`
- [x] 1.2 新增 `cat2bug.install.*` 配置项与 `InstallProperties`（install 文件路径、skip 标志）
- [x] 1.3 实现 `InstallService`：读取/写入 `cat2bug.install.completed`；legacy 检测：`sys_user` 存在 `admin` 用户则自动标记已安装
- [x] 1.4 调整 `H2Config`：未安装时不灌入带 seed admin 的数据，或改为仅 Flyway bootstrap
- [x] 1.5 新增 Flyway 迁移：写入 `cat2bug.install.completed` 与更新 `sys_config` 种子（无默认 admin 密码行）

## 2. 后端 Setup API

- [x] 2.1 新增 `SetupController`：`GET /setup/status`、`POST /setup/test/database`、`POST /setup/test/redis`（PING 测试，与 DB 测试同样可独立调用）、`POST /setup/test/ollama`、`POST /setup/test/path`
- [x] 2.1b 实现 `SetupRedisTestService`：短超时 Jedis 连接 + PING，支持 host/port/password/database
- [x] 2.2 新增 `POST /setup/submit`：校验参数、写 `application-install.yml`、跑 Flyway、创建管理员、写 `sys_config`
- [x] 2.3 实现 `SetupConfigWriter`：按 H2/MySQL 生成 datasource；按 `cat2bug.cache.type`（local/redis，与 DB 无关）生成 J2Cache/Redis 配置；以及 profile、logging、ai
- [x] 2.6 将 `j2cache.config-location` 改为 `${cat2bug.cache.type}-j2cache.properties`，新增/重命名 `local-j2cache.properties` 与 `redis-j2cache.properties`
- [x] 2.4 `SecurityConfig` 放行 `/setup/**`（仅 `installed=false` 时）并禁止安装后访问
- [x] 2.5 新增 `SetupFilter` 或拦截器：未安装时拒绝非 setup 业务 API（保留 captcha/version 等白名单）

## 3. 管理员与安全策略

- [x] 3.1 安装提交时通过 `SysUserService` 创建管理员（BCrypt）；默认 `admin`/`cat2bug`，允许用户保留默认密码
- [x] 3.2 写入 `sys.account.registerUser`（用户选择）
- [x] 3.3 写入 `sys.account.captchaEnabled`（默认 `false`，用户可选开启）
- [x] 3.4 支持 `CAT2BUG_INSTALL_SKIP=true` 跳过向导逻辑

## 4. 登录验证码解耦（login-captcha）

- [x] 4.1 `SysRegisterService` 移除验证码校验逻辑
- [x] 4.2 `SysConfigServiceImpl.selectCaptchaEnabled`：空值时返回 `false`（替代原 default true）
- [x] 4.3 更新 `sys_config` 种子与参数设置文案为「登录验证码」
- [x] 4.4 前端 `register.vue` 移除验证码表单项、`getCode` 调用及相关校验

## 5. 前端 Setup 向导

- [x] 5.1 新增路由 `/setup` 与 `views/setup/index.vue` 多步向导（7 步）
- [x] 5.2 实现各步骤表单：数据库（含测试连接）、缓存（local/Redis；Redis 含 host/port/password +「测试连接」按钮）、存储/日志、AI、管理员（默认 admin/cat2bug）、安全策略
- [x] 5.3 对接 setup API（测试连接、提交、状态查询）
- [x] 5.4 `permission.js`：未安装时重定向至 `/setup`；`/setup` 加入白名单
- [x] 5.5 提交成功页展示重启提示
- [x] 5.6 添加 i18n 中英文文案（`setup.*` 键）

## 6. 日志与存储路径

- [x] 6.1 `logback-spring.xml` 支持 `${LOG_PATH:./logs}` 外部化日志目录
- [x] 6.2 Setup 路径测试 API 校验目录存在且可写
- [x] 6.3 安装配置写入 `cat2bug.profile` / `cat2bug.temp` 并在文档说明 Docker 卷对齐

## 7. 测试与文档

- [x] 7.1 后端单元/集成测试：install 状态检测、submit 创建 admin、captcha 仅 login
- [x] 7.2 前端 e2e 或手工测试清单：首次安装流程、安装后锁定 setup
- [x] 7.3 更新 CLAUDE.md / 用户文档：首次安装向导、Docker skip、重启说明
- [x] 7.4 验证 legacy 升级实例（`sys_user` 已有 `admin`）不被错误重定向到 setup
