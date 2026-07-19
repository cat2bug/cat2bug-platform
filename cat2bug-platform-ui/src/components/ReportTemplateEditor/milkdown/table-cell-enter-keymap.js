import { $shortcut } from '@milkdown/kit/utils'
import { insertTableCellLineBreak, moveToNextTableCell } from './table-cell-enter'

/**
 * High-priority table Enter / Mod-Enter shortcuts.
 * Milkdown GFM maps both keys to ExitTable (insert paragraph after table).
 */
export const tableCellEnterKeymap = $shortcut(() => ({
  Enter: {
    key: 'Enter',
    priority: 200,
    onRun: () => moveToNextTableCell
  },
  'Mod-Enter': {
    key: 'Mod-Enter',
    priority: 200,
    onRun: () => insertTableCellLineBreak
  }
}))
