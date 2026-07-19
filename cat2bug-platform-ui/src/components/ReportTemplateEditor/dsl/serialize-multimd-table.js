import { TableMap } from 'prosemirror-tables'
import { formatPipeRow, formatPipeSeparator, isPipeSeparatorRow, splitPipeTableCells } from './normalize-pipe-table'
import { isPlanStatisticsTableContent } from './complex-blocks'

/**
 * Markdown-safe marker (link-ref comment). Survives MdEditor better than HTML comments,
 * and does not swallow the following GFM table during parse.
 */
export const MULTIMD_TABLE_MARKER = '[//]: # (cat2bug-table-spans)'

const MULTIMD_MARKER_RE =
  /(?:<!--\s*cat2bug-table-spans\s*-->|\[\/\/\]:\s*#\s*\(cat2bug-table-spans\))/i

/** Placeholder so hardbreaks survive whitespace collapse in getTableCellPlainText. */
const HARDBREAK_PLACEHOLDER = '\u0001'

/**
 * Serialize a DSL atom (variable / plan / image) for Multimd cell text.
 * Prefer `raw`; if empty, rebuild from path attrs so table rewrite does not drop chips.
 *
 * @param {import('prosemirror-model').Node} node
 * @returns {string|null}
 */
export function dslAtomToCellText(node) {
  if (!node || !node.type) {
    return null
  }
  const name = node.type.name
  const attrs = node.attrs || {}
  if (typeof attrs.raw === 'string' && attrs.raw) {
    return attrs.raw
  }
  if (name === 'cat2bug_variable' && attrs.path) {
    return `\${${attrs.path}}`
  }
  if (name === 'cat2bug_plan_variable' && attrs.path) {
    const index = Number(attrs.planIndex) || 1
    return `\${${attrs.path}[${index}]}`
  }
  if (name === 'cat2bug_dynamic_image' && attrs.path) {
    const stylePart = attrs.style ? `["style":"${attrs.style}"]` : ''
    return `$image${stylePart}{${attrs.path}}`
  }
  return null
}

/**
 * Serialize cell content for Multimd pipe tables.
 * Must keep DSL atom `raw` (`$image…`, `${…}`) — a blank leafText turns them into spaces
 * and the next rewrite/sync drops the cover/variable from the table.
 * Hardbreaks become `<br>` (GFM-safe in-cell line breaks; stock serialize turns them into spaces).
 *
 * @param {import('prosemirror-model').Node} cell
 * @returns {string}
 */
export function getTableCellPlainText(cell) {
  return cell
    .textBetween(0, cell.content.size, ' ', (node) => {
      if (node.type && node.type.name === 'hardbreak') {
        return HARDBREAK_PLACEHOLDER
      }
      const dsl = dslAtomToCellText(node)
      if (dsl != null) {
        return dsl
      }
      return ''
    })
    .replace(/[^\S\u0001]+/g, ' ')
    .replace(new RegExp(`\\s*${HARDBREAK_PLACEHOLDER}\\s*`, 'g'), '<br>')
    .trim()
}

/**
 * @param {import('prosemirror-model').Node} table
 * @returns {boolean}
 */
export function tableHasSpanAttrs(table) {
  let found = false
  table.descendants((node) => {
    if (found) {
      return false
    }
    if (node.type.name !== 'table_cell' && node.type.name !== 'table_header') {
      return true
    }
    if (Number(node.attrs.colspan || 1) > 1 || Number(node.attrs.rowspan || 1) > 1) {
      found = true
      return false
    }
    return false
  })
  return found
}

/**
 * @param {import('prosemirror-model').Node} table
 * @returns {boolean}
 */
export function tableHasHardbreak(table) {
  let found = false
  table.descendants((node) => {
    if (found) {
      return false
    }
    if (node.type.name === 'hardbreak') {
      found = true
      return false
    }
    return true
  })
  return found
}

/**
 * Serialize a ProseMirror table node to MultiMarkdown pipe syntax.
 * Covered colspan slots become empty (`||`); rowspan continuations become `^^`.
 *
 * @param {import('prosemirror-model').Node} table
 * @param {{ withMarker?: boolean }} [options]
 * @returns {string}
 */
export function serializeMultimdTable(table, options = {}) {
  const withMarker = options.withMarker !== false
  const map = TableMap.get(table)
  const { width, height } = map
  /** @type {string[]} */
  const lines = []

  for (let row = 0; row < height; row += 1) {
    /** @type {string[]} */
    const cells = []
    for (let col = 0; col < width;) {
      const cellPos = map.map[row * width + col]
      const rect = map.findCell(cellPos)

      if (rect.top === row && rect.left === col) {
        const cell = table.nodeAt(cellPos)
        cells.push(cell ? getTableCellPlainText(cell) : '')
        for (let i = 1; i < rect.right - rect.left; i += 1) {
          cells.push('')
        }
        col = rect.right
      } else if (rect.left === col && rect.top < row) {
        cells.push('^^')
        for (let i = 1; i < rect.right - rect.left; i += 1) {
          cells.push('')
        }
        col = rect.right
      } else {
        col += 1
      }
    }
    lines.push(formatPipeRow(cells))
    if (row === 0) {
      lines.push(formatPipeSeparator(width))
    }
  }

  const body = lines.join('\n')
  // Blank line after marker so GFM table parsing is reliable.
  return withMarker ? `${MULTIMD_TABLE_MARKER}\n\n${body}` : body
}

/**
 * Strip known Multimd span markers from a table markdown fragment.
 * @param {string} text
 * @returns {string}
 */
export function stripMultimdTableMarkers(text) {
  return String(text || '')
    .replace(/<!--\s*cat2bug-table-spans\s*-->\s*/gi, '')
    .replace(/\[\/\/\]:\s*#\s*\(cat2bug-table-spans\)\s*/gi, '')
    .trim()
}

/**
 * True when a pipe-table fragment uses Multimd empty slots for colspan
 * (non-empty cell followed by an empty cell).
 * @param {string} tableMd
 * @returns {boolean}
 */
export function pipeTableHasColspanEmpties(tableMd) {
  const lines = String(tableMd || '').split(/\r?\n/)
  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed.startsWith('|') || isPipeSeparatorRow(trimmed)) {
      continue
    }
    const cells = splitPipeTableCells(trimmed)
    for (let i = 0; i < cells.length - 1; i += 1) {
      if (cells[i] !== '' && cells[i + 1] === '') {
        return true
      }
    }
  }
  return false
}

