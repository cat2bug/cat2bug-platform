# 测试事件报告

## 文档标识

本文档的元信息（编号、发布方、审批与状态等），便于版本管理与归档。

| 项 | 内容 |
|----|------|
| 唯一标识 | （人工填写） |
| 发布组织 | （人工填写） |
| 批准权限 | （人工填写） |
| 文档状态 | （人工填写） |

## 概述

测试事件指测试过程中需调查的任何问题（如实际结果与预期不符）。Cat2Bug 中每条缺陷对应一次可跟踪的事件记录。  
项目：**${api.project.projectName}** · 计划：**${api.plan.planName[1]}**（${api.plan.planVersion[1]}）

---

## 单条事件报告模板

> 复制以下小节用于单条事件；或直接使用文末**缺陷清单表**从系统导出。

### 时间信息

（人工填写）观察日期/时间：________

> 系统记录可对照缺陷表中的 `createTime` / `updateTime` 列。

### 报告人

（人工填写）姓名与职务：________

> 系统记录可对照缺陷表中的 `createBy` 列。

### 上下文

| 项 | 内容 |
|----|------|
| 被测项 / 版本 | （人工填写）________ |
| 模块 | 见缺陷表 `moduleName` / `moduleVersion` |
| 测试级别/类型 | （人工填写）________ |
| 规程 / 用例标识 | （人工填写）________ |
| 环境 / 数据补充 | （人工填写）________ |

### 事件描述

（人工填写）详细描述、是否可重现、复现步骤：________

> 系统字段：`defectDescribe`；附件与截图：`imgList`、`annexList`。

### 报告人严重度评估

（人工填写）技术与业务影响、修复工作量估计：________

> 系统字段：`defectLevel`。

### 报告人优先级评估

（人工填写）修复紧迫程度分类：________

### 风险

**【待填写｜无 DSL 数据源】** 新风险或既有风险状态变更：________

### 事件状态

（人工填写）当前生命周期状态：________

> 系统字段：`defectStateName`（如处理中、已关闭、已驳回等）。

---

## 项目缺陷 / 事件清单（工具导出）

下列表格为当前项目缺陷列表，列与 Cat2Bug 报告 DSL 缺陷表默认列一致：

$table{api.defect.list[projectNum,defectTypeName,defectLevel,defectName,defectStateName,moduleName,moduleVersion,defectDescribe,createBy,createTime,updateBy,updateTime,imgList,annexList]}

### 计划维度缺陷统计

| 指标 | 数值 |
|------|------|
| 缺陷总数 | ${api.plan.defectCount[1]} |
| 处理中 | ${api.plan.defectProcessingStateCount[1]} |
| 已关闭 | ${api.plan.defectCloseStateCount[1]} |
| 紧急 | ${api.plan.defectLevelUrgentCount[1]} |

---

**参考文档：** ISO/IEC/IEEE 29119-3 测试事件报告信息项；GB/T 38634.3 对照。本模版按项目事件/缺陷报告独立编写，不依赖标准章节编号。
