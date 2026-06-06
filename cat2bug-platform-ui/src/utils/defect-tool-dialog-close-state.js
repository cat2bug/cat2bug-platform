/** 缺陷工具弹框：序列化表单状态，用于 Esc/关闭前判断是否有未保存修改 */

function normalizeToolDialogForm(form) {
  if (!form || typeof form !== 'object') return {}
  const f = { ...form }
  delete f.defectId
  if (Array.isArray(f.receiveBy)) {
    f.receiveBy = f.receiveBy
      .map((id) => Number(id))
      .filter((id) => !Number.isNaN(id))
      .sort((a, b) => a - b)
  }
  return f
}

export function serializeToolDialogFormCloseState(form) {
  return JSON.stringify(normalizeToolDialogForm(form))
}
