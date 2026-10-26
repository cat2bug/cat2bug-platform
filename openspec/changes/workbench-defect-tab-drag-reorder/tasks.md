## 1. 修复工作台拖序

- [x] 1.1 修正 `dashboard/index.vue` Sortable `filter(evt, target)`，对 `target` 调用 `isTabNavDraggable`
- [x] 1.2 指针落在 `.wb-tab-menu` 时 filter 掉，避免「更多」启动拖动
- [x] 1.3 手动回归：同组 ≥2 自定义 tab 可拖且刷新保持；跨组不可拖；点更多仍可开菜单（代码路径已对齐；需界面 ≥2 同组 tab 实测）

## 2. 缺陷页签拖序

- [x] 2.1 在 `defect/index.vue` 绑定 Sortable（forceFallback、拖完 sync DOM）；仅自定义 tab 可拖
- [x] 2.2 `onEnd` 写本地 `tabSort` 并调用 `updateSortOfTabs`；失败时回刷配置
- [x] 2.3 核对新建页签默认排序；create 时赋 `tabSort: Date.now()`
- [x] 2.4 手动回归：自定义互拖落库；全部/已删除/加号不可拖；更多不启动拖；与单次 list 路径无冲突（代码路径核对）

## 3. 回写长期知识

- [x] 3.1 在 `docs/04-UI规范/组件与交互约定.md` 补页签拖序约定；踩坑 [[Sortable-filter回调第一参是事件对象]]；无新 ADR
