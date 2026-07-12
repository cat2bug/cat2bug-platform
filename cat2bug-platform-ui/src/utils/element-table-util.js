/**
 * Element Table column helpers (replaces element-ui/packages/table/src/util).
 * Works with Element Plus el-table refs (store.states.columns) and legacy store shapes.
 */

function unwrapMaybeRef(value) {
  if (value != null && typeof value === 'object' && 'value' in value) {
    return value.value
  }
  return value
}

function getTableStates(table) {
  if (!table) return null
  const store = table.store
  return store?.states || table.states || null
}

function getTableColumns(table) {
  if (!table) return []
  if (Array.isArray(table.columns)) return table.columns
  const states = getTableStates(table)
  if (!states?.columns) return []
  const columns = states.columns
  return columns.value ?? columns
}

export function getElTableStore(table) {
  return table?.store ?? null
}

export function getElTableCurrentRow(table) {
  const states = getTableStates(table)
  if (!states?.currentRow) return null
  return unwrapMaybeRef(states.currentRow)
}

export function getElTableTreeDataMap(table) {
  const states = getTableStates(table)
  if (!states?.treeData) return null
  return unwrapMaybeRef(states.treeData)
}

export function toggleElTableTreeExpansion(table, row, expanded) {
  const store = getElTableStore(table)
  if (!store || typeof store.toggleTreeExpansion !== 'function' || !row) return
  store.toggleTreeExpansion(row, expanded)
}

export function setTableColumns(table, nextColumns) {
  if (!table) return
  const states = getTableStates(table)
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
