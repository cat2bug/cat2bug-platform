## Why

工作台编辑侧栏组件库已按业务域分组，组件较多时仍需靠展开各组翻找。需要按名称快速过滤，缩短定位目标组件的时间。

## What Changes

- 组件库顶部增加搜索框，按当前语言下的组件显示名称（可顺带匹配 type id）做子串过滤。
- 有关键词时：只展示含命中项的分组，并自动展开这些分组；无命中时显示空态文案。
- 清空搜索后恢复既有手风琴「同时只开一组」行为。
- 不改拖拽契约、已添加标记、预览缩放逻辑。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `workbench-widget-catalog`: 增加按名称搜索/过滤与搜索态下的分组展开规则

## Impact

- 前端：`WorkbenchWidgetCatalog.vue`、相关 i18n（zh-CN / en-US）
- 无后端 / 持久化 / API 变更
