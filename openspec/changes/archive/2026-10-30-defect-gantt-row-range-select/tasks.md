## 1. 选中模型与行序

- [x] 1.1 增加 `selectionAnchorId`；扩展 `selectDefect` 支持 `additive` / `range` / `mergeRange`（无锚点时 Shift 退化单选）
- [x] 1.2 实现可见已排期扁平行序（跳过折叠组、按 defectId 去重），供闭区间扫描

## 2. 左右接线

- [x] 2.1 左侧行头 click：按修饰键调用统一选中；无修饰保留 focus/滚动，有修饰不强制滚轴、不打开详情
- [x] 2.2 右侧条 mousedown：Ctrl/⌘ 加减；Shift 区间；Ctrl/⌘+Shift 并入；不再把单独 Shift 当加减
- [x] 2.3 未排期池 click 保持单选（忽略区间/多选修饰语义）

## 3. 回归与文档

- [x] 3.1 确认框选、拖已选条批量平移、拖避让多选不推挤仍读同一 `marqueeSelectedIds`
- [x] 3.2 回写 `docs/04-UI规范/组件与交互约定.md`、`docs/08-版本记录` 当月一行
