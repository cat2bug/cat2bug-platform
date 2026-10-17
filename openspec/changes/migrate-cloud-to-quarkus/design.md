## Context

`core` 已按 `docs/07-技术决策/ADR-010-移除Spring后端并统一模块命名.md` 删除 Spring，默认构建是 Quarkus。`cloud` 仍是 Spring，且是 `master` 的超集：独立模块 `cat2bug-platform-cloud`（JustAuth 钉钉/微信、华为短信、华为 OBS、`c_auth_user`、`c_project_push`）。两边互不为祖先，不能快进合并。

云站对外域名是 `https://cloud.cat2bug.com`。API 与 UI 同域反代，形状与 `deploy/docker/nginx.conf`、`cat2bug-platform-ui/.env.cloud` 一致：UI 在 `/`，API 前缀 `/prod-api`（剥前缀转到后端），WebSocket `/ws/`。旧 cloud 配置里的 `/cloud`、`/cloud/cloud-api` 和 `http://` 回调不再作为目标。

生产库已有三方绑定与推送记录，但仓库 Flyway 中没有 `c_auth_user`、`c_project_push` 的建表脚本。短信登录在 Spring cloud 上是半成品：能发码，`POST /sms/login` 被注释，前端组件名为 `SmsLogin.vue.temp`。旧华为短信客户端会关闭 TLS 校验，不能原样搬进 Quarkus。

## Goals / Non-Goals

**Goals:**

- `cloud` 分支 tip 变为「当前 `core` + 云能力」，发布线只有 Quarkus。
- cloud profile 下：微信扫码、钉钉、完整短信登录、微信小程序 openid 行为、OBS 唯一存储、入站项目同步。
- 站点路径与开源线 nginx 约定对齐，OAuth 回调使用 HTTPS 根路径。
- 密钥只来自环境变量；迁移可在已有生产表上重复执行。

**Non-Goals:**

- 不恢复 Spring 模块，不把 `cat2bug-platform-cloud` 原样留在 Quarkus 工程里。
- 不改 MCP、个人令牌、OAuth 授权服务器、IM 多渠道推送的既有需求。
- 本变更不交付云站 Native 镜像；先以 Quarkus JVM 作为云站运行时。
- 不把 OBS 做成开源默认存储；未启用 cloud profile 时仍用本地上传目录。

## Decisions

### 1. 分支策略：以 `core` 为父重建 `cloud` tip

在现 `cloud` tip 打 tag `cloud-spring-final`。新提交的第一父是当时的 `core`，云能力以新提交叠加上去。不用 `git merge core` 进 Spring 树。

备选：长期双轨（生产继续 Spring，旁边移植）。否决原因：两套后端会继续分叉，和 ADR-010 相反。

### 2. 云能力放进现有 Quarkus 模块，用构建 profile 隔开

三方登录、短信、OBS、入站同步写在 `cat2bug-platform-admin` / `framework` / `system`，由 Maven profile `cloud` 与 Quarkus profile `%cloud` 打开。默认（开源）构建不依赖华为 SDK、JustAuth、钉钉 SDK。CDI 用构建期或 profile 条件装配 OBS 实现，避免 Spring 式 `@Primary` 抢本地实现。

备选：新的 `cat2bug-platform-cloud` Quarkus 模块。模块边界更干净，但和「模块不再按技术栈/部署形态拆」的现状不一致，且公开构建要多维护一个 artifact。profile 足够把依赖挡在默认构建之外。

### 3. HTTP 用 JAX-RS，不移植 Spring Security Filter

保留现有路径契约，便于前端与私有端少改：

| 方法 | 路径 | 作用 |
|------|------|------|
| GET | `/three/auth/uri/{source}` | 返回微信或钉钉授权 URL |
| GET | `/three/auth/login/{source}` | 用 code 登录，已绑定则发 token |
| POST | `/three/auth/bind/{source}` | 已登录用户绑定 |
| POST | `/three/auth/register-bind/{source}` | 注册并绑定 |
| DELETE | `/three/auth/unlock/{authId}` | 解绑 |
| GET | `/sms/{phone}/code` | 发验证码 |
| POST | `/sms/login` | 校验验证码并发 token |

`source` 只允许 `wechat`、`dingtalk`。未绑定的三方登录返回明确错误，前端进入 `/three-bind`。这些路径在未登录时可访问的部分写入 `PublicPaths`（发码、授权 URL、三方登录、短信登录）；绑定与解绑仍要登录。

短信验证码放 Redis（cloud 部署已有 Redis），校验成功后删除。登录成功后的 token 与账密登录相同。

备选：继续用 Spring Security 的 `SmsAuthenticationTokenFilter`。Quarkus 没有这条过滤器链。

