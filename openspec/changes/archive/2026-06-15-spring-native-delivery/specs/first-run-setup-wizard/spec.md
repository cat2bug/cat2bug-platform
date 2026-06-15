## MODIFIED Requirements

### Requirement: 安装向导后端实现

首次安装向导的后端实现 MUST 继续由 **Spring MVC** 提供（非 Quarkus）；在 **Spring Native embedded** 为默认 Release 时，对外 URL、步骤数、配置写入路径 `config/install/application-install.yml` 与 **安装后重启** 要求 MUST 与 JVM 版不变。

#### Scenario: Spring Native 提供安装 API

- **WHEN** Spring Native embedded 二进制启动且实例未安装
- **THEN** `GET /setup/status` 返回 `installed: false`
- **AND** 安装向导各步骤 API 可用且写入 install 文件格式与 JVM 版兼容

#### Scenario: Spring Native 安装后重启

- **WHEN** 经 Native 进程完成安装向导 submit
- **THEN** 必须重启 Native 进程后新 datasource/cache 配置生效
- **AND** 重启后 `GET /setup/status` 返回 `installed: true`

#### Scenario: Native 下 Setup 与 SPA 共存

- **WHEN** 未安装且用户访问 `/`
- **THEN** UI 导航至 `/setup` 且静态资源由同一 Native 进程提供

#### Scenario: JVM 安装行为不变（回归）

- **WHEN** 使用 JVM embedded JAR 执行安装流程
- **THEN** 行为与 Native 场景一致
