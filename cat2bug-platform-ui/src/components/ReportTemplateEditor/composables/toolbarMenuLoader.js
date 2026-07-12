import { MarkdownTools } from '@/components/Cat2BugMarkdown/MarkdownTools'
import { markdownData } from '@/api/markdown/markdown'
import { injectPlanToolMenu } from './planToolMenus'

/**
 * Build full toolbar menus (MarkdownTools + dynamic plan menus).
 * @param {number|string} projectId
 */
export async function loadReportToolbarMenus(projectId) {
  let tools = MarkdownTools().filter((t) => t.name !== 'save' && t.name !== 'clear')
  if (!projectId) {
    return tools
  }
  try {
    const res = await markdownData(projectId)
    const planList = res.data?.plan?.list || []
    tools = injectPlanToolMenu(tools, planList)
  } catch (_e) {
    // plan menu optional when API unavailable
  }
  return tools
}
