## 1. 固化审查通过的封面资源

- [x] 1.1 将 `preview/` 中确认的 10 张封面 SVG 写入 `cat2bug-platform-ui/src/assets/images/project/project_icon1.svg` 至 `project_icon10.svg`（覆盖旧插画）
- [x] 1.2 确认 `PROJECT_DEFAULT_ICON_COUNT` 仍为 10，`getProjectIconStoragePath` 仍返回 `/static/img/project_iconN.svg`

## 2. 缩小封面选择器

- [x] 2.1 将 `add.vue` 与 `edit.vue` 的封面预览改为约 96×96
- [x] 2.2 将 `page-kbd-hints.scss` 中项目封面浮层的格子改为约 72×72，去掉 550px 宽和 130px 格子
- [x] 2.3 将两处 `image-upload` 的 `buttonStyle` 改为约 72×72；若组件样式把按钮撑大，只在 `.project-icon-popper` 内覆盖
- [x] 2.4 保持键盘网格为 10 个预设加 1 个上传格，并跑通 `project-icon-popover-kbd` 相关单测

## 3. 构建产物

- [x] 3.1 按现有 `build:embedded` 更新内嵌静态资源，删除旧的 `project_icon*-<hash>.svg`，不保留旧插画

## 4. 回写长期知识

- [x] 4.1 无新 ADR、无新踩坑；约定写入 `docs/04-UI规范/图标规范.md`，版本记录见 `docs/08-版本记录/2026-10.md`
