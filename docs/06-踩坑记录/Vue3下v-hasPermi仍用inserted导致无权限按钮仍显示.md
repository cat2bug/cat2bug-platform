# Vue3 下 v-hasPermi 仍用 inserted 导致无权限按钮仍显示

- 日期：2026-11-04
- 涉及：后台

## 现象

开发角色（`project.develop`）没有 `system:git:repo:manage`，点「新建仓库」后端返回「当前操作没有权限」，但源码页仍显示「新建仓库」按钮。

## 根因

1. 产品设计：开发默认可 list/push/pull，**不能**创建/导入/删除仓库；仅 `project.admin` / `project.create-by` 有 manage。
2. 前端 `v-hasPermi` / `v-hasRole` 迁移 Vue 3 后仍写 Vue 2 钩子 `inserted` / `update`；无 `@vue/compat` 时这些钩子**不会执行**，鉴权指令形同虚设，按钮一直显示。
3. 后端 `@RequiresPermissions` + `assertManage` 仍按项目角色拒绝，于是出现「能点、提交失败」。

## 解决办法

把 `hasPermi.js` / `hasRole.js`（及同类遗漏如 `imgFallback`）改为 Vue 3 的 `mounted` / `updated` / `unmounted`，并对 `permissions` / `roles` 做 `watch`，切换项目后也能重算显隐。

## 以后怎么避免

新增或迁移自定义指令时对照 Vue 3 钩子表（`inserted`→`mounted`，`unbind`→`unmounted`）；改完用无目标权限的账号点一遍入口，确认按钮隐藏而不是只靠后端 403。
