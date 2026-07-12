/** Cat2Bug report template DSL grammar — aligned with markdown-it plugins in Cat2BugMarkdown/plugins/ */

export const DEFECT_TABLE_DEFAULT_COLUMNS = [
  'projectNum',
  'defectTypeName',
  'defectLevel',
  'defectName',
  'defectStateName',
  'moduleName',
  'moduleVersion',
  'defectDescribe',
  'createBy',
  'createTime',
  'updateBy',
  'updateTime',
  'imgList',
  'annexList'
]

export const CASE_TABLE_DEFAULT_COLUMNS = [
  'caseNum',
  'caseName',
  'moduleName',
  'casePreconditions',
  'caseExpect',
  'caseStep',
  'createBy',
  'createTime',
  'updateBy',
  'updateTime'
]

export const PROJECT_VARIABLE_FIELDS = [
  'projectName',
  'projectIcon',
  'createTime',
  'updateTime',
  'projectIntroduce'
]

export const PLAN_VARIABLE_FIELDS = [
  'planName',
  'planVersion',
  'passCount',
  'failCount',
  'unexecutedCount',
  'executedCount',
  'itemTotal',
  'defectCount',
  'createDefectCountByTester',
  'createDefectCountByOutsider',
  'defectProcessingStateCount',
  'defectAuditStateCount',
  'defectRejectedStateCount',
  'defectCloseStateCount',
  'defectHistoryPassCount',
  'defectLevelUrgentCount',
  'defectLevelHeightCount',
  'defectLevelMiddleCount',
  'defectLevelLowCount',
  'defectRestartCount',
  'defectUseHourTime',
  'defectDiscoveryRate',
  'defectRepairRate',
  'defectDensity',
  'defectDetectionRate',
  'defectSeverityRate',
  'defectRestartRate',
  'defectEscapeRate',
  'defectRepairAvgHour',
  'moduleCount'
]

export const DEFECT_TABLE_TOOLBAR_TOKEN =
  '$table{api.defect.list[projectNum,defectTypeName,defectLevel,defectName,defectStateName,moduleName,moduleVersion,defectDescribe,createBy,createTime,updateBy,updateTime,imgList,annexList]}'

/** Same as TablePlugin.js */
export const TABLE_TOKEN_RE =
  /\$table\{((?:\w|\.)+)(?:\[((?:(?:\w|\.)+(?::(?:\w|[\u3040-\uD7AF]|\.)+)*)(?:,(?:\w|\.)+(?::(?:\w|[\u3040-\uD7AF]|\.)+)*)*)\])?\}/

export const CARD_TOKEN_RE = /\$card\{((?:\w|\.)+)\}/

/** Same as ImagePlugin.js */
export const IMAGE_TOKEN_RE = /\$(?:img|image)((?:\[[^\]]+\])+)\{((?:\w|\.)+)\}/

/** Plan variables — PlanVariablePlugin.js */
export const PLAN_VARIABLE_TOKEN_RE = /\$\{((?:\w|\.)+)\[(\d+)\]\}/

/** Simple variables — VariablePlugin.js (${api.namespace.field}) */
export const VARIABLE_TOKEN_RE = /\$\{((?:\w|\.)+)\}/

export function parseTableColumns(spec) {
  if (!spec) {
    return []
  }
  return spec.split(',').map((part) => {
    const colon = part.indexOf(':')
    if (colon === -1) {
      return { field: part.trim(), label: null }
    }
    return {
      field: part.slice(0, colon).trim(),
      label: part.slice(colon + 1).trim()
    }
  })
}

export function formatTableColumns(columns) {
  return columns
    .map((col) => {
      if (col.label) {
        return `${col.field}:${col.label}`
      }
      return col.field
    })
    .join(',')
}
