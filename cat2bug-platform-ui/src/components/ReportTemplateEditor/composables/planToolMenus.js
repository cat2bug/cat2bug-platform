import i18n from '@/utils/i18n/i18n'
import { normalizePipeTableMarkdown } from '../dsl/normalize-pipe-table'

/**
 * Build plan statistics mega-table markdown (from Cat2BugMarkdown.addDynamicToolMenu).
 * Output is normalized GFM so Milkdown/Crepe can edit rows/columns visually.
 * @param {number} planNumber
 */
export function buildPlanStatisticsTable(planNumber) {
  const n = planNumber
  const legacy = `|项目名称|\${api.project.projectName}|||||||||||||||||
|测试计划|\${api.plan.planName[${n}]}|||||||||||||||||
测试版本号|\${api.plan.planVersion[${n}]}||用例通过数量|\${api.plan.passCount[${n}]} 个|用例未通过数量|\${api.plan.failCount[${n}]} 个|用例未执行数量|\${api.plan.unexecutedCount[${n}]} 个||||||||||
|||||||
|-|-|-|-|
|度量指标数据如下:||||||||||||||||||
|缺陷发现率（单位：%）||缺陷修复率（单位：%）||缺陷探测率（单位：%）||缺陷密度（单位：个）||缺陷严重率（单位：%）||缺陷重开率（单位：%）||缺陷逃逸率（单位：个）||用例执行效率（单位：个）||缺陷修复平均时长（单位：h）||
|本轮已发现的缺陷数量|本轮已执行的用例数量|本轮已修复的缺陷数量|本轮发现的总的缺陷数量|测试或探测者发现的缺陷数量|客户反馈的缺陷数量|缺陷数量|代码行数或功能点的数 代码行数或功能点的数量|本计划所发现的严重缺陷数量|本轮已发现的缺陷总数|本轮重新打开的缺陷数量|本轮已修复的缺陷数量|用户反馈或实际使用中发现的缺陷数量|测试发现的缺陷数|本轮迭代执行的测试用例数量|执行本轮迭代测试耗时|缺陷数量|统计周期内缺陷的总解决时长
|\${api.plan.defectCount[${n}]}|\${api.plan.executedCount[${n}]}个|\${api.plan.defectCloseStateCount[${n}]}| \${api.plan.defectCount[${n}]}|\${api.plan.createDefectCountByTester[${n}]}|\${api.plan.createDefectCountByOutsider[${n}]}|\${api.plan.defectCount[${n}]}|\${api.plan.moduleCount[${n}]}|\${api.plan.defectLevelUrgentCount[${n}]}|\${api.plan.defectCount[${n}]}|\${api.plan.defectRestartCount[${n}]}|\${api.plan.defectHistoryPassCount[${n}]}|\${api.plan.createDefectCountByOutsider[${n}]}|\${api.plan.defectCount[${n}]}|\${api.plan.defectCount[${n}]}| |\${api.plan.defectCount[${n}]}|\${api.plan.defectUseHourTime[${n}]}h|

|\${api.plan.defectDiscoveryRate[${n}]}||\${api.plan.defectRepairRate[${n}]}||\${api.plan.defectDetectionRate[${n}]}||\${api.plan.defectDensity[${n}]}个||\${api.plan.defectSeverityRate[${n}]}||\${api.plan.defectRestartRate[${n}]}||\${api.plan.defectEscapeRate[${n}]}|| ||\${api.plan.defectRepairAvgHour[${n}]}h||`
  return normalizePipeTableMarkdown(legacy)
}

function planFieldLine(i18nKey, field, planNumber) {
  return `${i18n.t(i18nKey)}: \${api.plan.${field}[${planNumber}]}`
}

/**
 * @param {{ planName: string, planNumber: number }} plan
 */
