# member / notice 未进路径键保留字导致键盘设置 404

- 日期：2026-10-19
- 涉及：前端 / 公共校验

## 现象

顶栏头像菜单点「键盘设置」进入 `/member/keyboard`，整页 404。个人中心 `/member/profile` 正常。

## 根因

项目命名空间守卫 `ensureProjectScopedContext` 会用 `parseProjectScopedPath` 解析任意形如 `/a/b` 的路径。`member` 不在 `PATH_KEY_RESERVED` / `NON_PROJECT_MENU_ROOTS` 里，于是 `/member/keyboard` 被当成 `teamKey=member`、`projectKey=keyboard` 去调 `getProjectByPathKeys`，查不到 → 守卫返回 `not_found` → 跳 `/404`。

`/member/profile` 能开是因为第二段 `profile` 已在保留字里，解析直接失败并 `skip`。

另：守卫原先「先 parse、再看是否项目路由」，非命名空间页只要长得像 `/a/b` 就会误伤。

## 解决办法

1. 前端 `PATH_KEY_RESERVED`、`NON_PROJECT_MENU_ROOTS` 与后端 `PathKeyValidator` 补上 `member`、`notice`。
2. `ensureProjectScopedContext` 先判断 `meta.projectScoped`，非命名空间直接 `skip`。

## 以后怎么避免

- 新增 SPA 第一路径段常量路由时，同步进前后端路径键保留字（见 `path-key.js` / `PathKeyValidator`）。
- 项目守卫只处理真正匹配了 `projectScoped` 的路由，不要仅凭 path 形状推断。

相关：[[权限模型与鉴权链路]]
