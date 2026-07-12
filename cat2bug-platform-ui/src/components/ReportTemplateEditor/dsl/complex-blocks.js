import { normalizeDslSource } from './normalize'

export const RAW_BLOCK_KIND = {
  PLAN_STATISTICS: 'plan-statistics',
  MULTIMD_TABLE: 'multimd-table'
}

/**
 * @param {string} text
 */
export function isPlanStatisticsTableContent(text) {
  const t = normalizeDslSource(text).trim()
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
  return tableLines.some((line) => /\|\|/.test(line) || /^\s*\^\^/.test(line.trim()))
}

function overlapsRanges(ranges, start, end) {
  return ranges.some((range) => start < range.end && end > range.start)
}

/**
 * @param {string} source
 * @returns {{ start: number, end: number, kind: string, value: string }[]}
 */
export function findRawPreservedBlockRanges(source) {
  const normalized = normalizeDslSource(source)
  /** @type {{ start: number, end: number, kind: string, value: string }[]} */
  const ranges = []

  const planStart = normalized.indexOf('|项目名称|')
  if (planStart !== -1) {
    const slice = normalized.slice(planStart)
    const endMatch = slice.match(/\$\{api\.plan\.defectRepairAvgHour\[\d+\]\}h?\|\|?/)
    if (endMatch && endMatch.index != null) {
      let end = planStart + endMatch.index + endMatch[0].length
      while (end < normalized.length && normalized[end] === '\n') {
        end += 1
      }
      const value = normalized.slice(planStart, end).trimEnd()
      if (isPlanStatisticsTableContent(value)) {
        ranges.push({
          start: planStart,
          end,
          kind: RAW_BLOCK_KIND.PLAN_STATISTICS,
          value
        })
      }
    }
  }

  const lines = normalized.split('\n')
  let offset = 0
  let blockStart = -1
  /** @type {string[]} */
  let blockLines = []

  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i]
    const lineStart = offset
    offset += line.length + 1
    const trimmed = line.trim()
    const isTableLine = trimmed.startsWith('|') || trimmed.startsWith('^^')

    if (isTableLine) {
      if (blockStart === -1) {
        blockStart = lineStart
      }
      blockLines.push(line)
    } else if (blockStart !== -1) {
      const value = blockLines.join('\n')
      if (
        isMultimdMergedTableContent(value) &&
        !overlapsRanges(ranges, blockStart, lineStart)
      ) {
        ranges.push({
          start: blockStart,
          end: lineStart,
          kind: RAW_BLOCK_KIND.MULTIMD_TABLE,
          value
        })
      }
      blockStart = -1
      blockLines = []
    }
  }

  if (blockStart !== -1) {
    const value = blockLines.join('\n')
    if (isMultimdMergedTableContent(value) && !overlapsRanges(ranges, blockStart, offset)) {
      ranges.push({
        start: blockStart,
        end: offset,
        kind: RAW_BLOCK_KIND.MULTIMD_TABLE,
        value
      })
    }
  }

  return ranges.sort((a, b) => a.start - b.start)
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
