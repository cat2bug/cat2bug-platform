# case-prompt-export Specification

## Purpose
TBD - created by archiving change close-spring-parity-gaps. Update Purpose after archive.
## Requirements
### Requirement: 用例提示词导出 Excel
系统 SHALL 提供 `POST /system/CasePrompt/export`，按与列表相同的查询条件导出用例提示词为 Excel，列与 Spring 线 `SysCasePromptController#export` 一致，并要求与 Spring 相同的权限。

#### Scenario: 导出当前项目提示词
- **WHEN** 有权限的用户以项目 ID 为条件调用导出
- **THEN** 返回 `.xlsx` 文件，行数与同条件列表的总数一致

#### Scenario: 无权限
- **WHEN** 无导出权限的用户调用导出
- **THEN** 返回 403

