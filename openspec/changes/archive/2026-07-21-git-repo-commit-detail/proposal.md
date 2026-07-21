## Why

仓库浏览已有提交列表（`GET .../commits`）与「提交记录」Tab，但只能看短消息，无法进入单次提交详情。用户需要 Gitee/GitHub 风格的提交页：全文说明、父提交、文件变更统计与行级 diff，并从提交列表、代码页最新提交条、侧栏近期动态一键进入。

## What Changes

- 新增只读 **提交详情** API：按 commit id 返回全文消息、作者、时间、parents，以及变更文件列表与（受限制的）unified patch。
- 新增前端 **提交详情页**（独立全宽，挂在仓库 browse 路由下）：展示 meta、「浏览文件」、文件变更摘要与可展开的行级 diff。
- **三处入口可点跳转**：提交记录 Tab 的提交消息、代码页最新提交条消息、右侧栏近期动态的消息/hash。
- 大文件与二进制：不返回完整 patch，以占位说明（截断/跳过）；文本 patch 有单文件与总量上限。
- 不新增 capability：行为归入既有 `git-repo-browse`（与 branches/tree/blob/commits 同一只读浏览线）。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `git-repo-browse`: 增加单次提交详情 API、diff 限制策略、详情页 UI，以及从列表/最新提交/侧栏动态进入详情的导航

## Impact

- 后端：`ISysGitBrowseService` / `SysGitBrowseServiceImpl` / `SysGitBrowseController` 扩展；JGit `DiffFormatter` / tree walk；新 DTO（commit detail、file change、patch 元数据）
- 前端：`browse.vue` 入口可点；新路由（如 `.../commits/:commitId`）与详情视图；`gitRepo.js`；i18n；diff 渲染（可自研轻量 unified 视图）
- 权限：复用 pull（与现有 browse 一致）；无 manage 专用写操作
- 依赖：现有 JGit bare 仓；无新 Flyway 表
