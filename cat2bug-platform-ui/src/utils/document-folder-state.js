/**
 * 从 MD 编辑页返回文档列表时的「一次性」文件夹恢复。
 * 写入后仅消费一次并清除，避免侧栏再进文档管理仍停在旧文件夹。
 */

function storageKey(projectId) {
  return `cat2bug_document_return_folder_${projectId || 0}`
}

/**
 * @param {number|string} projectId
 * @param {{ docId?: number, docPid?: number, docName?: string, docType?: number }|null} folder
 */
export function setDocumentReturnFolder(projectId, folder) {
  try {
    const key = storageKey(projectId)
    if (!folder || !folder.docId || folder.docId <= 0) {
      sessionStorage.removeItem(key)
      return
    }
    sessionStorage.setItem(key, JSON.stringify({
      docId: folder.docId,
      docPid: folder.docPid != null ? folder.docPid : 0,
      docName: folder.docName || '',
      docType: folder.docType != null ? folder.docType : 0
    }))
  } catch (_) { /* ignore */ }
}

/**
 * 读取并清除；无待恢复文件夹时返回 null。
 * @param {number|string} projectId
 * @returns {{ docId: number, docPid: number, docName: string, docType: number }|null}
 */
export function consumeDocumentReturnFolder(projectId) {
  try {
    // 清掉旧版持久化 key，避免侧栏进入仍停在上次文件夹
    sessionStorage.removeItem(`cat2bug_document_folder_${projectId || 0}`)
    const key = storageKey(projectId)
    const raw = sessionStorage.getItem(key)
    sessionStorage.removeItem(key)
    if (!raw) return null
    const folder = JSON.parse(raw)
    if (!folder || !folder.docId || folder.docId <= 0) return null
    return folder
  } catch (_) {
    return null
  }
}
