import { Plugin, PluginKey } from 'prosemirror-state'
import { $prose } from '@milkdown/kit/utils'
import { applyMultimdSpansToDoc } from '../dsl/parse-multimd-table'
import { getTableCellPlainText } from '../dsl/serialize-multimd-table'

export const tableCellMergePluginKey = new PluginKey('cat2bug-table-cell-merge')

/**
 * Multimd span fix when `^^` markers are pasted.
 * Floating 添加/合并/拆分 UI lives in {@link installTableCellActionToolbar}.
 */
export const tableCellMergePlugin = $prose(() => new Plugin({
  key: tableCellMergePluginKey,
  appendTransaction(transactions, _oldState, newState) {
    const alreadyApplied = transactions.some(
      (tr) => tr.getMeta(tableCellMergePluginKey)?.spansApplied
    )
    if (alreadyApplied) {
      return null
    }
    if (!docHasExplicitRowspanMarker(newState.doc)) {
      return null
    }
    const next = applyMultimdSpansToDoc(newState, { forceEmptyColspan: true })
    if (!next || next.doc.eq(newState.doc)) {
      return null
    }
    return next.setMeta(tableCellMergePluginKey, { spansApplied: true })
  }
}))

/**
 * @param {import('prosemirror-model').Node} doc
 * @returns {boolean}
 */
function docHasExplicitRowspanMarker(doc) {
  let found = false
  doc.descendants((node) => {
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
