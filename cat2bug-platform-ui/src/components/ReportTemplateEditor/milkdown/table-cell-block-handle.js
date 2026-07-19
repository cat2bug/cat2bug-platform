import { TextSelection } from 'prosemirror-state'
import { isSelectionInsideTableCell } from './table-cell-caret'

const TABLE_CELL_TYPES = new Set(['table_cell', 'table_header'])
const HIDDEN_ANCESTOR_TYPES = new Set(['blockquote', 'math_inline'])

/**
 * @param {import('prosemirror-model').ResolvedPos} $pos
 * @param {string} typeName
 * @returns {boolean}
 */
function hasAncestorType($pos, typeName) {
  for (let depth = $pos.depth; depth > 0; depth -= 1) {
    if ($pos.node(depth).type.name === typeName) {
      return true
    }
  }
  return false
}

/**
 * @param {import('prosemirror-model').ResolvedPos} $pos
 * @param {Set<string>} typeNames
 * @returns {boolean}
 */
function hasAncestorInSet($pos, typeNames) {
  for (let depth = $pos.depth; depth > 0; depth -= 1) {
    if (typeNames.has($pos.node(depth).type.name)) {
      return true
    }
  }
  return false
}

/**
 * @param {import('prosemirror-model').ResolvedPos} $pos
 * @returns {number} table depth or -1
 */
function findTableDepth($pos) {
  for (let depth = $pos.depth; depth > 0; depth -= 1) {
    if ($pos.node(depth).type.name === 'table') {
      return depth
    }
  }
  return -1
}

/**
 * @param {import('prosemirror-model').ResolvedPos} $pos
 * @returns {number|null}
 */
export function findTablePos($pos) {
  const depth = findTableDepth($pos)
  return depth > 0 ? $pos.before(depth) : null
}

/**
 * Crepe's default filter hides everything under `table`, then walks up until the
 * **table** node itself — that is how +/- / drag appear before the table.
 *
 * We must NOT accept cell paragraphs here: if we do, the walker stops on the
 * cell and the whole-table handle never shows. In-cell insert uses the cell
 * floating toolbar (添加) instead.
 *
 * @param {import('prosemirror-model').ResolvedPos} $pos
 * @param {import('prosemirror-model').Node} node
 * @returns {boolean}
 */
export function tableAwareFilterNodes($pos, node) {
  if (
    HIDDEN_ANCESTOR_TYPES.has(node.type.name) ||
    hasAncestorInSet($pos, HIDDEN_ANCESTOR_TYPES)
  ) {
    return false
  }

  // Whole table block — Milkdown block handle (+ / drag) before the table.
  if (node.type.name === 'table') {
    return true
  }

  // Rows / cells / in-cell paragraphs: hide doc block handle so selection
  // walks up to the table (same as Crepe default for nested table content).
  if (
    node.type.name === 'table_row' ||
    node.type.name === 'table_header_row' ||
    TABLE_CELL_TYPES.has(node.type.name) ||
    hasAncestorType($pos, 'table')
  ) {
    return false
  }

  return true
}

/**
 * True when the left block handle is bound to the whole table block
 * (not a cell paragraph). Insert goes after the table via
 * insertParagraphAfterBlockHandle / ensureParagraphAfterTable.
 *
 * @param {import('prosemirror-view').EditorView} view
 * @param {HTMLElement} handleEl
 * @returns {boolean}
 */
export function isTableBlockHandle(view, handleEl) {
  if (!view?.dom || !(handleEl instanceof HTMLElement)) {
    return false
  }
  const handleRect = handleEl.getBoundingClientRect()
  const midY = handleRect.top + handleRect.height / 2
  const blocks = view.dom.querySelectorAll('.milkdown-table-block')
  for (let i = 0; i < blocks.length; i += 1) {
    const block = blocks[i]
    if (!(block instanceof HTMLElement)) {
      continue
    }
    const r = block.getBoundingClientRect()
    if (r.width < 1 || r.height < 1) {
      continue
    }
    // Handle sits in the left gutter before the table block.
    if (handleRect.left > r.left - 4) {
      continue
    }
    const overlapsY = midY >= r.top - 6 && midY <= r.bottom + 6
    if (!overlapsY) {
      continue
    }
    const nearTop = Math.abs(handleRect.top - r.top) <= 28
    const nearMid = Math.abs(midY - (r.top + r.height / 2)) <= 32
    if (nearTop) {
      return true
    }
    // Short tables use placement "left" (vertically centered on the block).
    if (nearMid && r.height <= 140) {
      return true
    }
  }
  return false
}

