/** AI 创建用例抽屉：序列化状态，用于关闭前判断是否有未保存内容 */

function normalizePrompt(prompt) {
  if (!prompt || typeof prompt !== 'object') return {}
  const text = prompt.prompt
  return {
    prompt: typeof text === 'string' ? (text.trim() || null) : (text || null),
    modelId: prompt.modelId || null,
    rowCount: prompt.rowCount != null && prompt.rowCount !== '' ? Number(prompt.rowCount) : null
  }
}

export function serializeCloudCaseDrawerCloseState(prompt, caseList) {
  const list = Array.isArray(caseList)
    ? caseList.map((item) => ({
      caseName: item.caseName || null,
      moduleId: item.moduleId || null,
      caseLevel: item.caseLevel || null
    }))
    : []
  return JSON.stringify({
    prompt: normalizePrompt(prompt),
    caseCount: list.length,
    cases: list
  })
}
