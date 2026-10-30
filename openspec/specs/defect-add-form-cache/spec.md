# defect-add-form-cache Specification

## Purpose

新建缺陷「免填」本地缓存：仅配置类字段；描述永不缓存且重置后编辑器必须清空。

## Requirements

### Requirement: 免填缓存不含缺陷描述

新建缺陷「保存本次选项用于下次创建时免填」所持久化的字段 MUST 仅包含业务配置类选项（缺陷类型、优先级、处理人、版本、交付物）。系统 MUST NOT 将缺陷描述（`defectDescribe`）写入或读出该本地缓存。

#### Scenario: 勾选免填并保存后再次打开新建

- **WHEN** 用户勾选「保存本次选项用于下次创建时免填」，填写描述与其它字段并成功创建缺陷
- **THEN** 本地免填缓存中 MUST NOT 出现描述内容
- **AND** 下次进入新建（或连续新建重置后）描述输入区 MUST 为空（或等价于未填写）

#### Scenario: 免填仍恢复允许的配置字段

- **WHEN** 用户已开启免填且缓存中有类型、处理人等允许字段
- **THEN** 新建表单重置后 MUST 恢复这些允许字段
- **AND** MUST NOT 因恢复缓存而填充描述

### Requirement: 描述与表单双向一致且重置可清空

新建缺陷表单中的描述编辑器 MUST 将用户输入同步到表单模型 `defectDescribe`；表单将描述置空时，编辑器展示 MUST 同步为空。

#### Scenario: 连续新建清空描述编辑器

- **WHEN** 用户开启免填，在描述中输入正文并成功创建，系统执行连续新建的表单重置
- **THEN** 描述编辑器可见内容 MUST 为空
- **AND** 表单模型 `defectDescribe` MUST 为空值

