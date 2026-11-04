## 1. 入口组件与 mailto 拼装

- [x] 1.1 新增顶栏反馈组件（对齐 `Cat2Bug/Site`），常量收件人 `service@cat2bug.com`，用 `email` 图标
- [x] 1.2 实现 mailto 拼装：i18n 主题 + 只读环境正文（locale / 昵称 / 项目等，UA 截断）+ `encodeURIComponent`；以 `location.href`（或等价）打开

## 2. 顶栏集成与文案

- [x] 2.1 在 `Navbar.vue` 桌面工具组接入组件（文档旁），`data-layout-nav="feedback"`，显隐与官网/文档同组
- [x] 2.2 补充各语言 i18n（tooltip / subject / body 引导）；按需登记布局键盘锚点

## 3. 验证与回写

- [x] 3.1 桌面点击核对收件人/主题/正文；确认 mobile 与 peer 工具同隐；焦点环符合约定
- [x] 3.2 回写长期知识：`docs/08-版本记录` 当月一行；无新 ADR / 无新踩坑则注明跳过
