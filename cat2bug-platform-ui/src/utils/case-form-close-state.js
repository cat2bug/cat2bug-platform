/** 用例新建/编辑抽屉：序列化表单状态，用于关闭前判断是否有未保存修改 */

function normalizeCaseFormForCompare(form) {
  if (!form || typeof form !== 'object') return {}
  const f = { ...form }
  if (Array.isArray(f.caseStep)) {
    f.caseStep = f.caseStep.map((step) => ({ ...step }))
  }
  if (Array.isArray(f.focusList)) {
    f.focusList = f.focusList.map((id) => Number(id)).filter((id) => !Number.isNaN(id)).sort((a, b) => a - b)
  }
  return f
}

export function serializeCaseFormCloseState(form, isCreateNextCase) {
  return JSON.stringify({
    form: normalizeCaseFormForCompare(form),
    isCreateNextCase: !!isCreateNextCase
  })
}
