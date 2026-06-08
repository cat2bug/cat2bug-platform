# 缺陷管理 [/project/defect](/project/defect)

缺陷是测试工作中记录问题的基础单元，每个产品需求、工作任务、软件 BUG，都统一叫做缺陷。

## 什么是缺陷

在 Cat2Bug 平台中，缺陷是一个广义的概念，包括：
- **软件 BUG** - 程序错误、功能异常
- **产品需求** - 新功能、功能改进
- **工作任务** - 待办事项、优化任务

通过统一的缺陷管理，可以：
- 跟踪问题的完整生命周期
- 协调开发和测试工作
- 评估产品质量
- 积累问题知识库

## 名词解释

| 名词 | 说明                                             |
|------|------------------------------------------------|
| 缺陷 | BUG、任务、需求 统称缺陷，是测试工作中记录问题的基础单元                 |
| 缺陷状态 | 缺陷在生命周期中的当前阶段，如处理中、待验证、已驳回、已关闭等 |
| 存活时间 | 缺陷从最近一次开始时间算起，直到结束或至今的时间区间                     |
| 驳回次数 | 缺陷从最近一次开始时间算起，直到结束或至今的时间区间内，反复被驳回的次数总和         |
| 处理人 | 当前负责处理缺陷的人员，通常是开发人员或测试人员                       |

缺陷状态流转与操作说明见 [Table模式介绍](defect/display-mode/table-mode/table-mode-intro.md#缺陷生命周期与工作流程)。

## 页面介绍

缺陷列表是展示缺陷数据的表格，可以通过查询、排序、定义显示列等方式查询显示数据。

![缺陷列表](../../../images/user-guide/current-project/defect/defect-list.png)

::: tip 提示
在列表中每个缺陷的标题列中，统计了存活时间、驳回次数等指标；并在标题上方实时显示查看操作此缺陷的成员头像。
:::

## 功能指南

- [页标签](defect/defect-tabs.md) - 自定义缺陷列表筛选视图
- [数据统计](defect/defect-statistics.md) - 缺陷统计分析和报表
- [键盘快捷键](#键盘快捷键) - 缺陷列表与相关界面快捷键（详见各子页面）

- 缺陷显示模式
  - Table模式
    - [Table模式介绍](defect/display-mode/table-mode/table-mode-intro.md) - Table 模式概述与工作流程
    - [新建缺陷](defect/display-mode/table-mode/defect-create.md) - 创建新的缺陷记录
    - [修改缺陷](defect/display-mode/table-mode/defect-edit.md) - 修改缺陷信息
    - [指派缺陷](defect/display-mode/table-mode/defect-assign.md) - 更改缺陷处理人
    - [修复缺陷](defect/display-mode/table-mode/defect-repair.md) - 开发人员提交修复
    - [驳回缺陷](defect/display-mode/table-mode/defect-reject.md) - 测试人员驳回修复
    - [通过缺陷](defect/display-mode/table-mode/defect-pass.md) - 测试人员验证通过
    - [开启缺陷](defect/display-mode/table-mode/defect-reopen.md) - 重新开启已关闭的缺陷
    - [关闭缺陷](defect/display-mode/table-mode/defect-close.md) - 关闭缺陷
    - [删除缺陷](defect/display-mode/table-mode/defect-delete.md) - 删除缺陷记录
    - [新建评论](defect/display-mode/table-mode/defect-comment.md) - 对缺陷添加评论和讨论
  - Excel模式
    - [Excel模式介绍](defect/display-mode/excel-mode/excel-mode-intro.md) - Excel 模式概述与适用场景
    - [新建缺陷](defect/display-mode/excel-mode/defect-create.md) - 空白行填必填项自动创建
    - [修改缺陷](defect/display-mode/excel-mode/defect-edit.md) - 单元格直接编辑实时保存
    - [删除缺陷](defect/display-mode/excel-mode/defect-delete.md) - 选中行按 Del 键删除
- [导入缺陷](defect/defect-import.md) - 批量导入缺陷数据
- [导出缺陷](defect/defect-export.md) - 导出缺陷为 Excel 文件

## 缺陷管理与其他模块的关系

### 与测试用例的关系

- 缺陷可以关联测试用例
- 记录在哪个测试步骤发现的问题
- 用例执行失败时可直接创建缺陷

### 与测试计划的关系

- 执行测试计划时发现问题创建缺陷
- 缺陷修复后需要在测试计划中回归验证

### 与交付物的关系

- 缺陷必须关联到某个交付物
- 按交付物统计缺陷分布
- 评估各模块的质量状况

## 键盘快捷键

通用说明见 [键盘快捷键](../../advanced/keyboard-shortcuts.md)。抽屉、工具弹框等子场景见 [Table模式介绍](defect/display-mode/table-mode/table-mode-intro.md#键盘快捷键) 与 [Excel模式介绍](defect/display-mode/excel-mode/excel-mode-intro.md#键盘快捷键)。

在缺陷列表一级界面（无抽屉/弹框/下拉）：

| 方式 | 说明 |
|------|------|
| **空格** → 字母 | 打开动作命令面板 |
| 按住 **⌘/Ctrl** → 字母 | 工具栏按钮浮层徽标 |

| 字母 | 动作 |
|------|------|
| S | 聚焦查询（**← / →** 在类型、状态、处理人、名称/版本间切换；最右 **→** 进入工具栏首按钮） |
| E | 新建缺陷菜单（**↑↓** 选新建/导入/导出，**Enter** 或 **空格** 执行并收起菜单，**Esc** 关闭） |
| I | 打开统计面板 |
| B | 上一页 |
| P | 下一页 |

按住 **⌘/Ctrl** 时，Table / Excel 模式下**当前可见行**序号列会显示动态字母（优先 `1`–`9`），按字母打开该缺陷详情。

**查询区**：焦点在查询条内时，**← / →** 在「类型 → 状态 → 处理人 → 名称/版本」间切换；最右一项 **→** 进入右侧工具栏首按钮，该按钮 **←** 回到名称/版本。按 **S** 默认聚焦最后一项，**Esc** 退出查询导航。

## 常见问题

### Q: 缺陷可以关联多个测试用例吗？

A: 目前一个缺陷只能关联一个测试用例。如果需要关联多个，可以在缺陷描述中添加其他用例的链接。

### Q: 如何查看我负责的缺陷？

A: 可以使用页标签功能，创建一个"我负责的"标签，设置筛选条件为"处理人=我"。

### Q: 缺陷可以导出吗？

A: 可以。在缺陷列表页面点击「导出缺陷」按钮，可以导出为 Excel 格式。

### Q: 如何查看缺陷的历史记录？

A: 打开缺陷详情页，可以看到缺陷的所有操作历史，包括创建、修改、状态变更等。
