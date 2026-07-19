import { registerCat2bugDslRemarkPlugin, registerCat2bugRawRemarkPlugin } from './remark-cat2bug-dsl'
import {
  cat2bugDefectTableNode,
  cat2bugCaseCardNode,
  cat2bugDynamicImageNode,
  cat2bugVariableNode,
  cat2bugPlanVariableNode,
  cat2bugRawBlockNode
} from './cat2bug-nodes'
import {
  cat2bugDefectTableView,
  cat2bugCaseCardView,
  cat2bugDynamicImageView,
  cat2bugVariableView,
  cat2bugPlanVariableView,
  cat2bugRawBlockView
} from './cat2bug-views'
import { tableCellCaretPlugin } from './table-cell-caret-plugin'
import { tableHandlePopupPlugin } from './table-handle-popup-plugin'
import { tableCellMergePlugin } from './table-cell-merge-plugin'
import { tableCellEnterKeymap } from './table-cell-enter-keymap'
import { tableCellHtmlBreakPlugin } from './table-cell-html-break-plugin'
import { staticHtmlImageView } from './static-html-image-view'
import { tableKeymap } from '@milkdown/kit/preset/gfm'

export const reportTablePlugins = [
  // Materialized `<img style>` HTML atoms → real image + resize (no DSL chrome)
  staticHtmlImageView,
  // Milkdown table-block first-click uses NodeSelection on the cell paragraph — restore text caret
  tableCellCaretPlugin,
  // Milkdown pointermove clears column/row .button-group — keep it while CellSelection active
  tableHandlePopupPlugin,
  // CellSelection merge/split toolbar + Multimd span apply
  tableCellMergePlugin,
  // Enter → next cell; Mod-Enter → line break in cell (override GFM ExitTable)
  tableCellEnterKeymap,
  // Parse GFM cell `<br>` html atoms back into hardbreak
  tableCellHtmlBreakPlugin
]

export const reportDslPlugins = [
  registerCat2bugRawRemarkPlugin,
  registerCat2bugDslRemarkPlugin,
  cat2bugDefectTableNode,
  cat2bugCaseCardNode,
  cat2bugDynamicImageNode,
  cat2bugVariableNode,
  cat2bugPlanVariableNode,
  cat2bugRawBlockNode,
  cat2bugDefectTableView,
  cat2bugCaseCardView,
  cat2bugDynamicImageView,
  cat2bugVariableView,
  cat2bugPlanVariableView,
  cat2bugRawBlockView,
  ...reportTablePlugins
]

/**
 * @param {import('@milkdown/core').Editor} editor
 * @param {{ enableDsl?: boolean }} [options]
 */
export function useReportDslPlugins(editor, options = {}) {
  const enableDsl = options.enableDsl !== false
  editor.config((ctx) => {
    // Clear GFM ExitTable (Enter / Mod-Enter) so they cannot race our table keymap.
    ctx.update(tableKeymap.key, (km) => ({
      ...km,
      ExitTable: {
        ...km.ExitTable,
        shortcuts: []
      }
    }))
  })
  editor.use(enableDsl ? reportDslPlugins : reportTablePlugins)
}
