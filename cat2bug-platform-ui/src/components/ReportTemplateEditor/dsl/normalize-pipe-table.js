/**
 * Normalize irregular pipe tables into GFM-compatible markdown tables
 * (consistent column count, leading/trailing pipes, header separator).
 */

/** Milkdown empty-paragraph marker inside GFM table cells */
const EMPTY_CELL_BREAK_RE = /^(?:<br\s*\/?\s*>)+$/i
const ANY_BR_RE = /<br\s*\/?\s*>/gi
/** In-place: `| <br /> |` or `| <br /> <br /> |` → `||` */
const CELL_ONLY_BR_RE = /\|(?:\s*<br\s*\/?\s*>\s*)+\|/gi

/**
 * Strip Milkdown empty-paragraph `<br />` markers from a pipe-table cell.
 * Empty cells must stay empty (`||`), not `| <br /> |`, or split-pane sync loops.
 * @param {string} cell
 * @returns {string}
 */
export function cleanPipeTableCell(cell) {
  const raw = String(cell ?? '')
  const trimmed = raw.trim()
  if (!trimmed || EMPTY_CELL_BREAK_RE.test(trimmed)) {
    return ''
  }
  // Keep intentional `<br>` line breaks inside non-empty cells (Mod-Enter).
  // Only empty-cell-only `<br />` markers are stripped above.
  ANY_BR_RE.lastIndex = 0
  return trimmed
}

/**
 * Split a pipe row into cells without treating all-empty rows as separators.
 * @param {string} line
 * @returns {string[]}
 */
export function splitPipeTableCells(line) {
  let text = String(line || '')
  if (text.startsWith('|')) {
    text = text.slice(1)
  }
  if (text.endsWith('|')) {
    text = text.slice(0, -1)
  }
  return text.split('|').map((cell) => cleanPipeTableCell(cell))
}

/**
 * Remove Milkdown `<br />` empty-cell markers from pipe-table rows in full source.
 * Uses in-place replacement so we do not rewrite column counts via formatPipeRow.
 * @param {string} source
 * @returns {string}
 */
export function stripMilkdownTableCellBreaks(source) {
  const input = String(source || '')
  if (!/<br\s*\/?\s*>/i.test(input)) {
    return input
  }
  return input
    .split('\n')
    .map((line) => {
      if (!line.includes('|') || !/<br\s*\/?\s*>/i.test(line)) {
        return line
      }
      const trimmed = line.trim()
      if (!trimmed.startsWith('|')) {
        return line
      }
      // Collapse `| <br /> |` → `||` repeatedly (overlapping pipes)
      let next = trimmed
      let prev = ''
      while (next !== prev) {
        prev = next
        CELL_ONLY_BR_RE.lastIndex = 0
        next = next.replace(CELL_ONLY_BR_RE, '||')
      }
      // Residual br inside non-empty cells
      if (/<br\s*\/?\s*>/i.test(next)) {
        next = formatPipeRow(splitPipeTableCells(next))
      }
      return next
    })
    .join('\n')
}

/**
 * A GFM separator row must contain at least one --- cell (not merely empty cells).
 * @param {string} line
 */
export function isPipeSeparatorRow(line) {
  const cells = splitPipeTableCells(line)
  if (!cells.length) {
    return false
  }
  const hasDash = cells.some((cell) => /^:?-+:?$/.test(cell))
  if (!hasDash) {
    return false
  }
  return cells.every((cell) => !cell || /^:?-+:?$/.test(cell))
}

/**
 * @param {string[]} cells
 * @param {number} colCount
 */
export function padPipeCells(cells, colCount) {
  const next = cells.slice(0, colCount)
  while (next.length < colCount) {
    next.push('')
  }
  return next
}

/**
 * @param {string[]} cells
 */
export function formatPipeRow(cells) {
  return `|${cells.map((cell) => String(cell ?? '')).join('|')}|`
}

/**
 * @param {number} colCount
 */
export function formatPipeSeparator(colCount) {
  return `|${Array.from({ length: colCount }, () => '-').join('|')}|`
}

/**
 * True when a pipe table needs structural repair (missing separator or jagged columns).
 * Already-valid GFM from Milkdown should not be rewritten on every keystroke.
 * @param {string} source
 */
export function isIrregularPipeTable(source) {
  const lines = String(source || '')
    .split(/\r?\n/)
    .map((line) => line.trimEnd())
    .filter((line) => line.trim().length > 0 && line.includes('|'))

  if (lines.length < 2) {
    return false
  }

  const rows = lines.map((line) => ({
    cells: splitPipeTableCells(line),
    separator: isPipeSeparatorRow(line)
  }))
  const dataRows = rows.filter((row) => !row.separator)
  if (!dataRows.length) {
    return true
  }
  const hasSeparator = rows.some((row) => row.separator)
  if (!hasSeparator) {
    return true
  }
  const width = dataRows[0].cells.length
  return dataRows.some((row) => row.cells.length !== width)
}

/**
 * Normalize a pipe-table markdown fragment to a single GFM table.
 * Column width follows the header (first data) row — never Math.max across rows,
 * so one broken Milkdown line cannot inflate the table into ||||| walls.
 * @param {string} source
 * @returns {string}
 */
export function normalizePipeTableMarkdown(source) {
  const lines = String(source || '')
    .split(/\r?\n/)
    .map((line) => line.trimEnd())
    .filter((line) => line.trim().length > 0 && line.includes('|'))

  if (!lines.length) {
    return String(source || '').trimEnd()
  }

  /** @type {{ cells: string[], separator: boolean }[]} */
  const rows = []
  for (const line of lines) {
    const cells = splitPipeTableCells(line)
    rows.push({ cells, separator: isPipeSeparatorRow(line) })
  }

  const withoutSeps = rows.filter((row) => !row.separator)
  if (!withoutSeps.length) {
    return formatPipeSeparator(1)
  }

  // Header row defines width; truncate runaway columns from bad serializes
  const colCount = Math.max(1, withoutSeps[0].cells.length)
  const out = [
    formatPipeRow(padPipeCells(withoutSeps[0].cells, colCount)),
    formatPipeSeparator(colCount)
  ]
  for (let i = 1; i < withoutSeps.length; i += 1) {
    out.push(formatPipeRow(padPipeCells(withoutSeps[i].cells, colCount)))
  }
  return out.join('\n')
}
