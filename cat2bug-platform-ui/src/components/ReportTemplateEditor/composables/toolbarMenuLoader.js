import { MarkdownTools } from '@/components/Cat2BugMarkdown/MarkdownTools'
import { markdownData } from '@/api/markdown/markdown'
import { injectPlanToolMenu } from './planToolMenus'

/** Import / save actions prepended to the report template toolbar. */
export function buildReportFileCommands() {
  return [
    {
      icon: 'import',
      name: 'import-markdown',
      type: 'command'
    },
    {
      icon: 'mk-save',
      name: 'save',
      type: 'command'
    },
    {
      type: 'siding'
    }
  ]
}

function stripTrailingSiding(tools) {
  const next = [...tools]
  while (next.length && next[next.length - 1].type === 'siding') {
    next.pop()
  }
  return next
}

/**
 * Build full toolbar menus (MarkdownTools + dynamic plan menus).
 * @param {number|string} projectId
 */
export async function loadReportToolbarMenus(projectId) {
  let tools = stripTrailingSiding(
    MarkdownTools().filter((t) => t.name !== 'save' && t.name !== 'clear')
  )
  tools = [...buildReportFileCommands(), ...tools]
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
