/**
 * Report body toolbar menus: Markdown formatting only (no project/case/defect variables).
 */
import { MarkdownTools } from '@/components/Cat2BugMarkdown/MarkdownTools'
import { filterReportContentToolbarTools } from './reportContentToolbarFilter'

export {
  REPORT_CONTENT_EXCLUDED_TOOL_NAMES,
  filterReportContentToolbarTools
} from './reportContentToolbarFilter'

/**
 * Static markdown toolbar for report body (same ToolsMenu shape as template, without variables).
 * @returns {Array<object>}
 */
export function loadReportContentToolbarMenus() {
  return filterReportContentToolbarTools(MarkdownTools())
}
