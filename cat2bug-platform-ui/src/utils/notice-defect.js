/** 从通知 markdown 中解析缺陷 ID（链接参数 defectId=） */
export function parseDefectIdFromNoticeContent(content) {
  if (content == null || content === '') return null
  const match = String(content).match(/defectId=(\d+)/)
  return match ? Number(match[1]) : null
}

/** 是否为缺陷分组通知 */
export function isDefectGroupNotice(notice) {
  return !!(notice && notice.groupName === 'defect')
}
