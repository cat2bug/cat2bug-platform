## Why

线上云站 `cloud` 分支仍是 Spring Boot，而产品默认后端已按 [[ADR-010-移除Spring后端并统一模块命名]] 切到 Quarkus（`core`）。两条线不能再硬合并：自上次把 `core` 合进 `cloud` 之后，`core` 已删除 Spring 并继续前进。云站需要就地换成 Quarkus，同时保留云站相对开源线的能力（三方登录、短信登录、华为 OBS、私有端项目同步）。

## What Changes

- **就地演进 `cloud` 分支**：以当前 `core`（Quarkus）为新 tip 的父；现有 Spring `cloud` tip 打 tag `cloud-spring-final` 留档，不再作为发布线。不在 Spring `cloud` 上 `git merge core`。
- **登录**：保留微信开放平台扫码、钉钉 OAuth（授权 URL、登录、绑定已有账号、注册并绑定、解绑）；把短信登录做成完整闭环（发验证码、校验、签发与账密相同的登录令牌、登录页正式入口）；微信小程序登录沿用 `core` 已有接口，并保留 cloud 上「退出时 openid 绑定」的既有行为。
- **存储**：cloud 部署的唯一文件存储是华为 OBS；cloud profile 不回退本地磁盘。
- **项目同步**：保留云端入站（`c_project_push`、同步列表、按成员映射入库）；私有端出站默认地址改为云站新入口。补齐仓库里缺失的 `c_auth_user`、`c_project_push` Flyway，并对照生产库差分。
- **入口**：**BREAKING（相对旧 cloud 路径）** 对外站点为 `https://cloud.cat2bug.com`。UI 在根路径；API 与 UI 同域反代，前缀 `/prod-api`（nginx 剥前缀后转到后端根路径）；WebSocket 为 `/ws/`。OAuth 回调为 `https://cloud.cat2bug.com/three-login`。不再使用 `/cloud`、`/cloud/cloud-api` 作为应用路径；若线上仍有旧前缀，只在网关做短期 301。
- **密钥**：钉钉、微信、华为短信与 OBS 的密钥移出仓库配置，改为环境变量。不把现有 yml 明文密钥带进新配置。
- 不恢复 Spring 模块，不推翻 ADR-010。IM 渠道推送（钉钉/飞书/企业微信/邮件）与 MCP/OAuth 授权服务器保持 `core` 现状，本变更不改其需求。

## Capabilities

### New Capabilities

- `cloud-social-login`: 微信开放平台与钉钉扫码登录、账号绑定/解绑，以及微信小程序 openid 绑定在退出时的保留行为
- `cloud-sms-login`: 华为短信验证码的发送、校验与登录发令牌，以及登录页短信入口
- `cloud-obs-storage`: cloud 部署下上传、下载与资源 URL 只走华为 OBS
- `cloud-project-sync`: 云端接收并同步私有部署推送包；出站默认指向 `https://cloud.cat2bug.com/prod-api`
- `cloud-public-host`: 云站域名、同域 `/prod-api` 反代、根路径 History 路由与 OAuth 回调

### Modified Capabilities

- （无）现有规格已要求 `publicPath` 为 `/`、产品 API 前缀为 `/prod-api`。本变更把云站对齐这些约定，不改开源线需求。

## Impact

- 分支：`cloud` tip 换成 Quarkus；Spring 云站以 tag 留档
- 后端：在 Quarkus admin/framework 增加 cloud profile 下的三方登录、短信登录、OBS 文件服务、入站同步；`PublicPaths` 放行登录回调与发码接口；Flyway 补云表
- 前端：登录页增加钉钉、微信、短信入口；`/three-login`、`/three-bind`；`.env.cloud` 使用根路径与 `/prod-api`（与现工作区 `.env.cloud` 一致）
- 部署：`cloud.cat2bug.com` 的 nginx 与 `deploy/docker/nginx.conf` 同形（`/`、`/prod-api/`、`/ws/`、OAuth/MCP 原路径）；密钥用环境变量
- 依赖：钉钉 OAuth、微信开放平台、华为短信、华为 OBS SDK 需在 Quarkus（及后续 Native，若云站打 Native）下可用；短信 SDK 不得再以 `system` scope 本地 jar 作为唯一来源
- 数据：生产已有 `c_auth_user`、`c_project_push` 时，迁移必须可重复执行且不丢绑定与推送记录
