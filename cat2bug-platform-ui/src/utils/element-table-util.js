/**
 * Element Table column helpers (replaces element-ui/packages/table/src/util).
 * Works with Element Plus el-table refs (store.states.columns) and legacy store shapes.
 */

function getTableColumns(table) {
  if (!table) return []
  if (Array.isArray(table.columns)) return table.columns
  const states = table.store?.states || table.states
  if (!states?.columns) return []
  const columns = states.columns
  return columns.value ?? columns
}

export function setTableColumns(table, nextColumns) {
  if (!table) return
  const states = table.store?.states || table.states
  if (!states?.columns) return
  if (states.columns != null && typeof states.columns === 'object' && 'value' in states.columns) {
    states.columns.value = nextColumns
    return
  }
  states.columns = nextColumns
}

export { getTableColumns }

export function getColumnById(table, columnId) {
  const columns = getTableColumns(table)
  return columns.find(column => column.id === columnId) || null
}

export default {
  getColumnById
}