/**
 * Whether markdown source should trigger Multimd span application after parse.
 * @param {string} source
 * @returns {boolean}
 */
export function sourceNeedsMultimdSpanApply(source) {
  const text = String(source || '')
  if (MULTIMD_MARKER_RE.test(text)) {
    return true
  }
  if (/\|\s*\^\^\s*\|/.test(text)) {
    return true
  }

  for (const block of findPipeTableBlocks(text)) {
    const raw = stripMultimdTableMarkers(block.value)
    if (isPlanStatisticsTableContent(raw)) {
      continue
    }
    if (/\|\s*\^\^\s*\|/.test(raw) || pipeTableHasColspanEmpties(raw)) {
      return true
    }
  }
  return false
}

/**
 * Extract pipe-table blocks from markdown (contiguous | lines), including an optional marker line above.
 * @param {string} source
 * @returns {{ start: number, end: number, value: string }[]}
 */
export function findPipeTableBlocks(source) {
  const normalized = String(source || '')
  const lines = normalized.split('\n')
  /** @type {{ start: number, end: number, value: string }[]} */
  const blocks = []
  let offset = 0
  let blockStart = -1
  let markerStart = -1

  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i]
    const lineStart = offset
    offset += line.length + 1
    const trimmed = line.trim()
    const isMarker = MULTIMD_MARKER_RE.test(trimmed)
    const isTableLine = trimmed.startsWith('|')

    if (isMarker && blockStart === -1) {
      markerStart = lineStart
      continue
    }

    // Blank line between marker and table stays attached to the block
    if (!trimmed && markerStart >= 0 && blockStart === -1) {
      continue
    }

    if (isTableLine) {
      if (blockStart === -1) {
        blockStart = markerStart >= 0 ? markerStart : lineStart
      }
      markerStart = -1
    } else if (blockStart !== -1) {
      blocks.push({
        start: blockStart,
        end: lineStart,
        value: normalized.slice(blockStart, lineStart).replace(/\n$/, '')
      })
      blockStart = -1
      markerStart = isMarker ? lineStart : -1
    } else {
      markerStart = -1
    }
  }

  if (blockStart !== -1) {
    blocks.push({
      start: blockStart,
      end: Math.min(offset, normalized.length),
      value: normalized.slice(blockStart).replace(/\n$/, '')
    })
  }

  return blocks
}

