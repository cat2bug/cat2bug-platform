# 测试完成报告（测试总结报告）

## 文档标识

本文档的元信息（编号、发布方、审批与状态等），便于版本管理与归档。

| 项 | 内容 |
|----|------|
| 唯一标识 | （人工填写） |
| 发布组织 | （人工填写） |
| 批准权限 | （人工填写） |
| 文档状态 | （人工填写） |
| 变更历史 | （人工填写） |

## 概述

本报告总结已完成的测试活动，适用于项目整体或特定测试级别/类型。  
项目：**${api.project.projectName}**  
测试计划：**${api.plan.planName[1]}**（版本 ${api.plan.planVersion[1]}）

$image["style":"width:200px;height:200px;"]{api.project.projectIcon}

## 已执行测试摘要

**【待填写】** 测试范围、测试级别/类型、执行约束（如环境可用性）的叙述性摘要：________

### 执行结果概览

| 指标 | 数值 |
|------|------|
| 项目用例总数 | ${api.case.total} |
| 计划项总数 | ${api.plan.itemTotal[1]} |
| 已执行用例数 | ${api.plan.executedCount[1]} |
| 通过 | ${api.plan.passCount[1]} |
| 未通过 | ${api.plan.failCount[1]} |
| 未执行 | ${api.plan.unexecutedCount[1]} |
| 关联模块数 | ${api.plan.moduleCount[1]} |

## 相对计划的偏差

**【待填写｜无 DSL 数据源】** 与计划测试的偏差、原因及风险处理引用：________

## 测试完成评价

**【待填写】** 是否达到完成/出口准则、发布建议及决策依据：________

## 阻碍进展的因素

**【待填写】** 测试期间阻碍进展的因素及解决情况：________

## 测试度量

### 缺陷汇总

| 度量项 | 数值 |
|--------|------|
| 缺陷总数 | ${api.plan.defectCount[1]} |
| 已关闭 | ${api.plan.defectCloseStateCount[1]} |
| 处理中 | ${api.plan.defectProcessingStateCount[1]} |
| 审核中 | ${api.plan.defectAuditStateCount[1]} |
| 已驳回 | ${api.plan.defectRejectedStateCount[1]} |
| 紧急 / 高 / 中 / 低 | ${api.plan.defectLevelUrgentCount[1]} / ${api.plan.defectLevelHeightCount[1]} / ${api.plan.defectLevelMiddleCount[1]} / ${api.plan.defectLevelLowCount[1]} |

### 质量比率

| 度量项 | 数值 |
|--------|------|
| 缺陷发现率 | ${api.plan.defectDiscoveryRate[1]} |
| 缺陷修复率 | ${api.plan.defectRepairRate[1]} |
| 缺陷密度 | ${api.plan.defectDensity[1]} |
| 缺陷检出率 | ${api.plan.defectDetectionRate[1]} |
| 缺陷严重率 | ${api.plan.defectSeverityRate[1]} |
| 缺陷重启率 | ${api.plan.defectRestartRate[1]} |
| 缺陷逃逸率 | ${api.plan.defectEscapeRate[1]} |
| 平均修复时长（小时） | ${api.plan.defectRepairAvgHour[1]} |
| 缺陷总耗时（小时） | ${api.plan.defectUseHourTime[1]} |

**【待填写】** 其他汇总度量（覆盖率等）：________

## 残余风险

**【待填写｜无 DSL 数据源】** 测试结束后仍存在的风险及处置：________

## 测试交付物

**【待填写】** 已交付的测试文档与记录清单（计划、说明、状态/完成报告、事件报告等）：________

## 可复用测试资产

**【待填写】** 可归档复用的用例、数据、脚本、环境配置等：________

> 本项目用例规模：${api.case.total} 条（详细清单见《测试说明》或附录用例表）。

## 经验教训

**【待填写】** 过程改进建议与经验总结：________

## 附录 A：缺陷清单

$table{api.defect.list[projectNum,defectTypeName,defectLevel,defectName,defectStateName,moduleName,moduleVersion,defectDescribe,createBy,createTime,updateBy,updateTime,imgList,annexList]}

---

**参考文档：** ISO/IEC/IEEE 29119-3 测试完成报告信息项；GB/T 38634.3 对照。本模版按项目报告独立编写，不依赖标准章节编号。
