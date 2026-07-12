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
  cat2bugRawBlockView
]

export function useReportDslPlugins(editor) {
  editor.use(reportDslPlugins)
}
