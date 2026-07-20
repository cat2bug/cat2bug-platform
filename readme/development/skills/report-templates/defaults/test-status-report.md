<!--
  标准：ISO/IEC/IEEE 29119-3 §7.3（GB/T 38634.3 对照）
  Taxonomy：standalone — 7.3.2–7.3.8 combined sections
-->

# 测试状态报告

## 文档标识（Clause 5 公共元素）

| 项 | 内容 |
|----|------|
| 唯一标识 | （人工填写） |
| 发布组织 | （人工填写） |
| 批准权限 | （人工填写） |
| 文档状态 | （人工填写） |
| 变更历史 | （人工填写） |

## 0. 概述（7.3.1）

本报告说明在特定报告期内的测试状态与进展。  
关联项目：**${api.project.projectName}**  
项目简介：${api.project.projectIntroduce}

$image["style":"width:200px;height:200px;"]{api.project.projectIcon}

关联测试计划：**${api.plan.planName[1]}**（版本 ${api.plan.planVersion[1]}）

## 1. 测试状态（7.3.2）

**【待填写｜无 DSL 数据源】** 相对进度总体判断（提前 / 按期 / 滞后）：________

**【待填写】** 状态说明与要点：________

## 2. 报告期（7.3.3）

**【待填写】** 报告期起止日期：________ 至 ________

## 3. 相对测试计划的进展（7.3.4）

**【待填写】** 相对《测试计划》的完成情况、偏差说明、补救措施及影响：________

### 3.1 计划执行摘要（DSL 可自动填充部分）

| 指标 | 数值 |
|------|------|
| 计划项总数 | ${api.plan.itemTotal[1]} |
| 已执行用例数 | ${api.plan.executedCount[1]} |
| 用例通过数 | ${api.plan.passCount[1]} |
| 用例未通过数 | ${api.plan.failCount[1]} |
| 用例未执行数 | ${api.plan.unexecutedCount[1]} |
| 项目用例总数（库） | ${api.case.total} |
| 关联模块数 | ${api.plan.moduleCount[1]} |

## 4. 阻碍进展的因素（7.3.5）

**【待填写】** 本报告期内阻碍测试进展的因素、已采取措施及未决问题：________

## 5. 测试度量（7.3.6）

### 5.1 缺陷与质量度量

| 度量项 | 数值 |
|--------|------|
| 缺陷总数 | ${api.plan.defectCount[1]} |
| 测试人员创建缺陷数 | ${api.plan.createDefectCountByTester[1]} |
| 非测试人员创建缺陷数 | ${api.plan.createDefectCountByOutsider[1]} |
| 处理中 | ${api.plan.defectProcessingStateCount[1]} |
| 审核中 | ${api.plan.defectAuditStateCount[1]} |
| 已驳回 | ${api.plan.defectRejectedStateCount[1]} |
| 已关闭 | ${api.plan.defectCloseStateCount[1]} |
| 历史通过 | ${api.plan.defectHistoryPassCount[1]} |
| 重启缺陷数 | ${api.plan.defectRestartCount[1]} |
| 缺陷耗时（小时） | ${api.plan.defectUseHourTime[1]} |

### 5.2 缺陷级别分布

| 级别 | 数量 |
|------|------|
| 紧急 | ${api.plan.defectLevelUrgentCount[1]} |
| 高 | ${api.plan.defectLevelHeightCount[1]} |
| 中 | ${api.plan.defectLevelMiddleCount[1]} |
| 低 | ${api.plan.defectLevelLowCount[1]} |

### 5.3 比率类度量

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

**【待填写】** 覆盖率、资源消耗等标准要求的其他度量（无对应 DSL）：________

## 6. 新增与变更的风险（7.3.7）

**【待填写｜无 DSL 数据源】** 本报告期识别的新风险及对既有风险的变更：________

## 7. 下期计划测试（7.3.8）

**【待填写】** 下一报告期计划开展的测试活动与里程碑：________

---

*项目创建时间：${api.project.createTime} · 更新时间：${api.project.updateTime}*
