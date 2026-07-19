import { normalizePipeTableMarkdown, isIrregularPipeTable } from './normalize-pipe-table'

export const RAW_BLOCK_KIND = {
  PLAN_STATISTICS: 'plan-statistics',
  MULTIMD_TABLE: 'multimd-table'
}

/**
 * @param {string} text
 */
export function isPlanStatisticsTableContent(text) {
  const t = String(text || '').trim()
  if (!t.includes('${api.plan.')) {
    return false
  }
  return (
    (t.includes('|项目名称|') && t.includes('度量指标数据如下')) ||
    (t.includes('|测试计划|') &&
      t.includes('defectDiscoveryRate') &&
      (t.match(/\$\{api\.plan\./g) || []).length >= 10)
  )
}

/**
 * Multimd rowspan / special merge markers only.
 * Do NOT treat GFM empty cells (`|a||b|`) as multimd — those must stay editable Milkdown tables.
 * @param {string} text
 */
export function isMultimdMergedTableContent(text) {
  const lines = text.trim().split(/\r?\n/)
  const tableLines = lines.filter((line) => {
    const trimmed = line.trim()
    return trimmed.startsWith('|') || trimmed.startsWith('^^')
  })
  if (tableLines.length < 2) {
    return false
  }
  return tableLines.some((line) => /\^\^/.test(line))
}

/**
 * Locate plan-statistics table span in source (for GFM normalization, not raw preserve).
 * @param {string} source
 * @returns {{ start: number, end: number, value: string }|null}
 */
export function findPlanStatisticsTableRange(source) {
  const normalized = String(source || '')
  const planStart = normalized.indexOf('|项目名称|')
  if (planStart === -1) {
    return null
  }
  const slice = normalized.slice(planStart)
  const endMatch = slice.match(/\$\{api\.plan\.defectRepairAvgHour\[\d+\]\}h?\|\|?/)
  if (!endMatch || endMatch.index == null) {
    return null
  }
  let end = planStart + endMatch.index + endMatch[0].length
  while (end < normalized.length && normalized[end] === '\n') {
    end += 1
  }
  const value = normalized.slice(planStart, end).trimEnd()
  if (!isPlanStatisticsTableContent(value)) {
    return null
  }
  return { start: planStart, end, value }
}

/**
 * Rewrite legacy irregular plan-statistics pipe tables into GFM so Milkdown can edit them.
 * @param {string} source
 */
export function normalizePlanStatisticsTablesInSource(source) {
  const input = String(source || '')
  const range = findPlanStatisticsTableRange(input)
  if (!range) {
    return input
  }
  // Already valid GFM (Milkdown live serialize) — do not rewrite; rewriting pads
  // jagged rows to a huge width and floods the source pane with |||||.
  if (!isIrregularPipeTable(range.value)) {
    return input
  }
  const normalizedTable = normalizePipeTableMarkdown(range.value)
  if (normalizedTable === range.value) {
    return input
  }
  return `${input.slice(0, range.start)}${normalizedTable}${input.slice(range.end)}`
}

/**
 * Raw-preserved blocks (legacy hook).
 * Multimd `^^` / colspan tables are editable Milkdown tables now — do not freeze them
 * as `cat2bug_raw_block`. Extreme unparseable cases can be added here later as fallback.
 * @param {string} _source
 * @returns {{ start: number, end: number, kind: string, value: string }[]}
 */
export function findRawPreservedBlockRanges(_source) {
  return []
}

/**
 * @param {string} source
 * @returns {{ type: 'markdown'|'raw', value: string, kind?: string }[]}
 */
export function splitSourceWithRawBlocks(source) {
  const ranges = findRawPreservedBlockRanges(source)
  if (!ranges.length) {
    return [{ type: 'markdown', value: source }]
  }

  /** @type {{ type: 'markdown'|'raw', value: string, kind?: string }[]} */
  const parts = []
  let cursor = 0

  for (const range of ranges) {
    if (range.start > cursor) {
      parts.push({ type: 'markdown', value: source.slice(cursor, range.start) })
    }
    parts.push({ type: 'raw', kind: range.kind, value: range.value })
    cursor = range.end
  }

  if (cursor < source.length) {
    parts.push({ type: 'markdown', value: source.slice(cursor) })
  }

  return parts
}
