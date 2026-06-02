import {
  DEFECT_TABLE_COLUMN_CACHE_KEY,
  mergeDefectTableColumnPrefs
} from '@/utils/defect-custom-field-columns'

export { DEFECT_TABLE_COLUMN_CACHE_KEY }

/** 合并字段管理默认列 + 本地显隐/固定偏好（顺序以 defaults 为准） */
export function resolveDefectMergedColumns(
  localCache,
  tableColumnDefaults,
  storageKey = DEFECT_TABLE_COLUMN_CACHE_KEY
) {
  return mergeDefectTableColumnPrefs(localCache, tableColumnDefaults || [], storageKey)
}

/** 「显示字段」勾选列表：与表格列顺序一致，仅含可配置列 */
export function defectDisplayFieldPickerOptions(mergedColumns) {
  return (mergedColumns || [])
    .filter((c) => c.showInColumnPicker !== false)
    .map((c) => ({ ...c }))
}

/** 当前应勾选的 TableOptions.key */
export function defectDisplayFieldCheckedKeys(mergedColumns) {
  return defectDisplayFieldPickerOptions(mergedColumns)
    .filter((c) => c.visible !== false)
    .map((c) => c.key)
}

/**
 * 按 merged 表格列顺序映射为 Excel 数据列 key（COLS.key，不含 projectNum）。
 * @param {Array} mergedColumns buildDefectTableColumnDefaults + merge 后的列
 * @param {Array} colDefs 通常为 excel.vue 的 COLS
 */
export function excelDataColumnKeysFromMerged(mergedColumns, colDefs) {
  const keys = []
  const seen = new Set()
  ;(mergedColumns || []).forEach((m) => {
    if (!m || m.key == null) return
    const tk = String(m.key)
    if (tk === 'id') return
    if (m.isCustomField || tk.startsWith('custom:')) {
      if (!seen.has(tk)) {
        keys.push(tk)
        seen.add(tk)
      }
      return
    }
    const col = (colDefs || []).find(
      (c) =>
        c &&
        c.key !== 'projectNum' &&
        c.titleKey != null &&
        c.titleKey !== '' &&
        String(c.titleKey) === tk
    )
    if (col && !seen.has(col.key)) {
      keys.push(col.key)
      seen.add(col.key)
    }
  })
  return keys
}

/** 按 key 列表重排列定义，未出现在 order 中的列保持原相对顺序接在末尾 */
export function reorderColsByKeyOrder(cols, order) {
  if (!cols || !cols.length) return []
  if (!order || !order.length) return cols
  const byKey = new Map(cols.map((c) => [c.key, c]))
  const out = []
  const seen = new Set()
  order.forEach((k) => {
    const c = byKey.get(k)
    if (c) {
      out.push(c)
      seen.add(k)
    }
  })
  cols.forEach((c) => {
    if (!seen.has(c.key)) out.push(c)
  })
  return out
}

export function sameStringArrayOrder(a, b) {
  if (!a && !b) return true
  if (!a || !b || a.length !== b.length) return false
  return a.every((v, i) => String(v) === String(b[i]))
}
