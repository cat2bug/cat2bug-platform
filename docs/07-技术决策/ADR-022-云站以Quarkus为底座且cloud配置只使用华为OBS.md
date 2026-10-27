# ADR-022 云站以 Quarkus 为底座且 cloud 配置只使用华为 OBS

- 日期：2026-10-27
- 状态：已采纳

## 背景

线上云站长期在 `cloud` 分支以 Spring Boot 运行，而产品默认后端已按 [[ADR-010-移除Spring后端并统一模块命名]] 切到 Quarkus（`core`）。两条线不能再硬合并。云站需就地换成 Quarkus，并保留相对开源线的能力：微信/钉钉扫码、短信登录、华为 OBS、私有端项目同步。

## 候选方案

### 方案 A：继续 Spring 云站与 Quarkus 开源双轨

优点：短期少动生产。

缺点：两套后端持续分叉，与 ADR-010 相反，云能力无法跟进 `core`。

### 方案 B：以当时 `core` 为父重建 `cloud` tip，云能力用 Maven/`%cloud` profile 叠在现有模块

优点：发布线只有 Quarkus；默认构建不拖入华为/JustAuth/钉钉 SDK；密钥只走环境变量。

缺点：需对照生产表补 Flyway；旧 `/cloud` 路径需网关或配置迁移。

## 最终决定

采用方案 B：

- `cloud` tip 以 Quarkus `core` 为父演进；Spring 云站 tip 以 tag `cloud-spring-final` 留档，不再作为发布线。
- 三方登录、短信、OBS、入站同步写在现有 `admin` / `framework` / `system`，由 profile `cloud` / `%cloud` 打开。
- **cloud 部署的唯一文件存储是华为 OBS**；未启用 cloud profile 时仍用本地 `cat2bug.profile` 目录。
- 对外站点：`https://cloud.cat2bug.com`；UI 在 `/`；API 同域 `/prod-api`（nginx 剥前缀）；WebSocket `/ws/`；OAuth 回调 `https://cloud.cat2bug.com/three-login`。不再以 `/cloud`、`/cloud/cloud-api` 为目标路径。
- 钉钉 / 微信 / OBS / 短信密钥只来自环境变量，不入仓库配置。

## 原因

与 ADR-010 对齐；profile 足以隔离商业依赖；同域 `/prod-api` 与开源线 nginx 约定一致，前端与私有端出站少改。

## 副作用

- 已安装私有端若仍写死旧 cloud 前缀，需改 `cat2bug.cloud.host`（默认现为 `https://cloud.cat2bug.com/prod-api`）或依赖网关短期 301。
- 云站本变更以 JVM `%cloud` 交付；Native 云镜像另议。
- 历史若曾提交明文密钥，上线须轮换。
