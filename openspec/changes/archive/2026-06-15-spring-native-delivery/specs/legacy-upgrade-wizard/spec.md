## MODIFIED Requirements

### Requirement: 升级向导后端实现

Legacy 升级向导的后端 MUST 继续由 **Spring MVC** 实现；在 **Spring Native embedded** 为默认 Release 时，`cat2bug.upgrade.state` 写入 install 文件、全锁策略、`POST /upgrade/retry` 幂等续跑 MUST 与 JVM 版一致。

#### Scenario: Spring Native 升级状态查询

- **WHEN** 访问 `GET /upgrade/status` on Native
- **THEN** 返回与 JVM 版相同 JSON 结构（含 state、lastStep 等字段）

#### Scenario: Spring Native 升级失败重试

- **WHEN** state 为 `failed` 且调用 `POST /upgrade/retry` on Native
- **THEN** 从 `lastStep` 续跑 config 或 migration 步骤

#### Scenario: Native 升级全锁

- **WHEN** 升级进行中且客户端访问非 upgrade/setup/静态 白名单 API
- **THEN** 拒绝或重定向行为与 JVM 版一致

#### Scenario: Legacy 库 Native 全流程

- **WHEN** 在 AlmaLinux 或 CI 使用 Legacy 生产库快照执行升级向导至 success
- **THEN** Native 与 JVM 版均写入 `cat2bug.upgrade.state: success` 且业务 API 可用
