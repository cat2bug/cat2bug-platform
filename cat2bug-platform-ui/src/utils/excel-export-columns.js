import { CaseTableColumnDefaults } from '@/views/system/case/case-table-options'
import { TableOptions as DefectTableOptions } from '@/views/system/defect/list/table-options'

/** 与 case/index.vue cat2-bug-table field-list-cache-key 一致 */
export const CASE_FIELD_LIST_KEY = 'case-table-field-list'

/**
 * 与 defect/list/table.vue cache-key="defect-table" 及 Cat2BugTable columnsStorageKey()
 * （cacheKey + 'defect-table-field-list'）一致
 */
export const DEFECT_FIELD_LIST_KEY = 'defect-tabledefect-table-field-list'

function readCachedColumns(storageKey, cache) {
  if (!storageKey || !cache) return null
  let cached = cache.getJSON(storageKey)
  if (cached == null) {
    const raw = cache.get(storageKey)
    if (raw) {
      try {
        cached = typeof raw === 'string' ? JSON.parse(raw) : raw
      } catch (e) {
        cached = null
      }
    }
  }
  return cached
}

/** 与 Cat2BugTable.mergeCachedColumns 逻辑一致 */
export function mergeTableColumns(defaults, storageKey, cache) {
  const list = defaults && defaults.length ? defaults.map(d => ({ ...d })) : []
  if (!storageKey || !list.length) {
    return list
  }
  const cached = readCachedColumns(storageKey, cache)
  if (!cached || !Array.isArray(cached) || cached.length === 0) {
    return list
  }
  if (typeof cached[0] === 'string') {
    const visibleKeys = new Set(cached)
    return list.map(d => ({ ...d, visible: visibleKeys.has(d.key) }))
  }
  const defaultByKey = {}
  list.forEach(d => {
    defaultByKey[d.key] = d
  })
  const merged = []
  const used = new Set()
  cached.forEach(c => {
    const base = defaultByKey[c.key]
    if (!base) return
    merged.push({
      ...base,
      fixed: !!c.fixed,
      visible: c.visible !== false
    })
    used.add(c.key)
  })
  list.forEach(d => {
    if (!used.has(d.key)) merged.push({ ...d })
  })
  return merged
}

function columnConfigEntry(col) {
  return {
    key: col.key,
    prop: col.prop,
    visible: col.visible !== false
  }
}

/**
 * @param {Array} columns 合并后的完整列（含隐藏列）
 * @param {'data'|'importTemplate'} scope
 * @param {'case'|'defect'} module
 */
export function buildExportParams(columns, scope, module) {
  void module
  return {
    exportScope: scope,
    exportColumns: JSON.stringify((columns || []).map(columnConfigEntry))
  }
}

export function getColumnsFromCat2BugTable(tableRef) {
  if (!tableRef) return null
  if (typeof tableRef.getColumnConfigForExport === 'function') {
    return tableRef.getColumnConfigForExport()
  }
  if (tableRef.tableFieldList) {
    return tableRef.tableFieldList.map(columnConfigEntry)
  }
  return null
}

export function appendExportColumnParams(payload, columns, scope, module) {
  const exportParams = buildExportParams(columns, scope, module)
  payload.params = { ...(payload.params || {}), ...exportParams }
  return payload
}

export function getDefaultColumnsForModule(module) {
  if (module === 'case') {
    return CaseTableColumnDefaults.map(c => ({ ...c }))
  }
  if (module === 'defect') {
    return DefectTableOptions.map(c => ({ ...c }))
  }
  return []
}
