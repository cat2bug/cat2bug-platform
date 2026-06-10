## Context

Cat2Bug 主站采用 Spring Security JWT（`Authorization: Bearer`）+ `/system/**` 业务接口。Vue 前端通过 `src/utils/request.js` 统一注入 Token、`language` 头，项目上下文由 `sys_user_config` 的 `currentTeamId` / `currentProjectId` 驱动。

平台已存在内嵌浏览器测试工具（`views/tool/project/browser`），使用 `js-web-screen-shot` 截屏并调用 `POST /common/upload/screen-shot`，成功后打开 `AddDefect` 表单。

Chrome 扩展作为**轻量客户端**，复用上述契约，不引入第二套 Open API。

```
┌──────────────────┐     Bearer JWT      ┌─────────────────────────┐
│  cat2bug-chrome  │ ──────────────────► │  cat2bug-platform       │
│  (任意被测页面)   │     /system/**       │  主站 Security 链       │
│                  │     /common/**       │  (无代码变更)           │
└──────────────────┘                       └─────────────────────────┘
```

## Goals / Non-Goals

**Goals:**

- 测试员在任意标签页通过扩展登录自托管 Cat2Bug，切换团队 / 项目后查看与自己相关的缺陷并快速新建
- 截图支持区域选择及标注（矩形、圆、箭头、文字、马赛克），上传后附图创建缺陷
- 请求行为与 Vue `request.js` 对齐（Token、防重复提交、`srcHost`）
- 满足 Chrome Web Store 公开发布的基本权限与隐私说明

**Non-Goals:**

- 修改 `cat2bug-platform` 后端或前端
- 使用 `/api/**` 或 `CAT2BUG-API-KEY`
- 在平台项目设置中增加扩展配置
- MVP 覆盖缺陷全生命周期（审核、指派、Excel 模式等）
- 云端 `C2B_C0/C1/C2` 签名头（自托管场景）

## Decisions

### 1. 仓库与目录

**选择**：`cat2bug/cat2bug-chrome/`，与 `cat2bug-platform` 同级，独立 git 仓库（worktree 规范与平台一致）。

**理由**：扩展生命周期、商店发布、MV3 构建链与主平台解耦。

### 2. 技术栈

**选择**：Vite + Vue 3（或 Vue 2.7）+ TypeScript；Manifest V3。

| 模块 | 选择 |
|------|------|
| Popup / Options UI | Vue 单文件组件 |
| Background | Service Worker（`service_worker`） |
| HTTP | `fetch` 封装，对齐 `request.js` 行为 |
| 截图标注 | `js-web-screen-shot`（与 `browser/index.vue` 一致） |
| 存储 | `chrome.storage.local`（token、baseUrl、当前团队/项目缓存） |

**理由**：与平台 UI 技术栈接近；截图库已在平台验证。

### 3. API 根地址（baseUrl）

用户于 Options 填写**后端 API 根**（不含末尾 `/`），示例：

| 部署 | baseUrl 示例 |
|------|-------------|
| 开发（直连后端） | `http://localhost:2020` |
| 反向代理 | `https://bugs.company.com/prod-api` |

扩展保存 baseUrl 后调用 `chrome.permissions.request({ origins: [baseUrl + '/*'] })`（`optional_host_permissions`）。

提供「测试连接」：对 `GET {baseUrl}/getInfo` 或 `GET {baseUrl}/version` 发请求（登录前可测连通性）。

### 4. 鉴权与 HTTP 客户端

对齐 `cat2bug-platform-ui/src/utils/request.js`：

```text
POST {baseUrl}/login          Body: { username, password, code?, uuid? }
                              Headers: isToken: false（实现上等价：不带 Authorization）

GET  {baseUrl}/getInfo
PUT  {baseUrl}/system/user-config
...  其他 /system/**、/common/**

通用请求头:
  Authorization: Bearer <token>
  language: zh_CN（或用户可选）
  Content-Type: application/json（上传除外）

POST 防重复提交:
  对 login、defect 创建、截图上传等设置 repeatSubmit: false（避免 1s 内同 body 被拒）
```

**验证码**：若服务端 `sys.account.captchaEnabled=true`，扩展 MUST 支持 `GET /captchaImage` 并在登录表单展示（与 Web 登录页一致）。默认种子库为 `false`。

**Token 过期**：响应 `401` 时清除本地 token，引导重新登录。

### 5. 团队与项目上下文

与 Web `TeamSelect` / `ProjectSelect` 相同序列：

