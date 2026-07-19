import { Plugin } from '@milkdown/kit/prose/state'
import { $prose } from '@milkdown/kit/utils'
import { buildConvertTableHtmlBreakTransaction } from './table-cell-html-break'

/**
 * After parse/sync, turn table-cell html `<br>` atoms into hardbreak nodes.
 */
export const tableCellHtmlBreakPlugin = $prose(() => new Plugin({
  appendTransaction(transactions, _oldState, newState) {
    if (!transactions.some((tr) => tr.docChanged)) {
      return null
    }
    return buildConvertTableHtmlBreakTransaction(newState)
  }
}))
