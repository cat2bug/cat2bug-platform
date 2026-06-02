import { CaseTableColumnDefaults } from '@/views/system/case/case-table-options'
import { TableOptions as DefectTableOptions } from '@/views/system/defect/list/table-options'
import {
  buildDefectTableColumnDefaults,
  loadDefectColumnLayout,
  mergeDefectTableColumnPrefs
} from '@/utils/defect-custom-field-columns'

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
    return list.map(d => {
      const key = d && d.key
      if (visibleKeys.has(key)) {
        return { ...d, visible: true }
      }
      // 旧版 string 缓存未包含的新自定义列：默认显示（与 syncNewDefectTableColumnsIntoFieldListCache 意图一致）
      if (key && String(key).startsWith('custom:')) {
        return { ...d, visible: true }
      }
      return { ...d, visible: false }
    })
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
  const entry = {
    key: col.key,
    prop: col.prop,
    visible: col.visible !== false
  }
  if (col.required || (col.customFieldMeta && col.customFieldMeta.required)) {
    entry.required = true
  }
  return entry
}

function getRefExportConfig(excelRef, tableRef) {
  if (excelRef && typeof excelRef.getColumnConfigForExport === 'function') {
    const cols = excelRef.getColumnConfigForExport()
    if (cols && cols.length) return cols
  }
  if (tableRef) {
    return getColumnsFromCat2BugTable(tableRef)
  }
  return null
}

/**
 * 以项目列布局为准合并导出列，再用当前表格/Excel 的显隐覆盖；避免 tableFieldList 未加载完时漏掉自定义列。
 */
function mergeDefectExportColumns(layoutColumns, refConfig) {
  const layoutByKey = new Map()
  ;(layoutColumns || []).forEach(col => {
    if (col && col.key != null) layoutByKey.set(String(col.key), col)
  })

  const visByKey = new Map()
  const reqByKey = new Map()
  if (refConfig && refConfig.length) {
    refConfig.forEach(c => {
      const k = String(c.key)
      visByKey.set(k, c.visible !== false)
      if (c.required) reqByKey.set(k, true)
    })
  }

  const orderedKeys = []
  if (refConfig && refConfig.length) {
    refConfig.forEach(c => {
      const k = String(c.key)
      if (layoutByKey.has(k) && !orderedKeys.includes(k)) orderedKeys.push(k)
    })
  }
  ;(layoutColumns || []).forEach(col => {
    const k = String(col.key)
    if (!orderedKeys.includes(k)) orderedKeys.push(k)
  })

  return orderedKeys
    .map(key => {
      const col = layoutByKey.get(key)
      if (!col) return null
      const merged = { ...col }
      if (visByKey.has(key)) {
        merged.visible = visByKey.get(key)
      } else if (String(key).startsWith('custom:')) {
        // 导出数据：布局中已启用但未出现在当前表格列配置里的自定义字段，默认导出
        merged.visible = true
      }
      if (reqByKey.has(key)) {
        merged.required = true
      }
      return merged
    })
    .filter(Boolean)
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

/**
 * 解析缺陷导出/导入模版列配置（含自定义字段与本地 field-list 缓存）。
 * @param {{ projectId?: number, cache?: object, tableRef?: object, excelRef?: object }} options
 */
export async function resolveDefectExportColumns(options = {}) {
  const { projectId, cache, tableRef, excelRef } = options
  const base = DefectTableOptions.map(c => ({ ...c }))
  const pid = Number(projectId)
  let layoutColumns = base
  if (pid) {
    const layout = await loadDefectColumnLayout(pid)
    layoutColumns = buildDefectTableColumnDefaults(base, layout)
  }
  layoutColumns = mergeDefectTableColumnPrefs(cache, layoutColumns, DEFECT_FIELD_LIST_KEY)
  const refConfig = getRefExportConfig(excelRef, tableRef)
  return mergeDefectExportColumns(layoutColumns, refConfig)
}
