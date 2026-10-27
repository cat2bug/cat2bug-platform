## Why

工作台编辑左侧组件库是扁平文字 chip 列表：组件一多就难扫，看不出长什么样，也不知道当前页已经用过哪些类型。列内堆叠上线后目录更长，需要按业务域分组、真图预览与「本页已用」提示，降低配置成本。

## What Changes

- 组件库按**产品业务域**分组展示（缺陷 / 用例 / 文档 / Git / 计划 / 通知 / 总览等），可折叠。
- 每个目录项展示**真实预览图**；预览画幅固定宽高比，图片以 `contain` 方式放入，**不得拉伸或裁切变形**；缺图时用同比例占位。
- 当前正在编辑的页面（该 Tab 全部 rows）上已出现过的 `type` 显示「已添加」类标记；**仅提示，不禁止再拖**到其它列（同列 type 唯一规则不变）。
- 抽出共享目录组件，供行编辑器及日后自由栅格等编辑面复用；本期先接到 `WorkbenchRowEditor`（含自定义 Tab 编辑）。
- 注册表为每个可目录组件补充 `group` 与预览资源约定；全量目录项交付真图。
- 不改变放置/堆叠/持久化语义；不做实况迷你渲染；不重做 FreeGrid 布局引擎。

## Capabilities

### New Capabilities

- `workbench-widget-catalog`: 工作台编辑组件库的分组、真图预览（保比例）与本页已用提示；可被各编辑面复用。

### Modified Capabilities

- `workbench-row-layout`: 行编辑模式的左侧目录 MUST 使用上述共享组件库能力（替换扁平 chip 列表），放置与同列唯一规则保持既有要求。

## Impact

- 前端：`widget-registry.js`（`group` / preview）、新建共享目录组件、`WorkbenchRowEditor.vue`、i18n、静态预览资源（如 `assets/workbench/previews/`）。
- OpenSpec：`openspec/specs/workbench-widget-catalog/`（新）、`workbench-row-layout` delta。
- 文档：产品侧工作台编辑说明可回写；一般不需要新 ADR（UI 呈现约定，非架构分叉）。
- 无后端 API / 持久化 schema 变更。
