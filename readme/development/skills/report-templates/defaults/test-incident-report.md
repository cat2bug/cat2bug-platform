<!--
  标准：ISO/IEC/IEEE 29119-3 §8.11（GB/T 38634.3 对照）
  Taxonomy：standalone — 单事件字段 + 项目级缺陷清单表
-->

# 测试事件报告

## 文档标识（Clause 5 公共元素）

| 项 | 内容 |
|----|------|
| 唯一标识 | （人工填写） |
| 发布组织 | （人工填写） |
| 批准权限 | （人工填写） |
| 文档状态 | （人工填写） |

## 0. 概述（8.11.1）

测试事件指测试过程中需调查的任何问题（如实际结果与预期不符）。Cat2Bug 中每条缺陷对应一次可跟踪的事件记录。  
项目：**${api.project.projectName}** · 计划：**${api.plan.planName[1]}**（${api.plan.planVersion[1]}）

---

## 单条事件报告模板（8.11.2–8.11.9）

> 复制以下小节用于单条事件；或直接使用文末**缺陷清单表**从系统导出。

### 1. 时间信息（8.11.2）

（人工填写）观察日期/时间：________

> 系统记录可参考缺陷表中的 `createTime` / `updateTime` 列。

### 2. 报告人（8.11.3）

（人工填写）姓名与职务：________

> 系统记录可参考缺陷表中的 `createBy` 列。

### 3. 上下文（8.11.4）

| 项 | 内容 |
|----|------|
| 被测项 / 版本 | （人工填写）________ |
| 模块 | 见缺陷表 `moduleName` / `moduleVersion` |
| 测试级别/类型 | （人工填写）________ |
| 规程 / 用例标识 | （人工填写）________ |
| 环境 / 数据补充 | （人工填写）________ |

### 4. 事件描述（8.11.5）

（人工填写）详细描述、是否可重现、复现步骤：________

> 系统字段：`defectDescribe`；附件与截图：`imgList`、`annexList`。

### 5. 报告人严重度评估（8.11.6）

（人工填写）技术与业务影响、修复工作量估计：________

> 系统字段：`defectLevel`。

### 6. 报告人优先级评估（8.11.7）

（人工填写）修复紧迫程度分类：________

### 7. 风险（8.11.8）

**【待填写｜无 DSL 数据源】** 新风险或既有风险状态变更：________

### 8. 事件状态（8.11.9）

（人工填写）当前生命周期状态：________

> 系统字段：`defectStateName`（如处理中、已关闭、已驳回等）。

---

## 项目缺陷 / 事件清单（工具导出）

下列表格为当前项目缺陷列表，列与 Cat2Bug 报告 DSL 缺陷表默认列一致：

$table{api.defect.list[projectNum,defectTypeName,defectLevel,defectName,defectStateName,moduleName,moduleVersion,defectDescribe,createBy,createTime,updateBy,updateTime,imgList,annexList]}

### 计划维度缺陷统计（参考）

| 指标 | 数值 |
|------|------|
| 缺陷总数 | ${api.plan.defectCount[1]} |
| 处理中 | ${api.plan.defectProcessingStateCount[1]} |
| 已关闭 | ${api.plan.defectCloseStateCount[1]} |
| 紧急 | ${api.plan.defectLevelUrgentCount[1]} |
