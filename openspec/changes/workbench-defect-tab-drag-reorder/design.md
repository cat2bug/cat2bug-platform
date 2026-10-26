## Context

工作台页签拖序已在 `workbench-custom-tabs` / ADR-021 落地：Sortable.js + `forceFallback`、拖完还原 DOM、同可见性组 `PUT .../tabs/sort`。现网拖不动的根因是 `filter` 写成单参并把 **事件对象** 交给 `isTabNavDraggable`，恒判不可拖。缺陷侧表字段与 `PUT /system/tabs/sort`（`updateSortOfTabs`）已存在，列表按 `tab_sort desc` 返回，但 `defect/index.vue` 未绑 Sortable。

## Goals / Non-Goals

**Goals:**

- 修复工作台 `filter(evt, target)`，使同组可拖自定义页签恢复可用。
- 从「更多」触发器（`.wb-tab-menu`）上的指针不启动拖动。
- 缺陷自定义页签可拖改序并调用已有 sort API；刷新后顺序保持。
- 固定页签（工作台 / 全部 / 已删除）与加号仍不可拖。

**Non-Goals:**

- 不跨共享/私有组拖序；不移动固定「工作台」位置。
- 不改后端 sort 契约或表结构。
- 不把顺序只存本地（两边均以服务端 `tab_sort` 为准）。

## Decisions

### 1. 工作台：修 filter 签名 + 菜单排除

- **选择**：`filter(evt, target) { ... isTabNavDraggable(target) }`；若 `evt.target` 落在 `.wb-tab-menu` 内则 filter 掉（不启动拖）。
- **备选**：改用 `handle` 只允许拖标签文字 → 改动面大，否决。
- **备选**：去掉 filter 只靠 onMove → 工作台/加号可能被误拖，否决。

### 2. 缺陷：移植工作台 Sortable 模式

- **选择**：同一套 `forceFallback` + `onEnd` 读序 → `syncNavDom` → 更新本地 `config.tabs` 的 `tabSort` → `updateSortOfTabs`。不可拖：`all-tab` / `deleted-tab` / 加号 pane。
- **备选**：仅本地 localStorage 记序 → 与多端/刷新真相源不一致，否决。
- **排序值**：与工作台一致用递减时间戳（或等价单调值），越大越靠前，匹配 SQL `order by tab_sort desc`。

### 3. 新建缺陷页签的默认 tabSort

- **选择**：创建时若后端未赋较大 `tabSort`，前端在首次拖序时统一重写组内排序即可；若 insert 默认 0，新建项可能总在末尾——可接受，或 create 时传 `Date.now()`（若 API 支持）。实现时核对 insert 行为，必要时在 add 成功后补 sort。

## Risks / Trade-offs

- [拖动与点选/更多菜单冲突] → 菜单区 filter；`preventOnFilter: false` 保留点击；回归拖+点菜单。
- [Vue/i18n 节点被 Sortable 挪坏] → 继续 forceFallback + 拖完立刻 sync DOM。
- [同组仅 1 个自定义 tab] → 无法换位属预期；验收需 ≥2 个。

## Migration Plan

- 纯前端修复/增强；无迁移脚本。
- 回滚：还原 dashboard/defect 的 Sortable 相关改动。

## Open Questions

- 无。

## 相关长期文档

- `docs/07-技术决策/ADR-021-工作台固定页与自定义私有共享页签.md`（同组拖序约束，不修改）
- `docs/04-UI规范/组件与交互约定.md`（归档前可补页签拖序）
- `docs/01-产品需求/产品定位与核心功能.md`（工作台拖序已有描述；缺陷页签可补一句）
- 拟新增 ADR：无
