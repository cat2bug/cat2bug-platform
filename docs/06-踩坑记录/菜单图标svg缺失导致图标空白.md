# 菜单图标 svg 缺失导致图标空白

记录侧栏菜单图标不显示的排查方向。

- 日期：2026-10-14
- 涉及：前端 / 数据库

## 现象

侧栏「文档」菜单图标空白，其它菜单正常（2026-10，`sys_menu.icon = library`）。

## 根因

菜单图标名存在数据库 `sys_menu.icon`，前端按名字到 `cat2bug-platform-ui/src/assets/icons/svg/` 找同名 svg。前端瘦身/迁移过程中 `library.svg` 不在该目录里，于是渲染为空，且没有任何报错。

## 解决办法

补回 `cat2bug-platform-ui/src/assets/icons/svg/library.svg`（提交 `59f6a23e`）。

相关：Vite 迁移后菜单管理页的图标选择器 `IconSelect/requireIcons.js` 也曾因 `require.context` 失效而空白，改为 `import.meta.glob`（见 `openspec/changes/archive/2026-10-14-restore-quarkus-full-delivery/SMOKE-RESULTS.md`）。

## 以后怎么避免

- 新增/修改菜单的 Flyway 脚本里写了 `icon`，必须同时确认对应 svg 存在。
- 删除或清理 svg 前，先在 `db/**` 迁移脚本里搜一遍图标名。
- 历史上也有过图标丢失或图标名不对的修正：恢复 `bug.svg`（`98fe5996`）、文档管理图标改 `education`（`0f935fdc`）、测试计划图标改 `date`（`508a6bbd`）。

返回 [[说明]]
