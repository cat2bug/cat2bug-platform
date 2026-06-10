## ADDED Requirements

### Requirement: 独立 Chrome 扩展仓库

系统 SHALL 在 `cat2bug-platform` 同级目录提供 `cat2bug-chrome` 项目，作为 Chrome Extension Manifest V3 应用，目标发布至 Chrome Web Store。

扩展 MUST 不修改 `cat2bug-platform` 后端或前端业务代码即可完成功能联调。

#### Scenario: 仓库位置与构建

- **WHEN** 开发者克隆 Cat2Bug 生态仓库
- **THEN** MUST 能在 `cat2bug/cat2bug-chrome` 执行依赖安装与扩展构建（如 `npm run build`），产出可加载的 MV3 包

### Requirement: 自托管服务器地址配置

扩展 MUST 提供 Options 页面，允许用户配置 Cat2Bug 实例的 API 根地址 `baseUrl`（不含末尾斜杠）。

保存 `baseUrl` 时，扩展 MUST 通过 `optional_host_permissions` 请求访问该源（`{baseUrl}/*`）。

Options MUST 提供「测试连接」能力，向已配置 `baseUrl` 发起连通性请求并在 UI 反馈成功或失败。

#### Scenario: 用户配置自托管地址

- **WHEN** 用户在 Options 输入 `https://bugs.company.com/prod-api` 并保存
- **THEN** 扩展 MUST 持久化该地址并在后续 API 请求中使用 `{baseUrl}/login` 等形式拼接路径

#### Scenario: 未授权 host 时拒绝请求

- **WHEN** 用户未授予 `baseUrl` 对应 host 权限即发起 API 请求
- **THEN** 扩展 MUST 提示用户返回 Options 完成授权或修正地址

### Requirement: 与 Vue 前端一致的登录鉴权

扩展 MUST 使用主站 JWT 登录，不得使用 Open API（`/api/**`）或 `CAT2BUG-API-KEY`。

登录 MUST 调用 `POST {baseUrl}/login`，请求体包含 `username`、`password`；当服务端启用登录验证码时 MUST 支持 `code`、`uuid`（通过 `GET {baseUrl}/captchaImage` 获取）。

登录成功后扩展 MUST 保存返回的 `token`，并在后续请求头设置 `Authorization: Bearer <token>`。

扩展 MUST 在登录后调用 `GET {baseUrl}/getInfo` 获取用户信息与 `permissions`。

对 `POST` 类请求（含登录、缺陷创建、截图上传），扩展 MUST 避免触发平台 1 秒内重复提交拦截（等价于 Web 端 `repeatSubmit: false`）。

#### Scenario: 成功登录

- **WHEN** 用户提供有效账号密码且验证码（若启用）正确
- **THEN** 扩展 MUST 保存 token 并进入团队 / 项目选择或主界面

#### Scenario: Token 失效

- **WHEN** 任意 API 返回未认证（如 HTTP 401 或业务 code 表示 token 过期）
- **THEN** 扩展 MUST 清除本地 token 并引导用户重新登录

### Requirement: 团队与项目切换

扩展 MUST 在登录后调用 `GET {baseUrl}/system/team/my` 展示用户可访问的团队列表。

用户切换团队时，扩展 MUST 调用 `PUT {baseUrl}/system/user-config`，设置 `currentTeamId` 并将 `currentProjectId` 置为 `0`（与 Web 行为一致）。

扩展 MUST 调用 `GET {baseUrl}/system/project/list?teamId=<teamId>` 获取该团队下用户参与的项目列表。

用户选择项目时，扩展 MUST 调用 `PUT {baseUrl}/system/user-config` 设置 `currentProjectId`，并调用 `GET {baseUrl}/getInfo` 刷新权限。

扩展 MUST NOT 依赖后端新增「按缺陷权限过滤项目列表」接口；项目列表为 `project/list` 全量结果。

#### Scenario: 切换团队后项目列表更新

- **WHEN** 用户在 UI 选择另一团队
- **THEN** 扩展 MUST 更新 `user-config` 并展示新团队下的项目列表

#### Scenario: 项目无缺陷权限

- **WHEN** 用户选择某项目后 `getInfo.permissions` 不包含 `system:defect:list` 且不包含 `system:defect:add`
- **THEN** 扩展 MUST 提示无缺陷权限并 MUST NOT 展示缺陷列表或新建入口

### Requirement: 缺陷列表与新建

在已选项目且具备 `system:defect:list` 权限时，扩展 MUST 调用 `GET {baseUrl}/system/defect/list`，查询参数 MUST 包含 `projectId`。

列表默认 SHOULD 筛选与当前用户相关的缺陷（如处理人包含当前用户），具体参数与 Web 缺陷列表对齐。

在具备 `system:defect:add` 权限时，扩展 MUST 支持新建缺陷，调用 `POST {baseUrl}/system/defect`。

新建请求 Body MUST 包含 `srcHost` 字段；SHOULD 预填当前浏览器标签页的 URL 与标题到缺陷名称或描述。

扩展 SHOULD 支持从列表跳转或在平台打开缺陷详情（使用可配置的前端根地址与路由）。

#### Scenario: 查看缺陷列表

- **WHEN** 用户已选项目且有 `system:defect:list`
- **THEN** 扩展 MUST 展示该项目下的缺陷列表（支持分页或「加载更多」）

#### Scenario: 新建缺陷

- **WHEN** 用户提交新建表单且字段合法
- **THEN** 扩展 MUST 调用 `POST /system/defect` 并在成功后刷新列表或提示成功

### Requirement: 微信式截图标注与上传

扩展 MUST 提供截图入口，支持区域选择（非仅全屏硬截）。

截图完成后用户 MUST 能在截图上使用标注工具，至少包括：矩形、圆形、箭头、文字、马赛克涂抹。

标注完成后的图像 MUST 通过 `POST {baseUrl}/common/upload/screen-shot` 上传，请求体 MUST 包含 `fileBody`（base64）及 `srcUrl`（被测页面 URL），与 Web `tool/project/browser` 行为一致。

上传成功后扩展 MUST 将返回的图片路径用于新建缺陷（附图创建），行为对齐 Web `AddDefect.open({ imgUrls })`。

实现 SHOULD 优先使用 `js-web-screen-shot`；若运行环境不支持，MAY 降级为 `chrome.tabs.captureVisibleTab` 加扩展内标注页。

#### Scenario: 截图并创建缺陷

- **WHEN** 用户在浏览被测页面时触发截图并完成标注
- **THEN** 扩展 MUST 上传图片并打开新建缺陷流程，且图片已关联到缺陷

### Requirement: 登出

扩展 MUST 支持登出：调用 `POST {baseUrl}/logout`（若平台要求），并清除本地 `token`、团队 / 项目缓存。

#### Scenario: 用户登出

- **WHEN** 用户点击登出
- **THEN** 扩展 MUST 清除会话状态并返回登录界面

### Requirement: Chrome Web Store 发布就绪

扩展 MUST 提供商店发布所需的最小材料：清晰单一用途描述、权限说明、隐私政策（说明凭证本地存储、仅与用户配置的 Cat2Bug 服务器通信）。

Manifest 权限 MUST 遵循最小化原则；网络访问 SHOULD 以 `optional_host_permissions` 为主，避免不必要的 `<all_urls>` 常驻权限。

#### Scenario: 权限声明可审计

- **WHEN** 审核员或用户查看扩展权限说明
- **THEN** MUST 能对应到实际功能（存储、当前标签页截图、用户配置的服务器域名）
