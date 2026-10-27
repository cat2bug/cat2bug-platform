## 1. 分支与配置骨架

- [x] 1.1 在当前 Spring `cloud` tip 打 tag `cloud-spring-final`，并以当时 `core` 为父重建 `cloud` 工作线（不把 `core` merge 进 Spring 树）
- [x] 1.2 增加 Maven profile `cloud` 与 Quarkus `%cloud`：默认构建不依赖华为 SDK、JustAuth、钉钉 SDK
- [x] 1.3 `%cloud` 配置只引用环境变量（钉钉、微信开放平台、OBS、短信、回调基址 `https://cloud.cat2bug.com`），仓库中不写入密钥值
- [x] 1.4 确认 `.env.cloud` 为根路径、`/prod-api`、`/ws`，并去掉登录与导航里写死的 `/cloud` 前缀

## 2. 数据表

- [x] 2.1 从生产库副本导出 `c_auth_user`、`c_project_push` 的建表语句，作为列清单（用户手工核对通过）
- [x] 2.2 添加可重复执行的 Flyway（MySQL，以及 H2 测试所需脚本）：表已存在时不丢行
- [x] 2.3 在空库与生产副本上各跑一次迁移，确认登录绑定与推送记录仍在（用户手工测通）

## 3. 三方登录与微信小程序

- [x] 3.1 实现 `/three/auth/uri/{source}`、`/three/auth/login/{source}`，`source` 仅 `wechat` 与 `dingtalk`，回调为 `https://cloud.cat2bug.com/three-login`
- [x] 3.2 实现绑定、注册并绑定、解绑；同一三方身份不能绑到第二个用户
- [x] 3.3 未绑定的回调不发 token，前端进入 `/three-bind`
- [x] 3.4 登录页提供微信与钉钉入口；`PublicPaths` 放行授权 URL 与三方登录，绑定与解绑仍要登录
- [x] 3.5 退出登录不清微信小程序 openid，并用同一 openid 再次登录验证仍是原用户（用户手工测通）

## 4. 短信登录

- [x] 4.1 用校验证书的 HTTPS 调用华为短信发一次性验证码（Redis、过期、用后作废、号码与 IP 限频），不移植关闭 TLS 校验的旧客户端
- [x] 4.2 实现 `POST /sms/login`：正确验证码签发与账密相同的 token
- [x] 4.3 登录页正式短信入口（不用 `.vue.temp`），走完发码到进入系统（用户手工测通）

## 5. OBS 存储

- [x] 5.1 cloud profile 用 OBS 替换上传、下载、删除；对象键为去掉 profile 前缀的相对路径；返回 bucket URL
- [x] 5.2 删除已不存在的对象视为成功
- [x] 5.3 未启用 cloud profile 时上传仍走本地目录且不要求 OBS 密钥

## 6. 项目同步

- [x] 6.1 移植入站收包、`c_project_push` 列表与删除
- [x] 6.2 按成员映射同步入库，失败回滚且推送行不得标成功（当前同步范围：项目名称/简介与成员补齐；用例缺陷等全量导入未移植）
- [x] 6.3 私有端出站默认 `cat2bug.cloud.host` 改为 `https://cloud.cat2bug.com/prod-api`
- [x] 6.4 用一包样例从私有端推到云上并在同步页完成导入（用户手工测通）

## 7. 同域部署冒烟

- [x] 7.1 云站 nginx 与 `deploy/docker/nginx.conf` 对齐：`/`、`/prod-api/` 剥前缀、`/ws/`、`/oauth/` 与 `/mcp` 原路径转发
- [x] 7.2 JVM `%cloud` 冒烟：四种登录、上传 URL 可打开、同步一包、OAuth/MCP 路径不被加上 `/prod-api`（用户手工测通）

## 8. 回写长期知识

- [x] 8.1 新增 `docs/07-技术决策/ADR-022-云站以Quarkus为底座且cloud配置只使用华为OBS.md`（原任务写 ADR-018，与「移除报告」撞号，改用 022），并登记到 `docs/07-技术决策/说明.md` 与 `docs/00-项目总览.md`
- [x] 8.2 更新 `docs/02-系统架构/部署.md`：域名为 `cloud.cat2bug.com`，同域 `/prod-api`，cloud 存储仅为 OBS
- [x] 8.3 可复用踩坑：无新增专篇（风险已写入 ADR-022 / 部署说明）
- [x] 8.4 在 `docs/08-版本记录/2026-10.md` 记一行本变更
