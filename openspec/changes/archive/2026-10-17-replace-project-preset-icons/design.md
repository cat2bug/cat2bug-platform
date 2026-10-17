## Context

「更改封面」弹出的红框内容是项目封面预设，不是侧栏菜单图标。现有文件 `project_icon1.svg`～`project_icon10.svg` 是 800×800 多色场景插画，无许可证，单张 24–87KB。点选时 `getProjectIconStoragePath` 只写入 `/static/img/project_iconN.svg`，不拷贝到上传目录；`resolveProjectIconUrl` 用序号加载打包资源。自定义上传走 `/profile/upload/`。

审查结论：替换物必须是整张封面小图（色块 + 简单图形），不能是 24px 线框功能图标。第二版审查稿在 `openspec/changes/replace-project-preset-icons/preview/`。

## Goals / Non-Goals

**Goals:**

- 用自绘彩色封面 SVG 替换 10 张预设，文件名与序号不变。
- 表单预览约 96×96，浮层预设格与上传按钮约 72×72。
- 已存的预设路径继续解析到新图；已上传的自定义封面保持原文件。

**Non-Goals:**

- 不做菜单风格的线框图标或 Element Plus 图标名入库。
- 不改 `sys_project` 表，不批量改已存路径，不保留旧插画。
- 不改团队图标、权限、菜单和后端项目接口。

## Decisions

### 1. 原地替换为封面式 SVG

继续用 `project_icon1.svg` … `project_icon10.svg` 和 `PROJECT_DEFAULT_ICON_COUNT = 10`。每张是完整方形封面：圆角色底 + 一个简单主体图形，viewBox 约 80，路径自绘，单张控制在数 KB 内。

| 序号 | 题材 |
|---|---|
| 1 | 手机应用 |
| 2 | 打开的书 |
| 3 | 日历 |
| 4 | 叠放的纸 |
| 5 | 三只箱子 |
| 6 | 柱状图 |
| 7 | 对话气泡 |
| 8 | 纸飞机 |
| 9 | 文件夹 |
| 10 | 三列看板 |

备选（已否决）：线框功能图标——用户明确「项目图标不是那种」；整包商用插画——版权与体积问题依旧。

### 2. 缩小封面选择器，保留上传格

`add.vue` / `edit.vue` 预览约 96×96。`page-kbd-hints.scss` 去掉 550px / 130px 写死尺寸，格子约 72×72，浮层随网格收缩。`image-upload` 的 `buttonStyle` 改为约 72×72；若组件把按钮撑大，只在 `.project-icon-popper` 内覆盖。键盘网格仍是 10 + 1。`ProjectNameplate` 保持 50×50，只换资源。

### 3. 不拷贝预设到上传目录

选预设仍只存路径字符串。换文件后所有预设序号封面一起更新。自定义上传路径不含 `project_iconN.svg`，继续按上传地址显示。

## Risks / Trade-offs

- [报告里写死了带旧内容哈希的文件名] → 重建后旧哈希文件删除；仍带 `project_iconN` 且走 `parseProjectIconIndex` 的地址显示新图。不保留旧插画。
- [封面在 50px 铭牌上糊成一团] → 图形只保留一个主体，避免细节过多。
- [暗色主题对比] → 封面自带色底，不依赖 `currentColor`；在浅色和暗色下列表与表单各看一次。

## Migration Plan

1. 把审查通过的封面 SVG 写入 `src/assets/images/project/`，缩小选择器与上传格。
2. 按 `build:embedded` 更新内嵌静态资源，删除旧的 `project_icon*-<hash>.svg`。
3. 不写数据库迁移。回滚是还原这 10 个 SVG 与相关样式。

## Open Questions

无。审查稿已确认方向为封面式，实现以 `preview/` 当前图形为准，可在实现时微调路径细节。

## 相关长期文档

- `docs/04-UI规范/图标规范.md`（项目预设封面）
- `docs/04-UI规范/主题与配色.md`
- `docs/04-UI规范/组件与交互约定.md`
- 不新增 ADR
