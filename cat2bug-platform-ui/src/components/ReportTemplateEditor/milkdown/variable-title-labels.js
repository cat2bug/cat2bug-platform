import i18n from '@/utils/i18n/i18n'

/**
 * path (without api. prefix variants) → i18n key for hover title.
 * Covers toolbar insert labels; unknown paths fall back to last segment.
 */
const PATH_I18N_KEYS = {
  'api.project.projectName': 'project.name',
  'project.projectName': 'project.name',
  'api.case.total': 'case.total',
  'case.total': 'case.total',
  'api.plan.planName': 'plan.name',
  'plan.planName': 'plan.name',
  'api.plan.planVersion': 'plan.version',
  'plan.planVersion': 'plan.version',
  'api.plan.failCount': 'plan.fail-count',
  'plan.failCount': 'plan.fail-count',
  'api.plan.passCount': 'plan.pass-count',
  'plan.passCount': 'plan.pass-count',
  'api.plan.unexecutedCount': 'plan.unexecuted-count',
  'plan.unexecutedCount': 'plan.unexecuted-count',
  'api.plan.executedCount': 'plan.executed-count',
  'plan.executedCount': 'plan.executed-count',
  'api.plan.itemTotal': 'plan.item-total',
  'plan.itemTotal': 'plan.item-total',
  'api.plan.defectCount': 'plan.defect-count',
  'plan.defectCount': 'plan.defect-count',
  'api.plan.createDefectCountByTester': 'plan.create-defect-count-by-tester',
  'plan.createDefectCountByTester': 'plan.create-defect-count-by-tester',
  'api.plan.createDefectCountByOutsider': 'plan.create-defect-count-by-outsider',
  'plan.createDefectCountByOutsider': 'plan.create-defect-count-by-outsider',
  'api.plan.defectProcessingStateCount': 'plan.defect-processing-count',
  'plan.defectProcessingStateCount': 'plan.defect-processing-count',
  'api.plan.defectAuditStateCount': 'plan.defect-audit-count',
  'plan.defectAuditStateCount': 'plan.defect-audit-count',
  'api.plan.defectRejectedStateCount': 'plan.defect-rejected-count',
  'plan.defectRejectedStateCount': 'plan.defect-rejected-count',
  'api.plan.defectCloseStateCount': 'plan.defect-close-count',
  'plan.defectCloseStateCount': 'plan.defect-close-count',
  'api.plan.defectHistoryPassCount': 'plan.defect-history-pass-count',
  'plan.defectHistoryPassCount': 'plan.defect-history-pass-count',
  'api.plan.defectLevelUrgentCount': 'plan.defect-level-urgent-count',
  'plan.defectLevelUrgentCount': 'plan.defect-level-urgent-count',
  'api.plan.defectLevelHeightCount': 'plan.defect-level-height-count',
  'plan.defectLevelHeightCount': 'plan.defect-level-height-count',
  'api.plan.defectLevelMiddleCount': 'plan.defect-level-middle-count',
  'plan.defectLevelMiddleCount': 'plan.defect-level-middle-count',
  'api.plan.defectLevelLowCount': 'plan.defect-level-low-count',
  'plan.defectLevelLowCount': 'plan.defect-level-low-count',
  'api.plan.defectRestartCount': 'plan.defect-restart-count',
  'plan.defectRestartCount': 'plan.defect-restart-count',
  'api.plan.defectUseHourTime': 'plan.defect-use-hour-time',
  'plan.defectUseHourTime': 'plan.defect-use-hour-time',
  'api.plan.defectDiscoveryRate': 'plan.defect-discovery-rate',
  'plan.defectDiscoveryRate': 'plan.defect-discovery-rate',
  'api.plan.defectRepairRate': 'plan.defect-repair-rate',
  'plan.defectRepairRate': 'plan.defect-repair-rate',
  'api.plan.defectDensity': 'plan.defect-density',
  'plan.defectDensity': 'plan.defect-density',
  'api.plan.defectDetectionRate': 'plan.defect-detection-rate',
  'plan.defectDetectionRate': 'plan.defect-detection-rate',
  'api.plan.defectSeverityRate': 'plan.defect-severity-rate',
  'plan.defectSeverityRate': 'plan.defect-severity-rate',
  'api.plan.defectRestartRate': 'plan.defect-restart-rate',
  'plan.defectRestartRate': 'plan.defect-restart-rate',
  'api.plan.defectEscapeRate': 'plan.defect-escape-rate',
  'plan.defectEscapeRate': 'plan.defect-escape-rate',
  'api.plan.defectRepairAvgHour': 'plan.defect-repair-avg-hour',
  'plan.defectRepairAvgHour': 'plan.defect-repair-avg-hour',
  'api.plan.moduleCount': 'plan.module-count',
  'plan.moduleCount': 'plan.module-count'
}

/**
 * @param {string} path
 * @returns {string}
 */
export function pathLastSegment(path) {
  const parts = String(path || '').split('.').filter(Boolean)
  return parts.length ? parts[parts.length - 1] : String(path || '')
}

/**
 * Resolve hover title for a variable path.
 * @param {string} path e.g. api.project.projectName
 * @param {{ planIndex?: number }} [opts]
 * @returns {string}
 */
export function resolveVariableTitle(path, opts = {}) {
  const key = PATH_I18N_KEYS[path]
  let title = ''
  if (key) {
    try {
      const translated = i18n.t(key)
      if (translated && translated !== key) {
        title = String(translated)
      }
    } catch (_e) {
      title = ''
    }
  }
  if (!title) {
    title = pathLastSegment(path)
  }
  const planIndex = Number(opts.planIndex)
  if (Number.isFinite(planIndex) && planIndex > 0 && String(path || '').includes('plan')) {
    return `${title} [${planIndex}]`
  }
  return title
}

export { PATH_I18N_KEYS }