export function buildPlanSubMenu(plan) {
  const n = plan.planNumber
  return {
    icon: 'md-create-time',
    name: plan.planName,
    children: [
      {
        icon: 'mk-table',
        name: `plan.statistics.${n}`,
        content: buildPlanStatisticsTable(n)
      },
      { type: 'siding' },
      {
        icon: 'md-create-time',
        name: `plan.item.${n}`,
        children: [
          { icon: 'mk-total', name: `plan.name.${n}`, content: planFieldLine('plan.name', 'planName', n) },
          { icon: 'mk-total', name: `plan.version.${n}`, content: planFieldLine('plan.version', 'planVersion', n) },
          { icon: 'mk-total', name: `plan.fail-count.${n}`, content: planFieldLine('plan.fail-count', 'failCount', n) },
          { icon: 'mk-total', name: `plan.pass-count.${n}`, content: planFieldLine('plan.pass-count', 'passCount', n) },
          { icon: 'mk-total', name: `plan.unexecuted-count.${n}`, content: planFieldLine('plan.unexecuted-count', 'unexecutedCount', n) },
          { icon: 'mk-total', name: `plan.executed-count.${n}`, content: planFieldLine('plan.executed-count', 'executedCount', n) },
          { icon: 'mk-total', name: `plan.item-total.${n}`, content: planFieldLine('plan.item-total', 'itemTotal', n) }
        ]
      },
      {
        icon: 'mk-bug',
        name: `plan.defect.${n}`,
        children: [
          { icon: 'mk-total', name: `plan.defect-count.${n}`, content: planFieldLine('plan.defect-count', 'defectCount', n) },
          { type: 'siding' },
          {
            icon: 'mk-member',
            name: `member.${n}`,
            children: [
              { icon: 'mk-member', name: `plan.create-defect-count-by-tester.${n}`, content: planFieldLine('plan.create-defect-count-by-tester', 'createDefectCountByTester', n) },
              { icon: 'mk-member', name: `plan.create-defect-count-by-outsider.${n}`, content: planFieldLine('plan.create-defect-count-by-outsider', 'createDefectCountByOutsider', n) }
            ]
          },
          { type: 'siding' },
          {
            icon: 'mk-total',
            name: `defect.state.${n}`,
            children: [
              { icon: 'mk-total', name: `plan.defect-processing-count.${n}`, content: planFieldLine('plan.defect-processing-count', 'defectProcessingStateCount', n) },
              { icon: 'mk-total', name: `plan.defect-audit-count.${n}`, content: planFieldLine('plan.defect-audit-count', 'defectAuditStateCount', n) },
              { icon: 'mk-total', name: `plan.defect-rejected-count.${n}`, content: planFieldLine('plan.defect-rejected-count', 'defectRejectedStateCount', n) },
              { icon: 'mk-total', name: `plan.defect-close-count.${n}`, content: planFieldLine('plan.defect-close-count', 'defectCloseStateCount', n) },
              { icon: 'mk-total', name: `plan.defect-history-pass-count.${n}`, content: planFieldLine('plan.defect-history-pass-count', 'defectHistoryPassCount', n) }
            ]
          },
          { type: 'siding' },
          {
            icon: 'mk-level',
            name: `plan.defect-level-count.${n}`,
            children: [
              { name: `plan.defect-level-urgent-count.${n}`, content: planFieldLine('plan.defect-level-urgent-count', 'defectLevelUrgentCount', n) },
              { name: `plan.defect-level-height-count.${n}`, content: planFieldLine('plan.defect-level-height-count', 'defectLevelHeightCount', n) },
              { name: `plan.defect-level-middle-count.${n}`, content: planFieldLine('plan.defect-level-middle-count', 'defectLevelMiddleCount', n) },
              { name: `plan.defect-level-low-count.${n}`, content: planFieldLine('plan.defect-level-low-count', 'defectLevelLowCount', n) }
            ]
          },
          { type: 'siding' },
          { icon: 'mk-total', name: `plan.defect-restart-count.${n}`, content: planFieldLine('plan.defect-restart-count', 'defectRestartCount', n) },
          { icon: 'md-create-time', name: `plan.defect-use-hour-time.${n}`, content: planFieldLine('plan.defect-use-hour-time', 'defectUseHourTime', n) },
          { icon: 'mk-rate', name: `plan.defect-discovery-rate.${n}`, content: planFieldLine('plan.defect-discovery-rate', 'defectDiscoveryRate', n) },
          { icon: 'mk-rate', name: `plan.defect-repair-rate.${n}`, content: planFieldLine('plan.defect-repair-rate', 'defectRepairRate', n) },
          { icon: 'mk-value', name: `plan.defect-density.${n}`, content: planFieldLine('plan.defect-density', 'defectDensity', n) },
          { icon: 'mk-rate', name: `plan.defect-detection-rate.${n}`, content: planFieldLine('plan.defect-detection-rate', 'defectDetectionRate', n) },
          { icon: 'mk-rate', name: `plan.defect-severity-rate.${n}`, content: planFieldLine('plan.defect-severity-rate', 'defectSeverityRate', n) },
          { icon: 'mk-rate', name: `plan.defect-restart-rate.${n}`, content: planFieldLine('plan.defect-restart-rate', 'defectRestartRate', n) },
          { icon: 'mk-rate', name: `plan.defect-escape-rate.${n}`, content: planFieldLine('plan.defect-escape-rate', 'defectEscapeRate', n) },
          { icon: 'md-create-time', name: `plan.defect-repair-avg-hour.${n}`, content: planFieldLine('plan.defect-repair-avg-hour', 'defectRepairAvgHour', n) }
        ]
      },
      {
        icon: 'cascader',
        name: `plan.module-count.${n}`,
        content: planFieldLine('plan.module-count', 'moduleCount', n)
      }
    ]
  }
}

/**
 * @param {Array<{ planName: string, planNumber: number }>} planList
 */
export function buildPlanToolMenu(planList) {
  if (!planList || !planList.length) {
    return null
  }
  return {
    icon: 'md-create-time',
    name: 'plan',
    children: planList.map(buildPlanSubMenu)
  }
}

/**
 * Insert plan menu after case menu in toolbar array (mutable copy).
 * @param {Array} tools
 * @param {Array} planList
 */
export function injectPlanToolMenu(tools, planList) {
  const planMenu = buildPlanToolMenu(planList)
  if (!planMenu) {
    return tools
  }
  const next = [...tools]
  const caseIdx = next.findIndex((t) => t.name === 'case')
  if (caseIdx === -1) {
    next.push(planMenu)
  } else {
    next.splice(caseIdx + 1, 0, planMenu)
  }
  return next
}
