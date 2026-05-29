# 常见问题

## 登录问题

### Q: 忘记密码怎么办？
A: 请联系管理员重置密码，或使用"忘记密码"功能通过邮箱重置。

### Q: 登录后提示 Token 过期？
A: Token 默认有效期为 240 分钟，过期后需要重新登录。

### Q: 无法登录，提示用户名或密码错误？
A: 请检查：
1. 用户名和密码是否正确
2. 账号是否被禁用
3. 是否使用了正确的登录地址

## 缺陷管理问题

### Q: 为什么看不到某些缺陷？
A: 可能原因：
1. 数据权限限制（仅能看到自己创建或分配给自己的缺陷）
2. 缺陷已被删除
3. 项目权限不足

### Q: 如何批量导入缺陷？
A:
1. 下载缺陷导入模板
2. 按模板格式填写数据
3. 在缺陷列表页点击"导入"按钮
4. 上传填好的 Excel 文件

### Q: 缺陷状态无法修改？
A: 检查是否有 `system:defect:edit` 权限。

### Q: 为何缺陷中粘贴剪贴板中的图片功能无法使用？
A: 由于现代浏览器安全限制，必须配置https证书后，才能使用此功能。

## 文件上传问题

### Q: 上传文件失败？
A: 可能原因：
1. 文件大小超过限制（默认 500MB）
2. 文件类型不支持
3. 磁盘空间不足

### Q: 支持哪些文件格式？
A: 支持常见的图片、文档、压缩包等格式。

## 性能问题

### Q: 系统响应慢？
A: 可能原因：
1. 数据量过大，建议定期归档
2. 缓存未启用或失效
3. 数据库连接池不足

### Q: 如何优化系统性能？
A:
1. 启用 Redis 缓存
2. 配置数据库索引
3. 定期清理日志和临时文件
4. 使用 MySQL 替代 H2（生产环境）

## 通知问题

### Q: 收不到通知？
A: 检查：
1. 通知设置是否开启
2. 邮件服务器配置是否正确
3. WebSocket 连接是否正常

### Q: 如何关闭某类通知？
A: 在"个人中心"-"通知设置"中配置。

## 数据库问题

### Q: H2 数据库文件在哪里？
A: 默认位置：`./data/cat2bug_platform.mv.db`

### Q: 如何切换到 MySQL？
A: 运行期数据库与连接池等基础设施配置**仅**由外部安装文件 `./config/install/application-install.yml` 提供（路径可通过 `cat2bug.install.config-path` 调整）。**不要**再编辑 `application-mysql.yml` 或设置 `spring.profiles.active`。

- **首次安装**：克隆仓库后磁盘上无 `application-install.yml`，启动应用会进入 `/setup` 安装向导；向导按所选数据库类型从 JAR 内 classpath 模板（`defaults/application-install-h2.yml` / `defaults/application-install-mysql.yml`）合并写入完整 install 文件，并设置 `cat2bug.install.completed: true`。提交后**需重启**应用，新数据源才会生效。
- **已安装实例**：直接编辑 `config/install/application-install.yml`，将 `spring.database-type` 改为 `mysql`，并按模板结构填写 `spring.datasource.druid.master` 的 URL、用户名、密码等（可参考 classpath 中的 MySQL 模板字段）。修改后**重启**应用。
- **Docker / 自动化**：挂载带 `cat2bug.install.completed: true` 的 install 文件，或设置 `CAT2BUG_INSTALL_SKIP=true` 并预配置数据源（见安装变更测试清单）。

### Q: 数据库迁移失败？
A: 检查 Flyway 版本脚本是否正确，查看日志获取详细错误信息。

## 其他问题

### Q: 如何联系技术支持？
A:
- 官方网站：https://www.cat2bug.com
- Gitee：https://gitee.com/cat2bug/cat2bug-platform

### Q: 系统支持哪些浏览器？
A: 推荐使用 Chrome、Firefox、Edge 等现代浏览器，IE 不支持。

### Q: 如何升级系统版本？
A:

**准备**

1. 在维护窗口内操作，并**备份**数据库、`config/install/`、上传目录等业务数据
2. 确保**仅运行一个** Cat2Bug 进程（升级向导不支持多实例协调）
3. 停止旧版本，替换为新版本 JAR 或镜像

**有 Legacy 数据或待执行 Flyway 脚本时（升级向导）**

适用于：磁盘无 completed 的 `application-install.yml`，但库中已有旧版数据（如 `sys_user.user_id=1`），或新版本存在未执行的 Flyway migration。

1. 启动应用后浏览器会进入 **`/upgrade`**（5 步：概览、配置确认、预检、执行、完成/失败重试）
2. 升级进行中系统**全锁**：无法登录或访问业务 API，仅可访问升级接口与静态资源
3. 在向导中确认配置（**默认保留**已有 JDBC、路径等，模板仅补缺失项），通过预检后提交
4. 提交成功会写入/更新 `config/install/application-install.yml` 并执行 Flyway；成功后状态为 `restart_required`，**必须重启**应用
5. 若迁移失败：查看向导中的 `lastError`，修复后点击**重试**（`POST /upgrade/retry`，按 `lastStep` 幂等续跑）
6. 重启后 `upgradeRequired` 为 false，可正常登录

**仅替换 JAR、无 pending migration、且已有 completed install 时**

直接重启即可，通常无需进入 `/upgrade`。

**跳过升级向导（Docker / 自动化）**

与旧版静默迁移行为一致，可自动写出 completed install 文件：

```bash
export CAT2BUG_UPGRADE_SKIP=true
```

或配置 `cat2bug.upgrade.skip=true`。

**全新空库**

无 legacy 数据时仍走 **`/setup`** 首次安装向导，不会进入 `/upgrade`。

手工测试清单见 `openspec/changes/legacy-upgrade-wizard/TESTING.md`。
