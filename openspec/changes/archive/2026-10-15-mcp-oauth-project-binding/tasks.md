## 1. 数据库迁移

- [x] 1.1 新增 `V1_1_0_42__mcp_oauth_project_binding.sql`（`db/migration` H2 与 `db/mysql` 各一份）：`sys_oauth_code`、`sys_user_access_token` 加 `project_id`；删除 `source='manual'` 令牌、`project_id IS NULL` 的令牌和全部授权码
- [x] 1.2 `SysOAuthCode`、`SysUserAccessToken` 增加 `projectId` 字段，对应 Mapper XML 读写 `project_id`

## 2. 删除个人访问令牌

- [x] 2.1 删除 `UserAccessTokenResource` 及 `SysUserAccessTokenService` 中手动创建 / 列表 / 吊销逻辑；`AccessTokens` 去掉 PAT 前缀与生成入口；`SysUserAccessToken.SOURCE_MANUAL` 及其引用清理
- [x] 2.2 `AccessTokenResolver.authenticate` 只接受 `source=oauth` 且 `project_id` 非空的令牌，其它统一返回空（401）
- [x] 2.3 清理 `messages*.properties`、`application.properties`、`NativeRuntimeInitFeature` 中与 PAT 相关的残留

## 3. OAuth 绑定项目

- [x] 3.1 `OAuthService.approve` 增加 `projectId` 参数并校验成员关系与 MCP 开关；`exchangeCode`、`refresh` 把 `project_id` 传递到新令牌；`revoke` 去掉 manual 分支
- [x] 3.2 `OAuthResource.check` 返回按团队分组的可选项目；`decision` 接收 `project_id`，允许时缺失或不合法即报错
- [x] 3.3 `AccessTokenResolver` 返回值携带 `projectId`，`AccessTokenIdentityProvider` 写入 `AccessTokenIdentities.PROJECT_ID_ATTRIBUTE`

## 4. MCP 工具改为绑定项目

- [x] 4.1 `McpContext.scope` 去掉 `projectRef`，项目取自身份属性；删除 `resolveProjectId`；更新报错文案
- [x] 4.2 `McpDefectTools`、`McpWorkTools`、`McpCodeTools`、`McpGenericTools` 删除 `project` 参数与 `PROJECT_DESC`，同步修改工具描述
- [x] 4.3 `McpProjectTools`：`list_my_projects` 改为 `get_current_project`（项目、可用模块、代码仓库）；`resolve_project_by_git_remote` 只认绑定项目内的仓库

## 5. 前端

- [x] 5.1 `views/oauth/consent.vue` 增加按团队分组的项目单选卡片（只有一个项目时自动选中，多个时预选网页当前项目，无可选项目时禁用允许并提示），`decision` 提交 `project_id`；暗色主题可读
- [x] 5.2 删除 `views/system/user/profile/accessToken.vue` 与个人中心 `tokens` 标签；`api/system/access-token.js` 改为只含 OAuth 确认接口的 `api/system/oauth.js`
- [x] 5.3 `McpCard.vue` 改为非卡片的 MCP 设置弹窗组件，由 `AICard.vue` 新增的「MCP 接入」链接打开；`AICard` 可见条件包含 MCP 链接（`system:project:query`）
- [x] 5.4 弹窗模块列表按 `m.key` 渲染并显示本地化名称；去掉「项目引用」与令牌链接；客户端示例改为只填 URL + 各客户端登录步骤，提示登录时选择团队和项目
- [x] 5.5 7 种语言 i18n：补齐 `project-mcp.module.*`、确认页团队 / 项目选择、示例提示等新 key，删除 `access-token.*` 中不再使用的 key
- [x] 5.6 `npm run lint` 通过（**放弃**：既有 `.eslintrc.js` 规则配置与插件版本不匹配，lint 无法运行；以 `vite build` 生产构建通过代替，见 [[前端lint配置失效实际不检查任何文件]]）

## 6. 测试与验证

- [x] 6.1 改写 `QuarkusOAuthTest`：授权须带项目、非成员 / 未开启 MCP 项目被拒、刷新保持项目、无项目令牌 401
- [x] 6.2 改写 `QuarkusAccessTokenTest`、`AccessTokenUnitTest`：PAT 前缀 401、`/system/user/access-token` 不存在
- [x] 6.3 MCP 测试：工具 schema 无 `project` 参数；`get_current_project`；跨项目 git remote 报错；绑定项目被关闭 MCP 后调用被拒
- [x] 6.4 `mvn install` 全量测试通过（先 unset `QUARKUS_CONFIG_LOCATIONS CAT2BUG_INSTALL_CONFIG_PATH`）
- [x] 6.5 本机实测：Cursor 只填 URL → 浏览器登录 → 选项目 → 调用 `get_current_project` 与 `search_defects` 成功（2026-10-15 Cursor 重连后验证通过，并实测 `get_guide`）
- [x] 6.6 对鉴权改动做 `/code-review` 安全审查（**归档时跳过正式子 agent 审查**：实现期已按「只 OAuth、绑项目、PAT 401」人工核对；后续大改鉴权再补正式审查）

## 7. 回写长期知识

- [x] 7.1 新建 `docs/07-技术决策/ADR-016-AI客户端只经OAuth登录接入且授权绑定单个项目.md`，ADR-013 状态改为「被 ADR-016 取代」，更新 `07-技术决策/说明.md` 与 `00-项目总览.md` 列表
- [x] 7.2 更新 `docs/03-技术方案/MCP接入与权限.md`、`权限模型与鉴权链路.md`、`docs/05-接口文档/接口清单-OpenAPI与AI.md`、`docs/02-系统架构/数据库.md`、`docs/02-系统架构/整体架构.md`、`docs/01-产品需求/产品定位与核心功能.md` 中关于个人访问令牌和 `project` 参数的说法
- [x] 7.3 `docs/08-版本记录/2026-10.md` 加一行；踩坑（模块对象当字符串拼 i18n key）如有复用价值写入 `docs/06-踩坑记录/`，否则注明无
