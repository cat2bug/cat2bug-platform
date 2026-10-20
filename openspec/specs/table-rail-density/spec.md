# table-rail-density Specification

## Purpose
TBD - created by archiving change fix-table-rail-vs-ep-small. Update Purpose after archive.
## Requirements
### Requirement: 表格轨样式覆盖 Element Plus small 表

全站业务表格（含因 ConfigProvider `size=small` 带有 `.el-table--small` 的表）SHALL 对单元格使用 `--cat2bug-table-cell-padding-y`（默认 16px）作为上下 padding，并对表体数据单元格施加 `--cat2bug-table-row-min-height`（默认约 56px）量级的最小行高。上述规则 MUST 在与 Element Plus `.el-table--small` 默认紧凑 padding 冲突时仍生效。

#### Scenario: 裸 el-table 在 small 下仍为表格轨行高

- **WHEN** 用户打开仍使用裸 `el-table` 的业务列表（且表处于全局 small 尺寸）
- **THEN** 数据行视觉行高与已迁 `Cat2BugTable` 的列表同轨（约 56px 单行文本行），不得明显矮于缺陷/用例列表

#### Scenario: Cat2BugTable 列表不退化

- **WHEN** 用户打开已使用 `Cat2BugTable` 的列表（如缺陷、用例）
- **THEN** 行高与变更前一致，仍遵循表格轨 token

### Requirement: 用例类型列表使用 Cat2BugTable

项目「用例类型」管理页 SHALL 使用 `Cat2BugTable` 展示类型名称、引用数与操作列；操作列 MUST 使用 `cat2bug-operate-column`（及 `.cat2bug-operate-tools`），修改/删除继续使用既有语义色 class（`green` / `red`）。

#### Scenario: 用例类型页行高与操作列合规

- **WHEN** 用户打开用例类型管理页
- **THEN** 表格为 `Cat2BugTable`，单行高度符合表格轨，操作列修改为绿、删除为红

