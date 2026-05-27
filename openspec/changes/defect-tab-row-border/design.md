## 背景

`.defect-tools-tab` 为 flex 行：`el-tabs`（flex:1）+ `.defect-tools-tab-right`（统计图标）。Element 默认在 `.el-tabs__nav-wrap::after` 画 1px 底线，仅覆盖 tabs 区域。

## 方案

在父级 `.defect-tools-tab` 增加 `border-bottom: 1px solid #E4E7ED`（与 Element UI 默认 Tab 底线色一致），并：

```scss
::v-deep .el-tabs__nav-wrap::after {
  display: none !important;
}
```

参考 `TreePlanItemModule.vue` 在 header 统一底边的做法。

## 右侧对齐

移除 `.defect-tools-tab-right` 的 `padding-bottom: 5px`，保持 `align-items: center`，使图标与 Tab 文字垂直居中，底线由父级统一承担。

## 风险

- 仅样式变更，不影响 Tab 选中指示条（`active-bar`）
- 统计图标 `v-show` 隐藏时，全宽底线仍合理
