# Docker Compose 部署说明

## 环境变量

### `CAT2BUG_UPGRADE_SKIP=true`

等价于配置 `cat2bug.upgrade.skip=true`。用于 Docker / CI 等无需人工进入升级向导的场景。

| 场景 | 行为 |
|------|------|
| 无 `application-install.yml` 的 legacy 实例 | 启动时自动从当前 Environment 写出 completed install 文件 |
| **install 已完成**且存在待执行 Flyway 脚本 | **不**进入 `/upgrade` UI；启动时后台静默执行 Flyway migrate |
| 静默 migrate **失败** | 进程以**非零状态退出**（`System.exit(1)`），容器应重启或告警，避免半升级状态继续服务 |

换 JAR / 镜像升级且已挂载 completed install 时，推荐在 `cat2bug-platform-web-after` 服务中设置：

```yaml
environment:
  CAT2BUG_UPGRADE_SKIP: "true"
```

### `CAT2BUG_INSTALL_SKIP=true`

跳过首次 `/setup` 向导（预配置 install 文件或 Environment 数据源时使用）。

## 首次安装与附着旧库

- **全新空库**：无 install 文件 → 浏览器进入 `/setup`
- **已有 H2 `.mv.db` 或 MySQL schema**：无 install 文件 → 仍进入 `/setup`（`databaseMode: existing`），setup 阶段不 Flyway；install 完成后若 schema 落后，再进入 `/upgrade` 或由 `CAT2BUG_UPGRADE_SKIP` 静默 migrate

## 启动

```bash
cd deploy/docker
docker-compose up -d
```

默认端口：`8022`（经 nginx 反代后端 `2020`）。
