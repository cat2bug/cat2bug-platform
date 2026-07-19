import { markdownSemanticallyEqual } from '@/components/ReportTemplateEditor/milkdown/model-sync'

/**
 * 报告正文是否与已保存版本不一致（用于自动保存 / 离开前提示）。
 * 使用语义比较，忽略尾部空行等 Milkdown 往返造成的无意义差异，
 * 避免定时器误触发保存并写入历史。
 * @param {string} draft
 * @param {string} saved
 * @returns {boolean}
 */
export function isReportContentDirty(draft, saved) {
  return !markdownSemanticallyEqual(draft, saved)
}

/**
 * 历史回复评论的 moduleType
 */
export const REPORT_HISTORY_COMMENT_MODULE = 'report_history'

/**
 * 自动保存间隔（毫秒）：3 分钟；内容未变时不请求保存、不写历史
 */
export const REPORT_AUTOSAVE_INTERVAL_MS = 3 * 60 * 1000
