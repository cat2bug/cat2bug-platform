## 1. 描述双向绑定

- [x] 1.1 在 `DefectFormOrderedFields`（或等价新建表单字段组件）为描述 `Cat2BugTextarea` 增加 `@change` / 等价回写，同步 `form.defectDescribe`
- [x] 1.2 确认 AI 填描述、手动输入两条路径均写同一模型字段

## 2. 免填与重置语义

- [x] 2.1 核对 `saveAddFormCache` / `readAddFormCache` 白名单不含 `defectDescribe`，必要时补注释说明「描述永不缓存」
- [x] 2.2 在连续新建 `reset` + `readAddFormCache` 路径保证描述模型为空，并在 `$nextTick` 防御性清空编辑器可见内容（若需）

## 3. 验证

- [x] 3.1 勾选免填：填写描述与其它字段 → 保存成功 → 描述区空、类型/处理人等仍恢复
- [x] 3.2 关闭免填：保存后抽屉关闭或按现有行为，无描述残留到下次打开（按产品现有关闭逻辑）
- [x] 3.3 提交时描述正文能正确随表单提交（绑定修复回归）

## 4. 回写长期知识

- [x] 4.1 评估是否需写入 `docs/06-踩坑记录/`（Cat2BugTextarea 单向 content 导致重置不刷新）；已写 `docs/06-踩坑记录/Cat2BugTextarea单向content导致重置不刷新.md`
