# 首次安装向导 — 手工测试清单

本文档用于验证 `first-run-setup-wizard` 变更的端到端行为。建议在干净环境（无 `config/install/application-install.yml`、H2 空库或新 MySQL 库）下执行。

## 前置条件

- JDK 17+
- 后端：`cd cat2bug-platform-admin && mvn spring-boot:run`
- 前端（开发模式）：`cd cat2bug-platform-ui && npm run dev`，访问 `http://localhost:2222`
- 或嵌入式前端：先 `npm run build:embedded` 再启动 JAR

## 1. 未安装时进入向导

- [ ] 删除或移走 `./config/install/application-install.yml`（若存在）
- [ ] 使用全新 H2 数据目录（删除 `./data/cat2bug_platform.mv.db`）或空 MySQL 库
- [ ] 启动应用后访问 `/` 或 `/login`，应自动重定向到 `/setup`
- [ ] 直接访问 `/setup` 可正常打开 7 步向导页面
- [ ] 未安装时访问业务 API（如 `/prod-api/system/user/list`）应被拦截（403 或等价错误）

## 2. 各步骤表单与连接测试

- [ ] **数据库**：选择 H2 或 MySQL，填写连接信息后点击「测试连接」，成功/失败提示正确
- [ ] **缓存**：选择 `local` 时无需 Redis；选择 `redis` 时填写 host/port/password，点击「测试连接」可 PING 成功
- [ ] **存储与日志**：填写 `cat2bug.profile`、`cat2bug.temp`、日志路径，点击路径测试，目录存在且可写时通过
- [ ] **AI**：可选启用 Ollama，填写 host 后测试连接
- [ ] **管理员**：默认 `admin` / `cat2bug` 可保留或修改
- [ ] **安全策略**：可选开启「允许注册」「登录验证码」（注册页不校验验证码）

## 3. 提交安装

- [ ] 完成全部步骤后提交，显示成功页及**重启提示**
- [ ] 生成 `./config/install/application-install.yml`，内容与所选配置一致
- [ ] `cat2bug.cache.type` 为 `local` 或 `redis`，与 `spring.database-type`（H2/MySQL）相互独立
- [ ] 重启应用后可用安装时设置的管理员账号登录
- [ ] `sys_config` 中 `cat2bug.install.completed` = `true`

## 4. 安装后锁定 setup

- [ ] 重启后访问 `/setup` 应被拒绝或重定向（不可再次进入向导）
- [ ] `GET /prod-api/setup/status` 返回已安装状态
- [ ] 安装后业务 API 与登录页正常可用

## 5. 跳过向导（Docker / 自动化）

- [ ] 设置环境变量 `CAT2BUG_INSTALL_SKIP=true` 后启动，不进入 `/setup`，行为与已安装实例一致
- [ ] 可通过外部 yml 或环境变量预配置数据库与缓存，无需走向导

## 6. 登录验证码与注册

- [ ] 安装时关闭「登录验证码」：登录页不显示验证码
- [ ] 安装时开启「登录验证码」：仅**登录**需要验证码；**注册**页无验证码表单项，注册成功不依赖验证码
- [ ] `sys.account.captchaEnabled` 空值时视为 `false`（默认关闭）

## 7. Legacy 升级实例（重要）

适用于从旧版本升级、数据库中**已存在** `sys_user.user_name = 'admin'` 且 `del_flag = '0'` 的实例。

- [ ] **不应**因 `cat2bug.install.completed` 未写入而被重定向到 `/setup`
- [ ] 启动后 `InstallService` 自动将 legacy admin 识别为已安装，并写入 `cat2bug.install.completed = true`（若此前缺失）
- [ ] 用户可照常访问 `/login` 并使用原有 admin 账号登录
- [ ] 访问 `/setup` 应被拒绝（与已安装实例一致）

验证方式示例：

1. 使用含 seed admin 的旧库或 Flyway 已灌入 admin 的库启动应用
2. 确认浏览器打开 `/login` 而非 `/setup`
3. 查库：`SELECT config_value FROM sys_config WHERE config_key = 'cat2bug.install.completed'` 应为 `true`（首次启动 legacy 检测后）

## 8. 后端单元测试

```bash
mvn test -pl cat2bug-platform-framework -Dtest=InstallServiceTest,SysRegisterServiceTest
```

- [ ] `InstallServiceTest`：skip 标志、完成标记、legacy admin 检测、`run()` 自动标记
- [ ] `SysRegisterServiceTest`：无验证码 code/uuid 仍可注册成功
