## Why

测试人员在浏览被测 Web 应用时需要边测边记录缺陷；切回 Cat2Bug 平台再新建缺陷效率低。现有平台内「浏览器测试工具」（`tool/project/browser`）依赖平台内嵌浏览器，无法在任意站点上使用。

需要在 `cat2bug-platform` 同级新建 **cat2bug-chrome** Chrome 扩展：连接自托管 Cat2Bug 实例，用与 Vue 前端相同的登录与接口记录、查看缺陷，并支持微信式截图标注后提交。

## What Changes

### 新建仓库 `cat2bug-chrome`（与 `cat2bug-platform` 同级）

- Chrome Extension Manifest V3
- Options：配置自托管 `baseUrl`（API 根地址）
- 登录：账号密码 → JWT，与 Web 一致（`POST /login`、`Authorization: Bearer`）
- 团队 / 项目切换：`/system/team/my`、`/system/user-config`、`/system/project/list`
- 缺陷列表与新建：`/system/defect/list`、`POST /system/defect`
- 截图标注：`js-web-screen-shot`（对齐 `tool/project/browser`）+ `POST /common/upload/screen-shot`
- 发布目标：Chrome Web Store 公开

### `cat2bug-platform` 本变更范围内

- **不修改** 后端、前端业务代码、Open API（`/api/**`）
- **不新增** 平台内「Chrome 扩展」设置页或 API Key 流程
- 在 `readme/production` 可选补充扩展安装说明（follow-up，非 MVP 阻塞）

## Capabilities

### New Capabilities

- `cat2bug-chrome-extension`：Chrome 扩展功能、鉴权、团队/项目上下文、缺陷 CRUD、截图标注、自托管与商店发布的验收标准

### Modified Capabilities

- （无）平台既有能力规格不变

## Impact

- **新仓库**：`../cat2bug-chrome/`（独立 git 仓库 / worktree）
- **平台代码**：无变更
- **接口依赖**：现有主站 REST（`/login`、`/getInfo`、`/system/**`、`/common/**`），非 Open API
- **参考实现**：`cat2bug-platform-ui/src/views/tool/project/browser/index.vue`、`src/utils/request.js`

## 产品决策（探索阶段已确认）

1. **用户**：测试人员，边浏览被测站点边记缺陷
2. **部署**：自托管；用户配置 `baseUrl`；`optional_host_permissions` 动态申请域名
3. **认证**：与 Vue 相同 JWT，**不使用** API Key、`/api/**`
4. **团队 / 项目**：登录后可切换团队；项目列表来自 `project/list`；**不改造**后端缺陷权限过滤，扩展侧在用户选项目时懒校验 `getInfo.permissions`
5. **截图**：框选 + 矩形 / 圆 / 箭头 / 文字 / 马赛克，参考微信截图与平台 `js-web-screen-shot`
6. **平台设置**：无需在 Cat2Bug 项目设置中增加扩展配置项
7. **发布**：Chrome Web Store 公开

## 非目标（Non-Goals）

- 不实现 Open API（`/api/**`）客户端或临时 API Key
- 不修改 `cat2bug-platform` 鉴权、项目列表过滤、登录接口
- 不替代平台内嵌浏览器测试工具（两者可并存）
- MVP 不做缺陷编辑全流程（指派 / 修复 / 关闭等可后续迭代）