/**
 * Guarantee an empty paragraph after the table and put the caret inside it.
 * Never use TextSelection.near(..., -1) — that walks back into the table when
 * the doc ends at the table, which made "$image" land in the last row / spawn
 * broken extra rows via Crepe onAdd.
 *
 * @param {import('prosemirror-view').EditorView} view
 * @param {number} tablePos
 * @returns {boolean}
 */
export function ensureParagraphAfterTable(view, tablePos) {
  const table = view.state.doc.nodeAt(tablePos)
  if (!table || table.type.name !== 'table') {
    return false
  }

  const paraType = view.state.schema.nodes.paragraph
  if (!paraType) {
    return false
  }

  const afterPos = tablePos + table.nodeSize
  let tr = view.state.tr
  const $after = tr.doc.resolve(Math.min(afterPos, tr.doc.content.size))
  const next = $after.nodeAfter

  if (next && next.type === paraType) {
    tr = tr.setSelection(TextSelection.create(tr.doc, afterPos + 1))
  } else if (next && next.isTextblock) {
    tr = tr.setSelection(TextSelection.create(tr.doc, afterPos + 1))
  } else {
    tr = tr.insert(afterPos, paraType.create())
    tr = tr.setSelection(TextSelection.create(tr.doc, afterPos + 1))
  }

  if (isSelectionInsideTableCell(tr.selection)) {
    return false
  }

  view.dispatch(tr.scrollIntoView())
  view.focus()
  return true
}

/**
 * @param {import('prosemirror-view').EditorView} view
 * @param {HTMLElement} handleEl
 * @returns {{
 *   pos: number,
 *   inTableCell: boolean,
 *   placeAfterTable?: boolean,
 *   tablePos?: number
 * } | null}
 */
export function resolvePosNearBlockHandle(view, handleEl) {
  if (!view || !(handleEl instanceof HTMLElement)) {
    return null
  }

  const handleRect = handleEl.getBoundingClientRect()
  const midY = handleRect.top + handleRect.height / 2
  const pmRect = view.dom.getBoundingClientRect()
  const left = Math.min(
    Math.max(handleRect.right + 12, pmRect.left + 48),
    Math.max(pmRect.left + 48, pmRect.right - 24)
  )

  const hit = view.posAtCoords({ left, top: midY })
  // Fall back to current selection when coords miss (common near table edges).
  const pos = hit?.pos ?? (
    isSelectionInsideTableCell(view.state.selection)
      ? view.state.selection.from
      : null
  )
  if (pos == null) {
    return null
  }

  const $pos = view.state.doc.resolve(pos)
  const tablePos = findTablePos($pos)
  const inTableCell = hasAncestorInSet($pos, TABLE_CELL_TYPES)

  if (!inTableCell || tablePos == null) {
    return { pos, inTableCell: false }
  }

  const tableNode = view.state.doc.nodeAt(tablePos)
  if (!tableNode) {
    return { pos, inTableCell: true, tablePos }
  }

  const tableDom = view.nodeDOM(tablePos)
  const tableRect =
    tableDom instanceof HTMLElement ? tableDom.getBoundingClientRect() : null

  // Last row of the table, or handle sitting on/below the table bottom:
  // left "+" means "new block under the table", not Crepe onAdd inside the cell
  // (onAdd inserts a 2nd paragraph into cellContent:"paragraph" and corrupts rows).
  const rowDepth = findRowDepth($pos)
  const isLastRow =
    rowDepth > 0 &&
    $pos.index(findTableDepth($pos)) === tableNode.childCount - 1
  const handleBelowTable =
    tableRect != null && midY >= tableRect.bottom - 12
  const handleInLowerThird =
    tableRect != null &&
    midY >= tableRect.top + (tableRect.height * 2) / 3

  if (handleBelowTable || (isLastRow && handleInLowerThird)) {
    return {
      pos: tablePos + tableNode.nodeSize,
      inTableCell: false,
      placeAfterTable: true,
      tablePos
    }
  }

  return { pos, inTableCell: true, tablePos }
}

/**
 * @param {import('prosemirror-model').ResolvedPos} $pos
 * @returns {number}
 */