```text
1. GET /system/team/my                    → 团队列表
2. PUT /system/user-config
     { currentTeamId, currentProjectId: 0 }   → 切换团队
3. GET /system/project/list?teamId=<id>  → 项目列表
4. PUT /system/user-config
     { currentProjectId }                     → 切换项目
5. GET /getInfo                           → 刷新 permissions / config
```

缺陷相关请求 MUST 携带查询参数 `projectId`（与 `defect/index.vue` 一致），不依赖仅后端 config 隐式推断。

**缺陷权限（无后端改造）**：用户选择项目后执行步骤 4–5；若 `permissions` 不含 `system:defect:list` 或 `system:defect:add`，UI 提示「该项目无缺陷权限」且不进入缺陷功能。不在登录时批量扫描全部项目。

### 6. 缺陷功能（MVP）

| 功能 | 接口 | 说明 |
|------|------|------|
| 列表 | `GET /system/defect/list?projectId=&...` | 默认筛选「处理人包含当前用户」 |
| 详情 | `GET /system/defect/{defectId}` | 可选：跳转平台 URL |
| 新建 | `POST /system/defect` | Body 含 `srcHost`（被测页 `https://...` 或平台 host） |
| 成员 | `GET /system/project/{projectId}/member` | 处理人下拉；默认当前用户 |

新建表单预填：当前标签页 `title`、`url` 写入描述；截图后附带 `imgUrls` / 图片字段（对齐 `AddDefect.open({ imgUrls })` 所用字段）。

在平台打开缺陷：`{frontBaseUrl}/#/...`（frontBaseUrl 可与 baseUrl 相同或 Options 单独配置前端入口，实施时以实际路由为准）。

### 7. 截图与标注

**选择**：复用 `js-web-screen-shot` 配置（参考 `browser/index.vue`）：

- `clickCutFullScreen: true`（区域选择）
- `enableWebRtc` / `loadCrossImg` 按扩展环境调优
- `completeCallback` 获得 base64 → `POST /common/upload/screen-shot`

上传 Body 对齐 Web：

```json
{
  "srcUrl": "<当前标签页 URL>",
  "fileBody": "<base64>"
}
```

标注能力（矩形、圆、箭头、文字、马赛克）由 `js-web-screen-shot` 内置工具栏提供；若 MV3 限制导致 WebRTC 截屏不可用，降级为 `chrome.tabs.captureVisibleTab` + 扩展内独立标注页（Fabric/Konva）作为 fallback（tasks 中分阶段）。

### 8. Chrome 扩展结构

```text
manifest.json
├── options.html          # baseUrl、测试连接
├── popup.html            # 登录 / 主界面
├── annotate.html         # （可选）截图后标注全屏页
├── background.js         # token 刷新、消息路由
├── content.js            # （可选）仅当需要页面内能力时
└── assets/
```

**权限（MVP）**：

- `storage`
- `activeTab`（截图）
- `optional_host_permissions`: `https://*/*`, `http://*/*`（由用户 baseUrl 收窄）

**Web Store**：单独隐私政策说明（凭证仅存本地、仅连接用户配置的服务器）。

### 9. 与 Open API 的关系

JUnit / CI 继续使用 `CAT2BUG-API-KEY` + `/api/**`。扩展**不调用**该路径，文档中区分「浏览器扩展客户端」与「Open API 集成」。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| baseUrl 与前端路由不一致 | Options 说明 + 测试连接；文档写清 `/prod-api` 场景 |
| 验证码开启增加登录复杂度 | 实现 captchaImage；多数部署默认关闭 |
| `js-web-screen-shot` 在部分站点失效 | fallback `captureVisibleTab` |
| 跨域仅扩展上下文可请求 | `optional_host_permissions` 必须在请求前授予 |
| 项目无缺陷权限 | 选项目后 `getInfo` 懒校验 + 明确提示 |
| Token 与 Web 同浏览器多端登录 | 接受；扩展独立 storage |

## Migration Plan

1. 初始化 `cat2bug-chrome` 仓库与 MV3 脚手架
2. 实现 HTTP 层 + 登录 + 团队 / 项目切换
3. 缺陷列表 / 新建 + 截图上传
4. 内测打包 + 自托管联调
5. 准备商店素材与隐私政策 → 提交 Chrome Web Store

**回滚**：卸载扩展即可；平台无变更。

## Open Questions

- Options 是否区分 `apiBaseUrl` 与 `frontBaseUrl`（API 与 SPA 不同源时）？MVP 可共用一个字段，文档说明例外
- Popup vs Side Panel：MVP 用 Popup；Side Panel 作为体验增强 follow-up
