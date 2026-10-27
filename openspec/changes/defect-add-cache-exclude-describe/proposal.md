## Why

新建缺陷勾选「保存本次选项用于下次创建时免填」后，用户期望类型、处理人等配置可复用，但**描述**每次都不同，不应被带入下一次。当前本地缓存白名单虽未写入 `defectDescribe`，连续新建时描述编辑器（`Cat2BugTextarea`）因与表单单向绑定，重置后内部文案仍可能残留，表现为「描述也被免填」。

## What Changes

- 明确约定：免填缓存**永不**包含缺陷描述（`defectDescribe`）；保存/读取白名单保持「类型 / 优先级 / 处理人 / 版本 / 交付物」。
- 修复描述与表单的双向同步：用户输入写回 `form.defectDescribe`；`reset` / 连续新建后描述编辑器视觉与表单均为空。
- 不改变后端 API、不改动其它免填字段集合（名称、图片、附件、自定义字段仍不进缓存，与现状一致）。

## Capabilities

### New Capabilities

- `defect-add-form-cache`：新建缺陷「保存本次选项免填」的字段范围与连续新建时描述必须清空的行为。

### Modified Capabilities

- （无）

## Impact

- 前端：`AddDefect.vue`（缓存读写与 `reset`）、`DefectFormOrderedFields.vue`（描述组件绑定）、必要时 `Cat2BugTextarea` 清空行为。
- 无后端、无迁移、无权限变更。
- 相关 UI：缺陷名称整行宽度已在先前小改中落地，**不在本变更范围**。
