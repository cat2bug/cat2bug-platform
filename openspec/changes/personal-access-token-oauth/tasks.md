## 1. 数据与配置

- [ ] 1.1 Flyway 脚本（H2 `db/migration` 与 MySQL `db/mysql` 各一份）：`sys_user_access_token`、`sys_oauth_client`、`sys_oauth_code`
- [ ] 1.2 Domain、Mapper 接口与 XML；令牌生成（`c2b_pat_` / `c2b_oat_` 前缀 + 随机数）、SHA-256 哈希、前缀截取工具类及单元测试
- [ ] 1.3 新增配置 `cat2bug.public-url`，实现对外地址解析（配置优先，其次 `X-Forwarded-*` / `Host`），补测试

## 2. 个人访问令牌

- [ ] 2.1 个人令牌接口（JWT 登录态）：创建（返回一次明文）、列表（只含本人）、吊销
- [ ] 2.2 `AccessTokenAuthFilter`：只匹配 MCP 路径，校验存在、未过期、未吊销、用户状态正常，构造 `LoginUser`，异步记录最后使用时间与 IP；`JwtAuthFilter` 不处理 MCP 路径
- [ ] 2.3 测试：令牌在 MCP 路径可用、在 `/system/**` 不可用；吊销、过期、停用用户后返回 401；库中无明文
- [ ] 2.4 前端个人中心新增「访问令牌」页签：列表（名称、前缀、来源、客户端、创建 / 过期 / 最后使用）、创建弹窗（只显示一次并可复制）、吊销确认；七种语言词条

## 3. OAuth 授权服务器

- [ ] 3.1 `/.well-known/oauth-authorization-server` 元数据；`PublicPaths` 放行 `/.well-known/**`、`/oauth/register`、`/oauth/authorize`、`/oauth/token`、`/oauth/revoke`
- [ ] 3.2 `/oauth/register` 动态客户端注册（只允许公开客户端，校验 redirect_uri），IP 限流
- [ ] 3.3 `/oauth/authorize` 参数校验与跳转确认页；`/oauth/authorize/decision`（需登录）生成授权码或返回拒绝
- [ ] 3.4 `/oauth/token`：授权码 + PKCE 换令牌、刷新令牌轮换、重放检测吊销整条链；IP 限流
- [ ] 3.5 `/oauth/revoke`；个人中心吊销 OAuth 条目时连带吊销刷新令牌
- [ ] 3.6 前端 `/oauth/consent` 确认页：未登录先登录再回到本页；显示客户端名、回调主机、权限说明；七种语言词条
- [ ] 3.7 测试：注册校验、PKCE 缺失 / 错误、授权码一次性与过期、redirect_uri 不匹配、拒绝授权、刷新轮换与重放、限流

## 4. 验收

- [ ] 4.1 全量测试通过；`/code-review` 安全审查 OAuth 与令牌相关代码
- [ ] 4.2 Native 重建与两种冒烟通过（新表在 H2 与 MySQL 都能迁移）
- [ ] 4.3 浏览器验证：生成令牌、吊销；用一个 OAuth 测试客户端（脚本模拟）走完授权码 + PKCE 全流程

## 5. 回写长期知识

- [ ] 5.1 `docs/03-技术方案/权限模型与鉴权链路.md` 新增「个人访问令牌与 OAuth」；`docs/05-接口文档/` 补令牌与 OAuth 接口；`docs/02-系统架构/部署.md` 补 `cat2bug.public-url` 与反向代理说明
- [ ] 5.2 新增 ADR「AI 客户端以个人身份接入：个人访问令牌 + 平台自带 OAuth，令牌只用于 MCP」，加入 ADR 说明与总览
- [ ] 5.3 可复用的坑写入 `docs/06-踩坑记录/`（没有则注明无）；`docs/08-版本记录/` 记录本变更；`check_links.py` 坏链为 0
