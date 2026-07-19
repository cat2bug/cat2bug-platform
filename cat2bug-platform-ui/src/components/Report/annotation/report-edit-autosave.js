/**
 * 报告正文是否与已保存版本不一致（用于自动保存 / 离开前提示）
 * @param {string} draft
 * @param {string} saved
 * @returns {boolean}
 */
export function isReportContentDirty(draft, saved) {
  return (draft || '') !== (saved || '')
}

/**
 * 历史回复评论的 moduleType
 */
export const REPORT_HISTORY_COMMENT_MODULE = 'report_history'

/**
 * 自动保存间隔（毫秒）
 */
export const REPORT_AUTOSAVE_INTERVAL_MS = 60000
