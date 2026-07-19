import {
  CellSelection,
  mergeCells,
  splitCell,
  selectedRect,
  addColSpan
} from 'prosemirror-tables'
import { Fragment } from 'prosemirror-model'

/**
 * Whether the current selection can be merged into one cell.
 * @param {import('prosemirror-state').EditorState} state
 * @returns {boolean}
 */
export function canMergeCells(state) {
  return mergeCells(state, undefined)
}

/**
 * Whether the current selection (or caret cell) can be split.
 * @param {import('prosemirror-state').EditorState} state
 * @returns {boolean}
 */
export function canSplitCell(state) {
  return splitCell(state, undefined)
}

/**
 * Milkdown table cells only allow a single `paragraph`. Stock `mergeCells`
 * appends other cells' paragraph fragments, which violates that content
 * expression and lifts text (e.g. "column2") out of the table.
 *
 * This merge keeps one paragraph and joins plain text instead.
 *
 * @param {import('prosemirror-state').EditorState} state
 * @param {(tr: import('prosemirror-state').Transaction) => void} [dispatch]
 * @returns {boolean}
 */
export function runMergeCells(state, dispatch) {
  if (!mergeCells(state, undefined)) {
    return false
  }
  if (!dispatch) {
    return true
  }

  const rect = selectedRect(state)
  const { map, table, tableStart } = rect
  const tr = state.tr
  const seen = Object.create(null)
  /** @type {number|null} */
  let mergedPos = null
  /** @type {import('prosemirror-model').Node|null} */
  let mergedCell = null
  /** @type {string[]} */
  const texts = []

  for (let row = rect.top; row < rect.bottom; row += 1) {
    for (let col = rect.left; col < rect.right; col += 1) {
      const cellPos = map.map[row * map.width + col]
      const cell = table.nodeAt(cellPos)
      if (seen[cellPos] || !cell) {
        continue
      }
      seen[cellPos] = true
      const text = cell.textBetween(0, cell.content.size, ' ', ' ').replace(/\s+/g, ' ').trim()
      if (text) {
        texts.push(text)
      }
      if (mergedPos == null) {
        mergedPos = cellPos
        mergedCell = cell
      } else {
        const mapped = tr.mapping.map(cellPos + tableStart)
        tr.delete(mapped, mapped + cell.nodeSize)
      }
    }
  }

  if (mergedPos == null || mergedCell == null) {
    return true
  }

  const absMerged = tr.mapping.map(mergedPos + tableStart)
  const nextAttrs = {
    ...addColSpan(
      mergedCell.attrs,
      mergedCell.attrs.colspan,
      rect.right - rect.left - mergedCell.attrs.colspan
    ),
    rowspan: rect.bottom - rect.top
  }
  tr.setNodeMarkup(absMerged, null, nextAttrs)

  const paraType = state.schema.nodes.paragraph
  if (paraType) {
    const combined = texts.join(' ')
    const paragraph = combined
      ? paraType.create(null, state.schema.text(combined))
      : paraType.createAndFill()
    if (paragraph) {
      const cellNode = tr.doc.nodeAt(absMerged)
      if (cellNode) {
        tr.replaceWith(absMerged + 1, absMerged + cellNode.nodeSize - 1, Fragment.from(paragraph))
      }
    }
  }

  try {
    tr.setSelection(CellSelection.create(tr.doc, absMerged))
  } catch (_e) {
    // Selection may fail if table map is mid-repair; attrs/content already applied.
  }
  dispatch(tr)
  return true
}

/**
 * Split a merged cell. Returns true when the command applied.
 * Caret inside a spanned cell is expanded to a CellSelection first.
 * @param {import('prosemirror-state').EditorState} state
 * @param {(tr: import('prosemirror-state').Transaction) => void} [dispatch]
 * @returns {boolean}
 */
export function runSplitCell(state, dispatch) {
  if (state.selection instanceof CellSelection) {
    return splitCell(state, dispatch)
  }
  const cellSel = cellSelectionFromCaret(state)
  if (!cellSel) {
    return false
  }
  const next = state.apply(state.tr.setSelection(cellSel))
  return splitCell(next, dispatch)
}

/**
 * @param {import('prosemirror-model').ResolvedPos} $pos
 * @returns {number} cell depth or -1
 */
function findTableCellDepth($pos) {
  for (let depth = $pos.depth; depth > 0; depth -= 1) {
    const name = $pos.node(depth).type.name
    if (name === 'table_cell' || name === 'table_header') {
      return depth
    }
  }
  return -1
}

/**
 * Expand caret / text selection inside a cell into a CellSelection on that cell.
 * @param {import('prosemirror-state').EditorState} state
 * @returns {import('prosemirror-tables').CellSelection|null}
 */
function cellSelectionFromCaret(state) {
  const depth = findTableCellDepth(state.selection.$from)
  if (depth < 0) {
    return null
  }
  return CellSelection.create(state.doc, state.selection.$from.before(depth))
}

/**
 * True when selection is a CellSelection that may need merge/split.
 * Includes full column/row selections: in a 2-row table, vertical two-cell select
 * is `isColSelection()` — hiding those would make rowspan merge impossible.
 * @param {import('prosemirror-state').Selection} selection
 * @returns {boolean}
 */
export function isMergeToolbarSelection(selection) {
  return selection instanceof CellSelection
}

/**
 * @param {import('prosemirror-state').EditorState} state
 * @returns {{ canMerge: boolean, canSplit: boolean }}
 */
export function getMergeToolbarState(state) {
  if (state.selection instanceof CellSelection) {
    try {
      return {
        canMerge: canMergeCells(state),
        canSplit: canSplitCell(state)
      }
    } catch (_e) {
      return { canMerge: false, canSplit: false }
    }
  }
  const depth = findTableCellDepth(state.selection.$from)
  if (depth < 0) {
    return { canMerge: false, canSplit: false }
  }
  const cell = state.selection.$from.node(depth)
  const colspan = cell.attrs.colspan || 1
  const rowspan = cell.attrs.rowspan || 1
  return {
    canMerge: false,
    canSplit: colspan > 1 || rowspan > 1
  }
}
