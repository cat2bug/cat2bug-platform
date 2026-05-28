## Why

Cat2Bug 当前首次启动依赖 YAML/SQL 种子完成初始化：默认管理员（`admin`/`cat2bug`）硬编码在 SQL 中，数据库、Redis、Ollama、日志与文件路径等基础设施配置分散在多个 yml 文件与 Docker 环境变量中，无统一引导。新部署者（尤其单机 JAR 用户）需要手动编辑配置并理解 H2/MySQL 差异，存在运维门槛。需要提供系统首次启动配置向导，在安装阶段一次性完成关键配置。

## What Changes

- 新增首次启动 Setup 向导（前端 `/setup` + 后端 Setup API），在未安装状态下引导用户完成配置
- 支持配置：数据库类型与连接（H2 / MySQL）、**缓存方式（独立于数据库，可选本地 Caffeine 或 Redis，Redis 可测试连接）**、Ollama AI 服务、日志路径、文件存储路径、**管理员用户名与密码（默认 `admin`/`cat2bug`，可修改）**
- 支持配置安全策略：是否允许公开注册；登录是否启用验证码（**默认关闭**）
- 安装完成后写入持久化配置并标记 `installed`，引导重启后进入正常模式
- **BREAKING（行为变更）**：登录验证码与注册验证码解耦——验证码开关仅作用于**登录**，注册不再受 `sys.account.captchaEnabled` 影响
- 安装时创建或确认管理员账号（默认 `admin`/`cat2bug`），并写入 `sys_config` 安全项
- 不在向导中配置新用户默认密码（`sys.member.initPassword` 保持现有机制）
- Setup 模式期间放行 `/setup/**` 相关 API，安装完成后锁定

## Capabilities

### New Capabilities

- `first-run-setup-wizard`：首次启动检测、Setup 模式、配置向导 UI、基础设施配置持久化、连接测试、Schema 初始化、管理员账号创建、安装完成标记与重启引导
- `login-captcha`：登录验证码独立开关（默认关闭），注册流程不再校验验证码；Setup 向导与参数设置均可配置登录验证码

### Modified Capabilities

（无——现有 OpenSpec 能力域未覆盖安装向导或验证码行为）

## Impact

- **后端**：新增 `cat2bug-platform-admin` Setup 控制器/服务；扩展或替代 `H2Config` 安装检测逻辑；`SecurityConfig` 放行 setup 路由；`SysLoginService` / `SysRegisterService` 验证码逻辑拆分；可能新增 `application-install.yml` 或 install 状态文件
- **前端**：新增 `views/setup/` 多步向导；`permission.js` 白名单与未安装重定向；i18n 文案
- **配置**：`application.yml` 增加 `cat2bug.install.*`；Flyway / schema init 与安装流程集成
- **数据库**：新增或复用 `sys_config` 键（如 `cat2bug.install.completed`）；管理员用户由安装 API 创建而非仅依赖 seed
- **部署**：Docker 部署可选跳过 wizard（env 预配置）；单机 JAR 为主要受益场景
