## 1. 后端共享解析

- [x] 1.1 抽取 `SetupFormDefaultsSupport`（JDBC URL 解析、`putMysqlFormFields`、`putRedisFormFields`、AI/存储字段提取），从 `UpgradePreflightService` 迁入并改为共用
- [x] 1.2 重构 `UpgradePreflightService` 调用共享支持类，确保现有 `UpgradePreflightServiceTest` 仍通过
- [x] 1.3 新增 `SetupPreflightService`：`exportFromEnvironment` → 类型推断 → 组装 preflight Map（含 `databaseType`、`cacheType` 及表单字段）

## 2. Setup API

- [x] 2.1 在 `SetupController` 增加 `GET /setup/preflight`，已安装时返回错误（与 setup 其它 API 一致）
- [x] 2.2 确认 `SetupFilter` / `SecurityConfig` 对 `GET /setup/preflight` 在未安装阶段放行
- [x] 2.3 新增 `SetupPreflightServiceTest`：覆盖 MySQL+Redis env、部分 env、JDBC 推断、无 env 空响应

## 3. 前端安装向导

- [x] 3.1 在 `src/api/setup.js` 增加 `getSetupPreflight()`
- [x] 3.2 在 `setup/index.vue` 的 `created` 中调用 preflight，实现 `patchFormFromPreflight`（env → 现有默认 → 空，无 UI 来源标注）
- [x] 3.3 确认 `databaseName` 与 `mysqlDatabase` 字段在 MySQL 模式下与 preflight 的 `mysqlDatabase` 同步

## 4. 部署与文档

- [x] 4.1 更新 `deploy/docker/docker-compose.yml` 示例：补充 `SPRING_DATABASE_TYPE`、`CAT2BUG_CACHE_TYPE`、`SPRING_REDIS_PORT`（及可选 AI 变量注释）
- [x] 4.2 更新 `deploy/docker/README.md`：说明 Compose 环境变量与安装向导预填的对应关系

## 5. 验证

- [x] 5.1 运行 `mvn test -pl cat2bug-platform-admin -Dtest=SetupPreflightServiceTest,UpgradePreflightServiceTest`
- [x] 5.2 手动或冒烟：带 MySQL/Redis env 启动未安装实例，打开 `/setup` 确认表单已预填且可一键测试连接并提交
