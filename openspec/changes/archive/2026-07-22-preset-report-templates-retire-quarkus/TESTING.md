# TESTING — preset-report-templates-retire-quarkus

## 预制资源

- [ ] `scripts/sync-report-preset-templates.sh` 可重复执行
- [ ] `cat2bug-platform-admin/src/main/resources/报告/模版/manifest.json` 含 `iso-29119-3` 与 `iso-9001` 两组
- [ ] 29119-3 业务 md 数量与 skills 目录一致（除 README）
- [ ] 9001 `manuals/` + `records/` 业务 md 与 skills 一致（除 README）
- [ ] 各 item 有对应 png 或允许缺省回退

## API（需登录 + `system:report:list` / `add`）

- [ ] `GET /system/report/template/picker?projectId=` 返回三组：`custom` → `iso-29119-3` → `iso-9001`
- [ ] `GET .../preset/iso-29119-3/{key}/content` 返回 Markdown
- [ ] `GET .../preset/iso-9001/manuals/{key}/content` 与 `.../records/{key}/content` 可用
- [ ] `GET .../preview` 返回 `image/png`
- [ ] `POST .../copy?projectId=` 后项目自定义列表出现可编辑副本
- [ ] 对预制无 DB id；update/delete 仅影响自定义行

## UI

- [ ] 生成报告下拉：三组顺序正确；添加仅自定义
- [ ] 预制卡片无编辑/删除，仅复制
- [ ] 点击预制可生成报告（正文来自 classpath）
- [ ] 复制后可编辑删除；预览图显示或占位

## Quarkus 退役

- [ ] `CLAUDE.md` / `README.md` 默认 Spring admin
- [ ] 无活跃 `build-embedded-quarkus` / `deploy/*quarkus*` 入口
- [ ] 默认 Maven 构建不依赖 Quarkus 模块

## 自动化

- [ ] 前端/后端单测（若本变更已添加）通过
