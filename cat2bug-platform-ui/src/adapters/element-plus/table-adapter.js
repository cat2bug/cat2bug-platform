/**
 * Element Plus el-table ref / store adapter.
 * Re-exports table helpers (also used via element-ui shim path).
 */
export {
  getElTableStore,
  getElTableCurrentRow,
  getElTableTreeDataMap,
  toggleElTableTreeExpansion,
  setTableColumns,
  getTableColumns,
  getColumnById
} from '@/utils/element-table-util'

export const TABLE_SELECTORS = {
  bodyRow: '.el-table__body tbody tr',
  body: '.el-table__body'
}

export function queryTableBodyRows(tableEl) {
  if (!tableEl || typeof tableEl.querySelectorAll !== 'function') return []
  return Array.from(tableEl.querySelectorAll(TABLE_SELECTORS.bodyRow))
}

export function findTableBodyRowFromActiveElement(activeEl) {
  if (!activeEl || !activeEl.closest) return null
  return activeEl.closest(TABLE_SELECTORS.bodyRow)
}
