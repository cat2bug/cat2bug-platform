import { Fragment } from 'prosemirror-model'
import { TableMap } from 'prosemirror-tables'
import { isPlanStatisticsTableContent } from './complex-blocks'
import {
  isPipeSeparatorRow,
  splitPipeTableCells,
  formatPipeRow
} from './normalize-pipe-table'
import { getTableCellPlainText, tableHasSpanAttrs } from './serialize-multimd-table'
import { normalizeDslSource } from './normalize'
import { parseTemplate } from './parse'

/**
 * @typedef {{ text: string, colspan: number, rowspan: number }} MultimdCell
 * @typedef {{ width: number, rows: MultimdCell[][] }} MultimdTableGrid
 */

/**
 * Build inline Fragment for a table cell, promoting `${}` / `$image` to Milkdown nodes.
 * Plain Multimd rebuild used to call `schema.text(cell)` only — that left DSL as dead text
 * (no image preview, no variable hover chrome) after every span apply.
 *
 * @param {import('prosemirror-model').Schema} schema
 * @param {string} text
 * @returns {import('prosemirror-model').Fragment|null}
 */
export function createTableCellInlineContent(schema, text) {
  const source = normalizeDslSource(String(text || ''))
  if (!source) {
    return null
  }

  const pushText = (nodes, value) => {
    if (!value) {
      return
    }
    nodes.push(schema.text(value))
  }

  const pushDslOrText = (nodes, chunk) => {
    if (!chunk) {
      return
    }
    if (!/\$\{|\$image|\$img/.test(chunk)) {
      pushText(nodes, chunk)
      return
    }
    const segments = parseTemplate(chunk)
    for (const segment of segments) {
      if (segment.type === 'text') {
        pushText(nodes, segment.value)
        continue
      }
      if (segment.type === 'variable' && schema.nodes.cat2bug_variable) {
        nodes.push(schema.nodes.cat2bug_variable.create({
          path: segment.path,
          raw: segment.raw || `\${${segment.path}}`
        }))
        continue
      }
      if (segment.type === 'planVariable' && schema.nodes.cat2bug_plan_variable) {
        nodes.push(schema.nodes.cat2bug_plan_variable.create({
          path: segment.path,
          planIndex: segment.planIndex,
          raw: segment.raw || `\${${segment.path}[${segment.planIndex}]}`
        }))
        continue
      }
      if (segment.type === 'dynamicImage' && schema.nodes.cat2bug_dynamic_image) {
        nodes.push(schema.nodes.cat2bug_dynamic_image.create({
          style: segment.style || '',
          path: segment.path,
          raw: segment.raw || `$image{${segment.path}}`
        }))
        continue
      }
      pushText(nodes, segment.raw || segment.value || '')
    }
  }

  /** @type {import('prosemirror-model').Node[]} */
  const nodes = []
  const lineParts = source.split(/<br\s*\/?\s*>/i)
  lineParts.forEach((part, index) => {
    pushDslOrText(nodes, part)
    if (index < lineParts.length - 1 && schema.nodes.hardbreak) {
      nodes.push(schema.nodes.hardbreak.create())
    } else if (index < lineParts.length - 1) {
      // Schema without hardbreak: keep a visible separator in cell text
      pushText(nodes, '\n')
    }
  })

  return nodes.length ? Fragment.from(nodes) : null
}

/**
 * Parse a Multimd pipe table into a rowspan/colspan grid.
 * - Empty cells extend the previous cell's colspan (MultiMarkdown).
 * - A cell whose text is exactly `^^` extends the cell above's rowspan.
 *
 * @param {string} markdown
 * @returns {MultimdTableGrid|null}
 */
export function parseMultimdTable(markdown) {
  const raw = String(markdown || '')
    .replace(/<!--\s*cat2bug-table-spans\s*-->\s*/gi, '')
    .replace(/\[\/\/\]:\s*#\s*\(cat2bug-table-spans\)\s*/gi, '')
    .trim()
  const lines = raw
    .split(/\r?\n/)
    .map((line) => line.trimEnd())
    .filter((line) => line.trim().length > 0 && line.includes('|'))

  if (lines.length < 2) {
    return null
  }

  /** @type {string[][]} */
  const rawRows = []
  for (const line of lines) {
    if (isPipeSeparatorRow(line)) {
      continue
    }
    rawRows.push(splitPipeTableCells(line))
  }
  if (!rawRows.length) {
    return null
  }

  const width = Math.max(...rawRows.map((row) => row.length))
  if (width < 1) {
    return null
  }

  const padded = rawRows.map((row) => {
    const next = row.slice(0, width)
    while (next.length < width) {
      next.push('')
    }
    return next
  })

  const height = padded.length
  /** @type {(MultimdCell|null)[]} flat grid width*height, pointing to origin cell objects */
  const slots = Array.from({ length: width * height }, () => null)

  for (let row = 0; row < height; row += 1) {
    let col = 0
    while (col < width) {
      const index = row * width + col
      if (slots[index]) {
        col += 1
        continue
      }

      const text = padded[row][col]
      if (text.trim() === '^^') {
        const above = findCoveringOrigin(slots, width, height, row - 1, col)
        if (!above) {
          const cell = { text: '', colspan: 1, rowspan: 1 }
          slots[index] = cell
          col += 1
          continue
        }
        above.cell.rowspan += 1
        for (let c = above.col; c < above.col + above.cell.colspan; c += 1) {
          slots[row * width + c] = above.cell
        }
        col = above.col + above.cell.colspan
        continue
      }

      let colspan = 1
      while (col + colspan < width && padded[row][col + colspan] === '' && !slots[row * width + col + colspan]) {
        colspan += 1
      }
      const cell = { text, colspan, rowspan: 1 }
      for (let c = 0; c < colspan; c += 1) {
        slots[row * width + col + c] = cell
      }
      col += colspan
    }
  }

  /** @type {MultimdCell[][]} */
  const rows = []
  for (let row = 0; row < height; row += 1) {
    /** @type {MultimdCell[]} */
    const out = []
    const seen = new Set()
    for (let col = 0; col < width; col += 1) {
      const cell = slots[row * width + col]
      if (!cell || seen.has(cell)) {
        continue
      }
      // Emit only at the origin row/col of this cell
      const origin = findCellOrigin(slots, width, height, cell)
      if (!origin || origin.row !== row || origin.col !== col) {
        continue
      }
      seen.add(cell)
      out.push({
        text: cell.text,
        colspan: cell.colspan,
        rowspan: cell.rowspan
      })
    }
    rows.push(out)
  }

  return { width, rows }
}

/**
 * @param {(MultimdCell|null)[]} slots
 * @param {number} width
 * @param {number} height
 * @param {number} row
 * @param {number} col
 * @returns {{ cell: MultimdCell, col: number, row: number }|null}
 */
function findCoveringOrigin(slots, width, height, row, col) {
  for (let r = row; r >= 0; r -= 1) {
    const cell = slots[r * width + col]
    if (!cell) {
      continue
    }
    const origin = findCellOrigin(slots, width, height, cell)
    if (!origin) {
      continue
    }
    if (origin.col <= col && col < origin.col + cell.colspan) {
      return { cell, col: origin.col, row: origin.row }
    }
  }
  return null
}

/**
 * @param {(MultimdCell|null)[]} slots
 * @param {number} width
 * @param {number} height
 * @param {MultimdCell} cell
 * @returns {{ row: number, col: number }|null}
 */
function findCellOrigin(slots, width, height, cell) {
  for (let row = 0; row < height; row += 1) {
    for (let col = 0; col < width; col += 1) {
      if (slots[row * width + col] === cell) {
        return { row, col }
      }
    }
  }
  return null
}

/**
 * Build a ProseMirror table node from a Multimd grid.
 *
 * @param {import('prosemirror-model').Schema} schema
 * @param {MultimdTableGrid} grid
 * @returns {import('prosemirror-model').Node|null}
 */
export function buildPmTableFromMultimdGrid(schema, grid) {
  const headerRowType = schema.nodes.table_header_row
  const rowType = schema.nodes.table_row
  const headerType = schema.nodes.table_header
  const cellType = schema.nodes.table_cell
  const paragraph = schema.nodes.paragraph
  const tableType = schema.nodes.table
  if (!headerRowType || !rowType || !headerType || !cellType || !paragraph || !tableType) {
    return null
  }

  /**
   * @param {MultimdCell} cell
   * @param {boolean} isHeader
   */
  function createCell(cell, isHeader) {
    const type = isHeader ? headerType : cellType
    const text = cell.text && cell.text !== '^^' ? cell.text : ''
    const inline = createTableCellInlineContent(schema, text)
    const para = inline
      ? paragraph.create(null, inline)
      : paragraph.createAndFill()
    return type.create(
      {
        colspan: cell.colspan,
        rowspan: cell.rowspan,
        alignment: 'left'
      },
      para ? Fragment.from(para) : undefined
    )
  }

  if (!grid.rows.length) {
    return null
  }

  const headerCells = grid.rows[0].map((cell) => createCell(cell, true))
  const headerRow = headerRowType.create(null, Fragment.from(headerCells))
  const bodyRows = grid.rows.slice(1).map((rowCells) => {
    const cells = rowCells.map((cell) => createCell(cell, false))
    return rowType.create(null, Fragment.from(cells))
  })

  try {
    return tableType.create(null, Fragment.from([headerRow, ...bodyRows]))
  } catch (_e) {
    return null
  }
}

/**
 * @param {import('prosemirror-model').Node} table
 * @returns {string}
 */
function tablePlainSource(table) {
  const map = TableMap.get(table)
  const { width, height } = map
  /** @type {string[]} */
  const lines = []
  for (let row = 0; row < height; row += 1) {
    /** @type {string[]} */
    const cells = []
    for (let col = 0; col < width;) {
      const pos = map.map[row * width + col]
      const rect = map.findCell(pos)
      if (rect.top === row && rect.left === col) {
        const cell = table.nodeAt(pos)
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
  }
  return lines.join('\n')
}

/**
 * Apply Multimd colspan/rowspan to tables in the document.
 *
 * @param {import('prosemirror-state').EditorState} state
 * @param {{ forceEmptyColspan?: boolean }} [options]
 * @returns {import('prosemirror-state').Transaction|null}
 */
export function applyMultimdSpansToDoc(state, options = {}) {
  const { forceEmptyColspan = false } = options
  const { schema, doc } = state
  /** @type {{ pos: number, node: import('prosemirror-model').Node }[]} */
  const targets = []

  doc.descendants((node, pos) => {
    if (node.type.name !== 'table') {
      return true
    }
    if (tableHasSpanAttrs(node)) {
      return false
    }
    const plain = tablePlainSource(node)
    if (isPlanStatisticsTableContent(plain)) {
      return false
    }
    const hasRowspanMarker = nodeHasRowspanMarker(node)
    const hasEmpty = forceEmptyColspan && tableHasEmptyCell(node)
    if (hasRowspanMarker || hasEmpty) {
      targets.push({ pos, node })
    }
    return false
  })

  if (!targets.length) {
    return null
  }

  let tr = state.tr
  for (let i = targets.length - 1; i >= 0; i -= 1) {
    const { pos, node } = targets[i]
    const plain = tablePlainSource(node)
    const grid = parseMultimdTable(plain)
    if (!grid) {
      continue
    }
    const next = buildPmTableFromMultimdGrid(schema, grid)
    if (!next) {
      continue
    }
    const mappedPos = tr.mapping.map(pos)
    tr = tr.replaceWith(mappedPos, mappedPos + node.nodeSize, next)
  }

  return tr.docChanged ? tr : null
}

/**
 * @param {import('prosemirror-model').Node} table
 * @returns {boolean}
 */
function nodeHasRowspanMarker(table) {
  let found = false
  table.descendants((node) => {
    if (found) {
      return false
    }
    if (node.type.name !== 'table_cell' && node.type.name !== 'table_header') {
      return true
    }
    if (getTableCellPlainText(node) === '^^') {
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
function tableHasEmptyCell(table) {
  let found = false
  table.descendants((node) => {
    if (found) {
      return false
    }
    if (node.type.name !== 'table_cell' && node.type.name !== 'table_header') {
      return true
    }
    if (!getTableCellPlainText(node)) {
      found = true
      return false
    }
    return false
  })
  return found
}
