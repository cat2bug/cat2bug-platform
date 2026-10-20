## ADDED Requirements

### Requirement: 列表操作列增改删使用统一语义色

业务列表操作列中的 text/link「修改」（含编辑）按钮 SHALL 使用语义 class `green`，颜色取自 `--cat2bug-operate-text-success`（及对应 hover token）。「添加」（行内新增子项等）按钮 SHALL 使用语义 class `blue`，颜色取自 `--cat2bug-operate-text-primary`（绑品牌主色 token）。「删除」按钮 SHALL 使用语义 class `red`，颜色取自 `--cat2bug-operate-text-danger`。业务页 MUST NOT 为上述三色写死十六进制色值。本条适用于表格操作列；缺陷流转类按钮（指派、修复等）的 `orange` 语义不受本条强制改为绿/蓝。

#### Scenario: 交付物行内三色

- **WHEN** 用户在交付物列表某一行查看操作列
- **THEN** 「修改」为绿色语义、「添加」为蓝色语义、「删除」为红色语义（浅色与暗色主题下均成立）

#### Scenario: OpenAI 账号修改与删除

- **WHEN** 用户在 OpenAI 账号列表某一行查看操作列
- **THEN** 「修改」为绿色语义、「删除」为红色语义；若该行有同类「添加」则亦为蓝色语义

#### Scenario: 缺陷列表修改删除不变质

- **WHEN** 用户在缺陷列表操作列查看「修改」与「删除」
- **THEN** 二者仍分别为绿与红语义，且流转类按钮不因本变更被改成绿或蓝

### Requirement: 蓝色语义 class 与 token 全局可用

系统 SHALL 提供与 `.green` / `.red` 同级的 `.blue`（作用于 `el-button` 的 link/text），并定义 `--cat2bug-operate-text-primary` 与 hover 变体，在浅色与暗色主题下均可读且对比度可接受。

#### Scenario: 无业务硬编码色

- **WHEN** 维护者检查 `.blue` 样式实现
- **THEN** 颜色仅通过 CSS 变量引用，且暗色规则与现有 operate-text 块同级覆盖
