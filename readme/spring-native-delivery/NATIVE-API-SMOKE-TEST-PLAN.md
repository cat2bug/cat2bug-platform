# Native 镜像 API 冒烟测试计划

## 背景

Spring Native（GraalVM）在运行时对反射、字符集、JWT、MyBatis 插件等有额外要求。每次修复单个点后若缺少回归测试，容易在下一功能点再次暴露问题。

本计划用于 **Docker minimal 镜像**（`http://127.0.0.1:2020`）的自动化冒烟，覆盖登录、上传、权限、团队等核心链路。

## 已修复的 Native 问题（回归必测）

| 序号 | 现象 | 根因 | 修复位置 |
|------|------|------|----------|
| 1 | 登录 `Executor.query` | PageHelper 反射 | `MyBatisNativeConfiguration` |
| 2 | 登录 `SSLMSW` | J2Cache/Caffeine | `RedisCache` Native 兜底 |
| 3 | 登录 JWT 失败 | jjwt-impl 动态类 | `JwtNativeRuntimeHints` |
| 4 | 上传 `CharsetKit` | GBK 未打包 | `CharsetKit` 安全初始化 |
| 5 | 创建团队 `hasPermi` | `@PreAuthorize` SpEL 反射 | `SecurityNativeRuntimeHints` |

## 测试分层

### L1：无鉴权 / 公共接口

- `GET /version` → `200`，body 含版本号
- `GET /captchaImage` → `200`
- `GET /setup/status` → `200`

### L2：鉴权基础

- `POST /login`（demo/123456 或 admin/cat2bug）→ `200` + `token`
- `GET /getInfo`（带 Bearer）→ `200` + user/roles

### L3：文件与团队（用户截图路径）

- `POST /common/upload`（multipart）→ `200` + url
- `POST /system/team`（JSON：teamName、teamIcon、remark）→ `200`，**不得**出现 `MissingReflectionRegistrationError`
- `GET /system/team/my` → `200`

### L4：权限注解接口抽样（314 处 @PreAuthorize）

按模块各抽 1 个 GET/POST，响应码应为 `200` 或业务 `401/403`，**不得**为 `500` 且 msg 含 `MissingReflectionRegistrationError`、`NoClassDefFoundError`、`ExceptionInInitializerError`。

| 模块 | 示例接口 | 权限 |
|------|----------|------|
| 团队 | `GET /system/team/list` | system:team:list |
| 项目 | `GET /system/project/list` | system:project:list |
| 缺陷 | `GET /system/defect/list` | system:defect:list |
| 用例 | `GET /system/case/list` | system:case:list |
| 成员 | `GET /system/member/list` | system:member:list |
| 管理端团队 | `GET /admin/team/list` | admin:team:list |

### L5：H2 SQL 分支抽样（Phase 2）

覆盖 H2 嵌入式库下易出语法差异的统计/仪表盘 SQL（`json_contains`、`date_format` 等），需先登录获取 token。

| 类型 | 示例接口 |
|------|----------|
| 公共 | `GET /captchaImage`、`GET /setup/status` |
| 缺陷统计 | `GET /system/defect/statistic/type/{projectId}` |
| 仪表盘 | `GET /system/dashboard/{projectId}/defect` |
| 扩展 | open-workload、defect-line、member-defect、state 等 |

```bash
./deploy/test/native-h2-sql-smoke.sh
# 或指定 base URL
./deploy/test/native-h2-sql-smoke.sh http://127.0.0.1:2020
```

### L6：Playwright UI 路径（可选）

1. 打开 `/login`，demo 登录
2. 进入创建团队页，上传图标、填写名称、提交
3. 断言无红色 `MissingReflectionRegistrationError` 通知

## 执行方式

```bash
# 1. 构建并启动 minimal 镜像
cd .worktree/feature-spring-native-upx
SKIP_EMBEDDED=true UPX_IN_DOCKER=true ./deploy/build-native-spring.sh aarch64
PORT=2020 ./deploy/docker/run-native-spring-minimal.sh pack minimal

# 2. API 冒烟 L1–L4（bash，Node 16 可用）
./deploy/test/native-api-smoke.sh

# 3. Phase 2 + H2 SQL 冒烟 L5
./deploy/test/native-h2-sql-smoke.sh

# 4. Playwright UI 冒烟 L6（需 Node 18 + playwright）
node deploy/test/native-ui-smoke.mjs
```

## 通过标准

- L1–L5 全部用例通过
- 失败用例数为 0
- 无 Native 典型错误关键字出现在响应 `msg` 中
- L5 不得出现 H2 SQL 语法错误（`json_contains`、`date_format` 等）

## 多 Agent 分工

| Agent | 职责 |
|-------|------|
| A | 修复 Native hints / 代码并触发 rebuild |
| B | 维护 `native-api-smoke.mjs`，跑 L1–L4 |
| C | Playwright UI 创建团队路径（L5） |
| D | 扫描 Controller 增量补充 L4 抽样列表 |

## 变更记录

- 2026-06-14：初版；补充 PermissionService 反射与冒烟脚本
- 2026-06-14：API 冒烟结果（Security 修复后）
  - 通过：登录、上传、**创建团队**、admin/team/list、case/list
  - 待跟进：`/system/team/my` PageHelper+BoundSql 反射（已加 hints，需 rebuild）
  - ~~非 Native 问题：defect/list 的 `json_contains` 为 H2 语法差异~~ → **FIXED 2026-06-14**（`SysDefectStatisticMapper.xml` H2 分支改写；L5 `native-h2-sql-smoke.sh` 回归）

## 执行脚本

| 脚本 | 说明 |
|------|------|
| `deploy/test/native-api-smoke.sh` | L1–L4 curl 冒烟（Node 16 可用） |
| `deploy/test/native-h2-sql-smoke.sh` | L5 Phase 2 + H2 SQL 抽样（Node 16 可用） |
| `deploy/test/native-api-smoke.mjs` | L1–L4 Node 18+ fetch 版 |
| `deploy/test/native-ui-smoke.mjs` | L6 Playwright UI（需 Node 18 + `npx playwright install chromium`） |
