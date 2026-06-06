/** 缺陷新建/编辑抽屉：序列化表单状态，用于关闭前判断是否有未保存修改 */

function sortObjectKeys(obj) {
  if (!obj || typeof obj !== 'object' || Array.isArray(obj)) return obj
  const sorted = {}
  Object.keys(obj).sort().forEach((k) => {
    sorted[k] = obj[k]
  })
  return sorted
}

function normalizeDefectFormForCompare(form) {
  if (!form || typeof form !== 'object') return {}
  const f = { ...form }
  if (Array.isArray(f.handleBy)) {
    f.handleBy = f.handleBy.map((id) => Number(id)).sort((a, b) => a - b)
  }
  if (f.customFields && typeof f.customFields === 'object') {
    f.customFields = sortObjectKeys(f.customFields)
  }
  return f
}

export function serializeDefectFormCloseState(form, planTimeRange) {
  const range = Array.isArray(planTimeRange) ? planTimeRange.map(String) : []
  return JSON.stringify({
    form: normalizeDefectFormForCompare(form),
    planTimeRange: range
  })
}
