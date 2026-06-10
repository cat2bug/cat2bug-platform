## 1. 仓库与工程脚手架

- [x] 1.1 在 `cat2bug/cat2bug-chrome` 初始化 git 仓库（worktree 规范）与 README（中文 UTF-8）
- [x] 1.2 搭建 Vite + Vue 3 + TypeScript + Manifest V3（Vite 多入口构建）
- [x] 1.3 配置 `manifest.json`：`storage`、`activeTab`、`optional_host_permissions`（`http(s)://*/*`）
- [x] 1.4 定义构建产物目录与本地加载说明（`chrome://extensions` 开发者模式）

## 2. HTTP 客户端与存储

- [x] 2.1 实现 `api/client.ts`：`baseUrl` 拼接、`Authorization: Bearer`、默认 `language: zh_CN`
- [x] 2.2 POST 请求默认关闭重复提交拦截（对齐 Web `repeatSubmit: false`）
- [x] 2.3 `chrome.storage.local` 封装：`baseUrl`、`token`、`user`、`currentTeamId`、`currentProjectId`
- [x] 2.4 统一 401 处理：清 token、跳转登录态

## 3. Options 页

- [x] 3.1 UI：服务器地址输入、保存、`chrome.permissions.request` 授权
- [x] 3.2 「测试连接」：未登录时对 `GET /version` 或轻量端点探测；已登录时对 `GET /getInfo`
- [x] 3.3 说明文案：baseUrl 示例（直连后端 vs `/prod-api` 前缀）

## 4. 登录与验证码

- [x] 4.1 登录页：`POST /login` + 保存 `token`
- [x] 4.2 登录后 `GET /getInfo` 缓存用户与 `permissions`
- [x] 4.3 当 `GET /captchaImage` 返回启用时，展示验证码 UI（对齐 Web 像素验证码或图片验证码）
- [x] 4.4 `POST /logout` + 本地清会话

## 5. 团队与项目上下文

- [x] 5.1 `GET /system/team/my` 展示团队列表与切换
- [x] 5.2 切换团队：`PUT /system/user-config`（`currentTeamId`，`currentProjectId: 0`）
- [x] 5.3 `GET /system/project/list?teamId=` 展示项目列表
- [x] 5.4 切换项目：`PUT /system/user-config`（`currentProjectId`）+ `GET /getInfo`
- [x] 5.5 选项目后校验 `system:defect:list` / `system:defect:add`；无权限时提示并阻断

## 6. 缺陷列表与新建

- [x] 6.1 列表：`GET /system/defect/list?projectId=`，默认筛「我负责」
- [x] 6.2 新建表单：名称、类型、等级、描述、处理人（默认当前用户）
- [x] 6.3 `GET /system/project/{projectId}/member` 填充处理人下拉
- [x] 6.4 `POST /system/defect`，Body 含 `srcHost`；预填当前标签页 URL / 标题
- [x] 6.5 （可选）「在平台打开」链接（前端路由与 `baseUrl` 对齐）

## 7. 截图与标注

- [x] 7.1 集成 `js-web-screen-shot`（参考 `browser/index.vue` 配置）
- [x] 7.2 `completeCallback` → `POST /common/upload/screen-shot`（`fileBody`、`srcUrl`）
- [x] 7.3 上传成功后打开新建缺陷并附带 `imgUrls` / 图片字段
- [x] 7.4 验证标注工具：矩形、圆、箭头、文字、马赛克可用（依赖 `js-web-screen-shot` 内置工具栏）
- [x] 7.5 Fallback：`captureVisibleTab` + 扩展内标注页（当 WebRTC 截屏失败时）

## 8. Popup 主界面

- [x] 8.1 未登录：登录表单
- [x] 8.2 已登录：顶栏显示用户、团队 / 项目选择器
- [x] 8.3 缺陷列表 Tab + 「记缺陷」「截图」入口
- [x] 8.4 响应式布局适配 Popup 尺寸（或评估 Side Panel follow-up）

## 9. 测试与文档

- [x] 9.1 单元测试：HTTP 客户端、storage、权限校验逻辑
- [ ] 9.2 自托管 H2/MySQL 实例手工验收清单（登录、切团队、列表、新建、截图）
- [x] 9.3 `cat2bug-chrome/README.md`：安装、配置 baseUrl、权限说明、与 Open API 的区别
- [ ] 9.4 （可选）`readme/production/advanced/chrome-extension.md` 链到扩展仓库说明

## 10. Chrome Web Store 发布

- [x] 10.1 隐私政策页面（凭证本地存储、仅连用户配置服务器）
- [ ] 10.2 商店截图与单一用途描述
- [x] 10.3 打包 zip 并提交审核（`npm run zip` → `cat2bug-chrome.zip`）

## 验收（整体）

- [x] A.1 `cat2bug-platform` 代码无改动即可完成功能联调
- [x] A.2 扩展不使用 `/api/**` 与 API Key
- [ ] A.3 自托管用户仅配置 baseUrl + 账号即可使用（待手工验收）
- [ ] A.4 截图标注后成功附图创建缺陷（待手工验收）