function findRowDepth($pos) {
  for (let depth = $pos.depth; depth > 0; depth -= 1) {
    const name = $pos.node(depth).type.name
    if (name === 'table_row' || name === 'table_header_row') {
      return depth
    }
  }
  return -1
}

/**
 * @param {import('prosemirror-view').EditorView} view
 * @param {HTMLElement} handleEl
 * @returns {'cell' | 'after-table' | 'none'}
 */
export function focusBlockHandleInsertTarget(view, handleEl) {
  const resolved = resolvePosNearBlockHandle(view, handleEl)
  const selectionInCell = isSelectionInsideTableCell(view.state.selection)

  // Handle below / last-row → always a doc block after the table.
  if (resolved?.placeAfterTable && resolved.tablePos != null) {
    return ensureParagraphAfterTable(view, resolved.tablePos) ? 'after-table' : 'none'
  }

  // Any other hit inside a cell → keep inline insert in that cell; never let Crepe onAdd run.
  if (resolved?.inTableCell || selectionInCell) {
    const pos = resolved?.inTableCell
      ? resolved.pos
      : view.state.selection.from
    try {
      const selection = TextSelection.near(view.state.doc.resolve(pos), 1)
      view.dispatch(view.state.tr.setSelection(selection))
      view.focus()
    } catch (_e) {
      // ignore
    }
    return 'cell'
  }

  return 'none'
}

/**
 * @param {import('prosemirror-view').EditorView} view
 * @param {HTMLElement} handleEl
 * @returns {boolean}
 */
export function focusTableCellNearHandle(view, handleEl) {
  return focusBlockHandleInsertTarget(view, handleEl) === 'cell'
}

/**
 * Mimic Crepe onAdd: insert an empty paragraph after the block the left handle
 * is bound to, then leave the caret there — without opening Crepe's slash menu.
 *
 * @param {import('prosemirror-view').EditorView} view
 * @param {HTMLElement} handleEl
 * @returns {boolean}
 */
export function insertParagraphAfterBlockHandle(view, handleEl) {
  if (!view?.dom || !(handleEl instanceof HTMLElement)) {
    return false
  }

  const paraType = view.state.schema.nodes.paragraph
  if (!paraType) {
    return false
  }

  // Whole-table handle: always insert below the table (never into a cell).
  if (isTableBlockHandle(view, handleEl)) {
    const tablePos = findTablePosForBlockHandle(view, handleEl)
    if (tablePos != null) {
      return ensureParagraphAfterTable(view, tablePos)
    }
  }

  const handleRect = handleEl.getBoundingClientRect()
  const midY = handleRect.top + handleRect.height / 2
  const pmRect = view.dom.getBoundingClientRect()
  const left = Math.min(
    Math.max(handleRect.right + 12, pmRect.left + 48),
    Math.max(pmRect.left + 48, pmRect.right - 24)
  )
  const hit = view.posAtCoords({ left, top: midY })
  if (hit == null) {
    return false
  }

  const $pos = view.state.doc.resolve(hit.pos)
  for (let depth = $pos.depth; depth >= 1; depth -= 1) {
    const node = $pos.node(depth)
    if (!node.isBlock) {
      continue
    }
    const before = $pos.before(depth)
    const $check = view.state.doc.resolve(Math.min(before + 1, view.state.doc.content.size))
    if (!tableAwareFilterNodes($check, node)) {
      continue
    }

    const insertPos = before + node.nodeSize
    let tr = view.state.tr.insert(insertPos, paraType.create())
    tr = tr.setSelection(TextSelection.near(tr.doc.resolve(insertPos + 1)))
    if (isSelectionInsideTableCell(tr.selection)) {
      return false
    }
    view.dispatch(tr.scrollIntoView())
    view.focus()
    return true
  }

  return false
}

/**
 * @param {import('prosemirror-view').EditorView} view
 * @param {HTMLElement} handleEl
 * @returns {number|null}
 */
