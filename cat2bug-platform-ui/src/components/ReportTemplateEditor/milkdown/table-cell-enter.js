import { TextSelection } from 'prosemirror-state'
import { goToNextCell, isInTable } from 'prosemirror-tables'

/**
 * Enter in a table cell: move to the next cell (never exit the table).
 * Always consumes the key when inside a table so Milkdown's ExitTable cannot run.
 *
 * @param {import('prosemirror-state').EditorState} state
 * @param {(tr: import('prosemirror-state').Transaction) => void} [dispatch]
 * @returns {boolean}
 */
export function moveToNextTableCell(state, dispatch) {
  if (!isInTable(state)) {
    return false
  }
  goToNextCell(1)(state, dispatch)
  return true
}

/**
 * Mod-Enter / Ctrl-Enter: insert a hard line break inside the current cell.
 * Does not set the Milkdown `hardbreak` transaction meta — that meta triggers
 * hardbreakFilterPlugin which blocks breaks inside tables.
 *
 * @param {import('prosemirror-state').EditorState} state
 * @param {(tr: import('prosemirror-state').Transaction) => void} [dispatch]
 * @returns {boolean}
 */
export function insertTableCellLineBreak(state, dispatch) {
  if (!isInTable(state)) {
    return false
  }
  const hardbreakType = state.schema.nodes.hardbreak
  if (!hardbreakType) {
    return false
  }
  if (!(state.selection instanceof TextSelection)) {
    return false
  }
  if (dispatch) {
    dispatch(state.tr.replaceSelectionWith(hardbreakType.create()).scrollIntoView())
  }
  return true
}
