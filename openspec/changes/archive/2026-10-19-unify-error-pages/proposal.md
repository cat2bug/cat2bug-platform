## Why

全站 404 / 401 / 团队禁用 / 项目禁用页仍是旧版 RuoYi 风格（云朵动画 + 横排大图），与登录页狸花猫品牌、浅/深色主题不一致。列表空态已统一为 `Cat2BugEmpty`，错误态需要独立整页壳与定稿插画，避免再套空箱子。

## What Changes

- 新增整页错误态壳组件 `ErrorStateShell`（居中竖排：插画 → 大状态码 → 标题 → 说明 → 操作区）。
- 用定稿狸花猫插画替换 404 / 401 / team-lock / project-lock 配图；浅色与暗色各一套，随 `themeMode` 切换。
- 去掉云朵浮动与旧横排布局；**不**复用 `Cat2BugEmpty`（错误页不做箱子浮移动效）。
- 404 / 401 文案走 i18n；团队/项目禁用保留现有业务操作（切换团队 / 选择项目）。
- 移除或停用旧 `404_images` / `401_images` / `lock_flag` 在错误页上的引用。

## Capabilities

### New Capabilities

- `error-state-pages`: 统一错误/禁用整页的布局、插画与主题切换约定。

### Modified Capabilities

- （无）不改现有 capability 的需求条文；本变更为前端呈现层。

## Impact

- 前端：`cat2bug-platform-ui/src/views/error/*`、`components/ErrorStateShell`、`assets/images/error/*`、i18n。
- 长期文档：`docs/04-UI规范/组件与交互约定.md` 增补「错误整页」与空态的区分。
- 不改后端 API、路由权限判定逻辑。