function findTablePosForBlockHandle(view, handleEl) {
  const handleRect = handleEl.getBoundingClientRect()
  const midY = handleRect.top + handleRect.height / 2
  const blocks = view.dom.querySelectorAll('.milkdown-table-block')
  for (let i = 0; i < blocks.length; i += 1) {
    const block = blocks[i]
    if (!(block instanceof HTMLElement)) {
      continue
    }
    const r = block.getBoundingClientRect()
    if (r.width < 1 || r.height < 1) {
      continue
    }
    if (handleRect.left > r.left - 4) {
      continue
    }
    const overlapsY = midY >= r.top - 6 && midY <= r.bottom + 6
    if (!overlapsY) {
      continue
    }
    const nearTop = Math.abs(handleRect.top - r.top) <= 28
    const nearMid = Math.abs(midY - (r.top + r.height / 2)) <= 32
    if (!nearTop && !(nearMid && r.height <= 140)) {
      continue
    }
    try {
      const pos = view.posAtDOM(block, 0)
      const $pos = view.state.doc.resolve(pos)
      const tablePos = findTablePos($pos)
      if (tablePos != null) {
        return tablePos
      }
      if ($pos.nodeAfter?.type.name === 'table') {
        return pos
      }
    } catch (_e) {
      // fall through
    }
  }
  return null
}

/**
 * Always prepare insert target and skip Crepe onAdd (custom overlay owns the menu).
 * - cell / after-table → focus only
 * - whole-table / normal block → insert empty paragraph after the block
 *
 * @param {import('prosemirror-view').EditorView} view
 * @param {HTMLElement} handleEl
 * @returns {boolean}
 */
export function prepareBlockHandleInsert(view, handleEl) {
  if (isTableBlockHandle(view, handleEl)) {
    return insertParagraphAfterBlockHandle(view, handleEl)
  }
  const kind = focusBlockHandleInsertTarget(view, handleEl)
  if (kind === 'cell' || kind === 'after-table') {
    return true
  }
  return insertParagraphAfterBlockHandle(view, handleEl)
}

/**
 * $table / $card are block-level and must stay outside table cells.
 * $image is an inline atom — use caret insert instead (see isInlineImageDslContent).
 * @param {string} content
 * @returns {boolean}
 */
export function isBlockishDslContent(content) {
  return /^\s*\$(?:table|card)\b/.test(String(content || ''))
}

/**
 * Resolve the enclosing table when the caret (or a captured insert pos) is in a cell.
 *
 * @param {import('prosemirror-view').EditorView} view
 * @param {number|null|undefined} capturedPos
 * @returns {number|null}
 */
export function resolveTablePosAroundSelection(view, capturedPos) {
  if (!view?.state) {
    return null
  }
  if (isSelectionInsideTableCell(view.state.selection)) {
    const pos = findTablePos(view.state.selection.$from)
    if (pos != null) {
      return pos
    }
  }
  if (capturedPos == null) {
    return null
  }
  try {
    const safe = Math.max(0, Math.min(capturedPos, view.state.doc.content.size))
    return findTablePos(view.state.doc.resolve(safe))
  } catch (_e) {
    return null
  }
}

/**
 * Insert block nodes immediately after a table — selection-independent.
 * Avoids Milkdown `insert(md, false)` which uses replaceSelection and can
 * flatten blocks into `cellContent: "paragraph"` when the caret is still in-cell.
 *
 * @param {import('prosemirror-view').EditorView} view
 * @param {number} tablePos
 * @param {import('prosemirror-model').Fragment} fragment
 * @returns {boolean}
 */
export function insertBlockFragmentAfterTable(view, tablePos, fragment) {
  const table = view.state.doc.nodeAt(tablePos)
  if (!table || table.type.name !== 'table' || !fragment || fragment.size < 1) {
    return false
  }

  const afterPos = tablePos + table.nodeSize
  let tr = view.state.tr
  const $after = tr.doc.resolve(Math.min(afterPos, tr.doc.content.size))
  const next = $after.nodeAfter

  if (next && next.type.name === 'paragraph' && next.content.size === 0) {
    tr = tr.replaceWith(afterPos, afterPos + next.nodeSize, fragment)
  } else {
    tr = tr.insert(afterPos, fragment)
  }

  try {
    tr = tr.setSelection(TextSelection.near(tr.doc.resolve(afterPos + 1), 1))
  } catch (_e) {
    // Content is still inserted even if caret placement fails.
  }

  if (isSelectionInsideTableCell(tr.selection)) {
    // Prefer leaving caret outside the table when possible.
    try {
      const safeAfter = tablePos + (tr.doc.nodeAt(tablePos)?.nodeSize || table.nodeSize)
      tr = tr.setSelection(TextSelection.near(tr.doc.resolve(Math.min(safeAfter, tr.doc.content.size)), 1))
    } catch (_e2) {
      // ignore
    }
  }

  view.dispatch(tr.scrollIntoView())
  view.focus()
  return true
}

export { isSelectionInsideTableCell }
