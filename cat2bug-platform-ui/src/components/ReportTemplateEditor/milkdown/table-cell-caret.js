import { NodeSelection, TextSelection } from 'prosemirror-state'

const TABLE_CELL_TYPES = new Set(['table_cell', 'table_header'])
const INLINE_ATOM_TYPES = new Set(['cat2bug_variable', 'cat2bug_plan_variable'])

/**
 * @param {import('prosemirror-state').Selection} selection
 * @returns {boolean}
 */
export function isSelectionInsideTableCell(selection) {
  const $from = selection.$from
  for (let depth = $from.depth; depth > 0; depth -= 1) {
    if (TABLE_CELL_TYPES.has($from.node(depth).type.name)) {
      return true
    }
  }
  return false
}

/**
 * Milkdown table-block selects the cell paragraph (NodeSelection) on first click,
 * which hides the text caret. Convert that (and in-cell variable atoms) to TextSelection.
 *
 * @param {import('prosemirror-state').EditorState} state
 * @returns {import('prosemirror-state').TextSelection|null}
 */
export function resolveTableCellTextSelection(state) {
  const { selection, doc } = state
  if (!(selection instanceof NodeSelection)) {
    return null
  }
  if (!isSelectionInsideTableCell(selection)) {
    return null
  }

  const { node } = selection
  if (node.type.name === 'paragraph') {
    return TextSelection.near(doc.resolve(selection.from + 1), 1)
  }
  if (INLINE_ATOM_TYPES.has(node.type.name)) {
    return TextSelection.near(doc.resolve(selection.to), 1)
  }
  return null
}
