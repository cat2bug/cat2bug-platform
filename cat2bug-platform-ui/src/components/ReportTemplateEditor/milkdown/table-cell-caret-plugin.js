import { Plugin } from '@milkdown/kit/prose/state'
import { $prose } from '@milkdown/kit/utils'
import { resolveTableCellTextSelection } from './table-cell-caret'

/**
 * Append a text caret when Milkdown table cell click would otherwise leave a NodeSelection.
 */
export const tableCellCaretPlugin = $prose(() => new Plugin({
  appendTransaction(_transactions, _oldState, newState) {
    const textSelection = resolveTableCellTextSelection(newState)
    if (!textSelection || textSelection.eq(newState.selection)) {
      return null
    }
    return newState.tr.setSelection(textSelection)
  }
}))