/**
 * Replace pipe tables in markdown with Multimd serialization where the PM table has spans.
 * Tables and pipe blocks are paired by document order.
 * Also absorbs orphaned non-pipe lines that stock GFM serialize may lift out of spanned tables.
 *
 * @param {string} markdown
 * @param {import('prosemirror-model').Node} doc
 * @returns {string}
 */
export function rewriteMarkdownTablesFromDoc(markdown, doc) {
  /** @type {import('prosemirror-model').Node[]} */
  const tables = []
  doc.descendants((node) => {
    if (node.type.name === 'table') {
      tables.push(node)
    }
  })

  /** @type {{ index: number, withMarker: boolean }[]} */
  const rewriteTargets = []
  tables.forEach((table, index) => {
    const spanned = tableHasSpanAttrs(table)
    const hasBreak = tableHasHardbreak(table)
    if (spanned || hasBreak) {
      // Only Multimd span tables need the marker; hardbreak-only stays plain GFM.
      rewriteTargets.push({ index, withMarker: spanned })
    }
  })
  if (!rewriteTargets.length) {
    return markdown
  }

  const blocks = findPipeTableBlocks(markdown)
  if (!blocks.length) {
    return markdown
  }

  let result = markdown
  for (let i = rewriteTargets.length - 1; i >= 0; i -= 1) {
    const { index: tableIndex, withMarker } = rewriteTargets[i]
    const block = blocks[tableIndex]
    const table = tables[tableIndex]
    if (!block || !table) {
      continue
    }
    const end = extendBlockEndOverOrphans(result, block.end)
    let serialized = serializeMultimdTable(table, { withMarker })
    if (!serialized.endsWith('\n')) {
      serialized = `${serialized}\n`
    }
    let rest = result.slice(end)
    // Keep a blank line before following DSL blocks so they stay outside the table.
    if (/^\r?\n?\s*\$(?:image|img|table|card)\b/.test(rest) || /^\r?\n?\s*\$\{/.test(rest)) {
      rest = rest.replace(/^\r?\n*/, '\n')
    }
    result = `${result.slice(0, block.start)}${serialized}${rest}`
  }
  return result
}

/**
 * After a broken GFM table serialize, cell text may sit on the next line outside `|…|`.
 * Absorb those orphan lines into the replacement range so Multimd rewrite removes them.
 * @param {string} markdown
 * @param {number} end
 * @returns {number}
 */
function extendBlockEndOverOrphans(markdown, end) {
  let cursor = end
  while (cursor < markdown.length) {
    if (markdown[cursor] === '\n') {
      cursor += 1
    }
    const nextNewline = markdown.indexOf('\n', cursor)
    const lineEnd = nextNewline === -1 ? markdown.length : nextNewline
    const line = markdown.slice(cursor, lineEnd).trim()
    if (!line) {
      break
    }
    if (
      line.startsWith('|') ||
      line.startsWith('#') ||
      line.startsWith('```') ||
      line.startsWith('<!--') ||
      line.startsWith('[//]:') ||
      line.startsWith('>') ||
      line.startsWith('- ') ||
      line.startsWith('* ') ||
      /^\d+\.\s/.test(line) ||
      // Intentional DSL blocks after a spanned table must not be treated as
      // "orphaned cell text" or rewrite will delete $image / $card / ${…}.
      /^\s*\$(?:image|img|table|card)\b/.test(line) ||
      /^\s*\$\{/.test(line)
    ) {
      break
    }
    cursor = lineEnd
    if (nextNewline === -1) {
      break
    }
  }
  return cursor
}
