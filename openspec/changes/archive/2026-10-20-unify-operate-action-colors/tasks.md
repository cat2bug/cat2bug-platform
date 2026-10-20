## 1. Token 与全局样式

- [x] 1.1 在浅色/暗色增加 `--cat2bug-operate-text-primary`（及 hover），绑定品牌主色；为 `.el-button.is-link.blue` / `--text.blue`（含 `.cat2bug-operate-tools` / `.defect-tools` 作用域）补规则，镜像 green/red
- [x] 1.2 确认既有 `.green` / `.red` 仍走 success/danger token，暗色 `!important` 块覆盖完整

## 2. 首批列表页打齐 class

- [x] 2.1 交付物操作列：修改 `green`、添加 `blue`、删除 `red`
- [x] 2.2 OpenAI 账号操作列：修改 `green`、删除 `red`（有添加则 `blue`）
- [x] 2.3 用例类型及其它同模式操作列：修改 `green`、删除 `red`；行内添加补 `blue`
- [x] 2.4 核对缺陷 `DefectTools`：修改/删除已对齐；不改流转 `orange`

## 3. 验收

- [x] 3.1 浅色/暗色对照缺陷与交付物操作列：修绿、加蓝、删红可读（代码侧：token + class 已齐；请本地硬刷新再看一眼）

## 4. 回写长期知识

- [x] 4.1 更新 `docs/04-UI规范/组件与交互约定.md`：操作列 CRUD 三色与 class ↔ token
- [x] 4.2 `docs/08-版本记录/2026-10.md` 增加本变更一行；拟新增 ADR：无
- [x] 4.3 可复用踩坑：无则勾掉注明

备注（2.3）：顺带计划/文档/用例/缺陷字段/Webhook「修改」补 `green`；复制/执行/测试/下载等未打 CRUD 色。
