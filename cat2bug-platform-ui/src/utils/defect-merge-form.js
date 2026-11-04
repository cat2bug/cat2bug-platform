/** 合并抽屉：表单拷贝、单字段替换、主缺陷覆盖确认 */

function deepClone(value) {
  if (value === undefined || value === null) return value
  return JSON.parse(JSON.stringify(value))
}

export function planTimeRangeFromDefect(defect) {
  const range = []
  if (defect && defect.planStartTime) range.push(defect.planStartTime)
  if (defect && defect.planEndTime) range.push(defect.planEndTime)
  return range
}

/** 打开抽屉时的空表单基线（与 EditDefectDialog.reset 一致） */
export function createEmptyMergeForm() {
  return {
    defectId: null,
    defectType: null,
    defectName: null,
    defectDescribe: null,
    annexUrls: null,
    imgUrls: null,
    projectId: null,
    projectNum: null,
    testPlanId: null,
    caseId: null,
    dataSources: null,
    dataSourcesParams: null,
    moduleId: null,
    moduleVersion: null,
    createBy: null,
    updateTime: null,
    createTime: null,
    updateBy: null,
    defectState: null,
    caseStepId: null,
    handleBy: null,
    handleTime: null,
    defectLevel: 'middle',
    customFields: {}
  }
}

/** 将 getDefect 响应写入左侧可编辑表单 */
export function defectToMergeForm(defect) {
  const base = createEmptyMergeForm()
  if (!defect) return base
  const merged = { ...base, ...deepClone(defect) }
  if (!merged.customFields || typeof merged.customFields !== 'object') {
    merged.customFields = {}
  } else {
    merged.customFields = deepClone(merged.customFields)
  }
  return merged
}

export function resolveFocusFieldKeyFromOrderedField(field) {
  if (!field) return null
  if (field.kind === 'custom') return `custom:${field.fieldKey}`
  return field.formKey || null
}

export function applyMergeFieldToForm(form, planTimeRange, fieldKey, sourceDefect) {
  if (!form || !fieldKey || !sourceDefect) return
  if (fieldKey === 'planTime') {
    const next = planTimeRangeFromDefect(sourceDefect)
    if (Array.isArray(planTimeRange)) {
      planTimeRange.splice(0, planTimeRange.length, ...next)
    }
    return
  }
  if (fieldKey.startsWith('custom:')) {
    const key = fieldKey.slice(7)
    if (!form.customFields) form.customFields = {}
    const cf = sourceDefect.customFields || {}
    const val = cf[key]
    form.customFields[key] = val === undefined ? null : deepClone(val)
    return
  }
  if (Object.prototype.hasOwnProperty.call(sourceDefect, fieldKey)) {
    form[fieldKey] = deepClone(sourceDefect[fieldKey])
  }
}

function isBlankMergeValue(value) {
  if (value == null || value === '') return true
  if (Array.isArray(value) && value.length === 0) return true
  return false
}

/**
 * 左侧是否仍为「未填写」：忽略 projectId、默认优先级 middle、以及自定义字段的空/false/[] 默认值。
 * 用于首次点「主缺陷」时避免被表单控件默认值误判为需覆盖确认。
 */
export function isMergeFormEffectivelyEmpty(form, planTimeRange) {
  if (Array.isArray(planTimeRange) && planTimeRange.length > 0) return false
  if (!form || typeof form !== 'object') return true

  const keys = Object.keys(form)
  for (let i = 0; i < keys.length; i++) {
    const key = keys[i]
    if (key === 'customFields' || key === 'projectId' || key === 'defectId' || key === 'projectNum') {
      continue
    }
    if (
      key === 'createBy' ||
      key === 'updateBy' ||
      key === 'createTime' ||
      key === 'updateTime' ||
      key === 'defectState'
    ) {
      continue
    }
    const value = form[key]
    if (key === 'defectLevel' && (isBlankMergeValue(value) || value === 'middle')) {
      continue
    }
    if (!isBlankMergeValue(value)) return false
  }

  const customFields = form.customFields && typeof form.customFields === 'object' ? form.customFields : {}
  const customKeys = Object.keys(customFields)
  for (let i = 0; i < customKeys.length; i++) {
    const value = customFields[customKeys[i]]
    /* 布尔未开、空串、空数组、null 视为控件默认，不算用户已填 */
    if (value === false || isBlankMergeValue(value)) continue
    return false
  }
  return true
}

/**
 * 左侧是否已有内容（需覆盖确认）：已选主，或相对空基线有改动，且非「仅默认值」空表单。
 * @param {string|null} emptyBaseline serializeDefectFormCloseState(emptyForm, [])
 */
export function mergeLeftHasContent(masterDefectId, form, planTimeRange, emptyBaseline, serialize) {
  if (masterDefectId != null) return true
  if (isMergeFormEffectivelyEmpty(form, planTimeRange)) return false
  if (!serialize || !emptyBaseline) return true
  const current = serialize(form, planTimeRange)
  return current !== emptyBaseline
}

export function shouldConfirmMergeMasterOverwrite(
  masterDefectId,
  form,
  planTimeRange,
  emptyBaseline,
  serialize
) {
  return mergeLeftHasContent(masterDefectId, form, planTimeRange, emptyBaseline, serialize)
}
