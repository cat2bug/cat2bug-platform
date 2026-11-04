# EP2 表单 trigger 用 input 导致填了仍报红

- 日期：2026-11-04
- 涉及：platform-ui（缺陷合并/新建/编辑等）

## 现象

字段已有内容（或点「使用此字段」写入后），下面仍显示「xxx不能为空」红字。

## 根因

Element UI（Vue2）时代常用 `trigger: 'input'`。Element Plus 的 `el-input` 只会在 **`change` / `blur`** 时调用 `formItem.validate(...)`。规则写 `trigger: 'input'` 时：

1. 点提交 → `form.validate('')` 会跑全部规则，空值报红；
2. 之后输入或程序化赋值触发的是 `change`，规则被 `getFilteredRule` 滤掉，红字不消。

## 解决办法

必填规则改成 `trigger: ['change', 'blur']`（与自定义字段 `format.js` 一致）。程序化改表单值后，再 `clearValidate` / `validateField` 一次。

## 以后怎么避免

新表单不要写 `trigger: 'input'`；从旧 Element UI 迁过来的规则一并改成 `change`/`blur`。