### 4. 短信客户端改为校验 TLS 的 HTTPS 调用

按华为短信 HTTP API 自行发请求，校验证书。模板变量、签名、通道号来自环境变量。不引入会 `trustAllHttpsCertificates` 的旧实现，也不再依赖 `system` scope 的 `java-sdk-core` jar。

### 5. OBS 是 cloud profile 的唯一 `FileUpload` 实现

对象键规则沿用现 `HuaWeiFileServiceImpl`：去掉 profile 前缀后的相对路径作为 OBS key。上传、下载、删除都走 OBS。删除时对象已不存在视为成功（Spring cloud 已有这个行为）。资源 URL 使用 bucket 公网地址，不再映射到本地 `profile/` 目录。开源 profile 不注册该实现。

### 6. 项目同步：入站移植，出站只改默认 URL

入站语义对齐 `PushProject042ServiceImpl`：按推送包版本选择实现，写入 `c_project_push`，同步时按调用方给出的成员映射入库。出站仍用 `core` 的 `ProjectCloudPushService`，默认 `cat2bug.cloud.host` 改为 `https://cloud.cat2bug.com/prod-api`。

Flyway：新增 MySQL（及 H2 测试用）脚本建 `c_auth_user`、`c_project_push`。脚本必须在表已存在时跳过或等价于当前生产结构（实施前导出生产 `SHOW CREATE TABLE` 作为列清单，不凭记忆改列）。

### 7. 站点与密钥

- 浏览器入口：`https://cloud.cat2bug.com/`
- API：`https://cloud.cat2bug.com/prod-api/` → 后端 `/`
- OAuth `redirectUri`：`https://cloud.cat2bug.com/three-login?source=wechat|dingtalk`
- 环境变量（名称在实现时落在 `%cloud` 配置，仓库不放值）：钉钉 clientId/secret、微信开放平台 clientId/secret、OBS ak/sk/endpoint/bucket/bucketUrl、短信 url/appKey/appSecret/sender/templateId/signature、三方回调基址
- 旧 yml 中的明文密钥不复制进 Git。若这些密钥曾进过历史提交，轮换是上线步骤，不是可选项。

### 8. 拟新增长期决策

实现并归档时新增 ADR：**ADR-018-云站以 Quarkus 为底座且 cloud 配置只使用华为 OBS**。说明云站不再保留 Spring 回滚线，以及 OBS 仅在 cloud profile 启用。

## Risks / Trade-offs

- [生产表结构与猜测的 DDL 不一致] → 实施第一步对照生产 `SHOW CREATE TABLE`，Flyway 以该结构为准，并先在库副本上演练。
- [JustAuth / 钉钉 SDK 在 Quarkus 上反射或原生镜像失败] → 本变更只要求 JVM；依赖能用 JAX-RS + 官方 HTTP API 替代 SDK 时优先少依赖。
- [OBS 公网 URL 与同域 API 不一致，前端把相对路径当本地文件] → 上传接口返回的 URL 必须是可直接访问的 OBS 地址；回归「合并后图片不可见」那类路径剥离。
- [历史密钥已在 Git] → 上线前轮换；新配置只用环境变量。
- [旧 `/cloud` 前缀仍被私有端写死] → 网关短期 301；出站默认 URL 随本变更改掉，已安装的旧私有端需改 `cat2bug.cloud.host`。
- [短信接口被刷] → 同一号码与同一 IP 限频；验证码短 TTL、一次性。

## Migration Plan

1. 给当前 `cloud` tip 打 `cloud-spring-final`（先本地 tag，推送与否由发布时决定）。
2. 从 `core` 开出新的 `cloud` 历史（或在 `cloud` 上以 `core` 为 ours 做一次性替换提交），再按 tasks 加云能力。
3. 在库副本补表/对齐表，导入绑定与推送数据的抽样。
4. 配置环境变量，JVM 启动 `%cloud`，nginx 使用与 `deploy/docker/nginx.conf` 相同的 location。
5. 冒烟：四种登录、上传后 URL 可打开、私有端推一包并在云上同步。
6. 回滚：切回 tag `cloud-spring-final` 的 Spring 进程与旧 nginx。数据库只加表或加可空列，不做破坏性删列，以便回滚进程仍能读原表。

## Open Questions

无。登录范围、OBS 唯一、入站同步、域名、同域 `/prod-api`、就地演进均已确认。

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/02-系统架构/整体架构.md`
- `docs/02-系统架构/部署.md`
- `docs/07-技术决策/说明.md`
- `docs/07-技术决策/ADR-010-移除Spring后端并统一模块命名.md`
- 拟新增：`docs/07-技术决策/ADR-018-云站以Quarkus为底座且cloud配置只使用华为OBS.md`
