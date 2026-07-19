/**
 * Filter MarkdownTools for static report body editing (no realtime variables).
 */

/** Realtime / template variable roots excluded from static report editing. */
export const REPORT_CONTENT_EXCLUDED_TOOL_NAMES = new Set([
  'project',
  'case',
  'defect',
  'variables-expand-collapse',
  'import-markdown'
])

function stripTrailingSiding(tools) {
  const next = [...tools]
  while (next.length && next[next.length - 1].type === 'siding') {
    next.pop()
  }
  return next
}

function collapseAdjacentSidings(tools) {
  const out = []
  for (const tool of tools) {
    if (tool && tool.type === 'siding') {
      if (out.length && out[out.length - 1].type === 'siding') {
        continue
      }
      out.push(tool)
      continue
    }
    out.push(tool)
  }
  while (out.length && out[0].type === 'siding') {
    out.shift()
  }
  return stripTrailingSiding(out)
}

/**
 * @param {Array<object>} tools
 * @returns {Array<object>}
 */
export function filterReportContentToolbarTools(tools) {
  const filtered = (tools || []).filter((tool) => {
    if (!tool) {
      return false
    }
    if (tool.type === 'siding') {
      return true
    }
    return !REPORT_CONTENT_EXCLUDED_TOOL_NAMES.has(tool.name)
  })
  return collapseAdjacentSidings(filtered)
}
