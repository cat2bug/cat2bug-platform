## Context

新建缺陷抽屉（`AddDefect.vue`）提供「保存本次选项用于下次创建时免填」：开关存 `defect-is-save-add-form-cache`，字段快照存 `defect-add-form-cache`（local）。保存成功且开关开启时不关抽屉，而是 `reset()` 再 `readAddFormCache()`，便于连续录入。

现状：

- `saveAddFormCache` / `readAddFormCache` 白名单仅为 `defectType`、`defectLevel`、`handleBy`、`moduleVersion`、`moduleId`，**不含** `defectDescribe`。
- 描述控件在 `DefectFormOrderedFields` 中仅 `:content="form.defectDescribe"`，未 `@change` / `v-model` 回写；用户键入主要留在 `Cat2BugTextarea` 内部 `textContent`。
- `reset()` 将 `form.defectDescribe` 置 `null` 时，若表单侧本就为 `null`，`content` watch 不触发，编辑器残留上次文案——表现为描述被「免填」。

## Goals / Non-Goals

**Goals:**

- 产品语义：免填**不包含**描述；连续新建后描述输入区为空。
- 技术：描述与 `form.defectDescribe` 双向一致，提交与重置都可靠。

**Non-Goals:**

- 不扩展/缩减其它免填字段（名称、图片、附件、自定义字段仍不进缓存）。
- 不改后端、不做服务端偏好存储。
- 不处理缺陷名称整行宽度（已另改，不属本变更）。

## Decisions

1. **保持白名单，不引入「排除列表」**  
   继续显式写入允许字段；在代码注释与 spec 中写明描述永不缓存。避免「先全量再删」误带大字段。

2. **描述绑定改为双向**  
   在 `DefectFormOrderedFields` 对 `Cat2BugTextarea` 使用 `@change`（或等价 `v-model`）写回 `form.defectDescribe`。这样 `reset` 赋 `null` 会触发 `content` watch 清空内部状态。

3. **`reset` 后防御性清空**  
   若 ref 可取到描述组件，在 `$nextTick` 再确保内容为空（防止旧实例未收到 prop 变化）。优先依赖双向绑定；防御清空作为保底。

4. **不迁历史 localStorage**  
   旧缓存本无描述键；无需迁移脚本。

## Risks / Trade-offs

- [Risk] 仅修清空不修绑定 → 提交时描述仍可能丢字 → Mitigation：绑定与清空一起做。  
- [Risk] AI 填描述路径与手动输入不一致 → Mitigation：统一写 `form.defectDescribe`，控件跟 content。  
- [Trade-off] 防御性清空略冗余，换连续新建可靠。

## Migration Plan

- 仅前端；发版后硬刷即可。回滚：还原绑定与 `reset` 逻辑。

## Open Questions

- 无。

## 相关长期文档

- `docs/01-产品需求/产品定位与核心功能.md`（缺陷模块入口）
- `docs/04-UI规范/组件与交互约定.md`（若有表单交互约定可引用；无则保持本 design）
- 拟新增 ADR：无
