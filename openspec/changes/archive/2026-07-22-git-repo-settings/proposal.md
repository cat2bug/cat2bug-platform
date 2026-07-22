## Why

仓库设置今天只存在于项目仓库列表的对话框里，进入代码浏览壳后无法就地管理基本信息或删除仓库。需要在壳内增加「管理」Tab 与嵌套 settings 路由，把已有能力收拢到与码云类似的管理入口，并为后续仓级成员 ACL 预留侧栏位置。

## What Changes

- 在 `GitRepoShell` 增加「管理」Tab（仅对当前生效的 manage 权限用户可见）
- 嵌套路由 `/code/{slug}/settings/...`（与现有 browse / pulls / releases 同壳）
- Settings 布局：左侧导航 + 右侧内容
  - **基本信息**：迁入现有编辑字段（名称、slug/路径展示、介绍、可见性、默认分支等已支持字段）
  - **删除仓库**：迁入现有删除确认流程
  - **仓库成员管理**：侧栏项灰显不可点（占位，本 change 不实现 ACL）
- 项目仓库列表上的「设置」入口改为进入壳内 settings（避免两套编辑 UI 长期并存）
- **不在本 change 实现**仓级 ACL、保护分支、WebHook、镜像等；ACL 产品决议写入 design，供后续 `git-repo-members` change 使用

## Capabilities

### New Capabilities

- `git-repo-settings`: 仓库壳内管理 Tab、嵌套 settings 路由、基本信息与删除页、成员菜单灰显占位

### Modified Capabilities

- `project-git-repository`: 明确仓库元数据编辑与删除的**首选 UI 入口**为壳内 settings（权限矩阵本 change 仍按现有项目角色 manage；不引入仓级 ACL 行为变更）

## Impact

- 前端：`GitRepoShell.vue`、git 仓库路由/守卫、`browse`/`index` 设置入口、新 settings 页面与 i18n
- 后端：原则上复用现有 update / delete API；若缺字段再小幅扩展，不新增 ACL 表
- 权限：本 change 仍用 `GitRepositoryAccessService.canManage(user, projectId)`；design 记录二期 effectivePerm 切换点
- 无 **BREAKING** API 变更；列表页设置对话框可移除或改为跳转
