## Why

项目创建和编辑里「更改封面」的 10 张预设是无授权声明的 800×800 场景插画，合计约 545KB，选择浮层格子约 130×130，占空间且和当前界面尺度不匹配。需要换成自绘、体积更小的项目封面图，并缩小选择器与上传格。

## What Changes

- 用 10 张自绘彩色封面 SVG 替换 `project_icon1.svg` 至 `project_icon10.svg`（整张封面，不是菜单线框图标）。文件名与序号不变。审查稿见本变更目录 `preview/`。
- 题材依次为：手机应用、打开的书、日历、叠放的纸、三只箱子、柱状图、对话气泡、纸飞机、文件夹、三列看板。
- 创建/编辑表单的封面预览与选择浮层格子缩小（预览约 96px，格子约 72px）；上传格留在最后并同尺寸缩小。
- 仍把 `/static/img/project_iconN.svg` 写入 `project_icon`。展示时按序号加载打包资源。自己上传的 `/profile/upload/` 图片保持不变。
- 还在使用预设序号的项目会一起显示新封面。不迁移数据库，不保留旧插画文件。
- 不改团队图标、权限、菜单或后端接口。

## Capabilities

### New Capabilities

- `project-preset-icons`：项目预设封面的图形、尺寸、存储路径，以及选择浮层里上传格的尺寸。

### Modified Capabilities

- 无。

## Impact

- 前端资源：`cat2bug-platform-ui/src/assets/images/project/project_icon1.svg` 至 `project_icon10.svg`，以及构建产物里带内容哈希的副本。
- 前端界面：`views/system/project/add.vue`、`edit.vue`，封面浮层样式 `page-kbd-hints.scss`，列表铭牌 `ProjectNameplate`。
- 数据：`sys_project.project_icon` 不改表、不改已存路径。预设序号仍由 `parseProjectIconIndex` 识别。
- 不推翻已有 ADR。
