# Cat2BugTextarea 单向 content 导致重置不刷新

- 日期：2026-10-27
- 涉及：后台（缺陷新建抽屉）

## 现象

勾选「保存本次选项用于下次创建时免填」连续新建时，保存成功后描述输入区仍显示上一单正文，看起来像描述也被免填。

## 根因

1. 表单只用 `:content="form.defectDescribe"`，未 `@change` / `v-model` 回写；键入主要留在组件内部 `textContent`。
2. `reset` 把 `form.defectDescribe` 置 `null`，若模型本就为 `null`，旧 `content` watch 用 `n !== o` 判断，两边都是 `null` 不更新内部状态。
3. 免填缓存白名单本不含描述，问题在编辑器残留，不在 localStorage。

## 解决办法

- 描述增加 `@change` 回写 `form.defectDescribe`；`Cat2BugTextarea` 在 `@input` 也触发 change。
- `content` / `modelValue` watch 与内部 `textContent` 比较后再同步。
- 暴露 `clear()`，新建 `reset` 后 `$nextTick` 调用 `clearDescribeEditor()`。

## 以后怎么避免

富文本 / 自研输入组件若维护内部状态，父级重置必须双向绑定或显式 `clear`，不要假定「prop 仍是 null」会清空 UI。
