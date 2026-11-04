# defect-form-personal-field-order Specification

## Purpose
TBD - created by archiving change defect-form-personal-field-order. Update Purpose after archive.
## Requirements
### Requirement: 新建/编辑缺陷表单字段顺序优先个人显示字段序

系统 MUST 在解析新建与编辑缺陷表单字段顺序时，优先使用当前登录用户隔离的缺陷列表「显示字段」field-list 缓存中的 key 顺序；当且仅当该用户无有效个人缓存时，MUST 回退为团队缺陷属性配置产生的 `orderedEnabledFieldKeys`（或等价团队默认列序）。

#### Scenario: 有个人显示字段序时表单跟个人序

- **WHEN** 当前用户已有对象数组形式的缺陷显示字段缓存，且团队已启用若干内置与自定义字段
- **THEN** 新建/编辑表单中字段出现顺序 MUST 与该缓存 key 序经 table→表单映射后一致（计划开始/结束合并为单一计划时间控件；编号、状态、创建人、更新时间仍不出现在表单）

#### Scenario: 无个人缓存时回退团队序

- **WHEN** 当前用户没有缺陷显示字段个人缓存（或仅有旧版 string[] 仅显隐缓存以致无法表达个人序）
- **THEN** 表单字段顺序 MUST 与团队 `orderedEnabledFieldKeys` 一致（含自定义字段穿插），与变更前团队权威行为相同

#### Scenario: 列表隐藏不影响表单显隐

- **WHEN** 用户在显示字段中将某团队已启用字段设为不可见，但团队仍启用该字段
- **THEN** 该字段 MUST 仍出现在新建/编辑表单中，仅顺序遵循个人 key 序（或团队回退序）

#### Scenario: 打开表单时读取最新个人序

- **WHEN** 用户刚调整过列表显示字段顺序后打开新建页或编辑抽屉
- **THEN** 表单字段顺序 MUST 反映最新个人缓存，不得沿用打开前内存中的旧序

