import { listColumnLayout } from '@/api/system/defect-field'
import { normalizeFieldRow } from '@/components/DefectCustomField/format'
import {
  buildAllowedDefectTableColumnKeys,
  filterTableOptionsByBuiltin,
  orderDefectTableColumnsByFieldManage
} from '@/utils/defect-field-layout'

/** 表格列 prop / 排序字段是否为自定义字段（非物理列，不可直接 ORDER BY） */
export function isCustomFieldTableProp(prop) {
  if (prop == null || prop === '') return false
  const s = String(prop)
  return s.startsWith('custom_') || s.startsWith('custom:')
}

/** 缺陷列表 orderByColumn：映射展示列 prop → 库字段；自定义列返回 null */
export function resolveDefectTableOrderByColumn(prop) {
  if (prop == null || prop === '') return null
  if (isCustomFieldTableProp(prop)) return null
  switch (prop) {
    case 'defectStateName':
      return 'defectState'
    case 'defectTypeName':
      return 'defectType'
    default:
      return prop
  }
}

/** 构建表格/Excel 动态列定义 */
export function buildCustomFieldColumns(fields) {
  return (fields || []).map(f => {
    const row = normalizeFieldRow(f)
    const width =
      row.fieldType === 'image' ? 220 : row.fieldType === 'file' ? 400 : 140
    return {
      key: `custom:${row.fieldKey}`,
      prop: `custom_${row.fieldKey}`,
      label: row.fieldLabel,
      customFieldKey: row.fieldKey,
      customFieldMeta: row,
      fixed: false,
      visible: true,
      showInColumnPicker: true,
      sortable: false,
      align: 'left',
      width
    }
  })
}

/** 按内置启用配置过滤 TableOptions，并追加自定义列 */
export function buildDefectTableColumnDefaults(tableOptions, layout) {
  const enabledBuiltinKeys = (layout && layout.enabledBuiltinFieldKeys) || null
  const customFields = mapEnabledFields((layout && layout.customFields) || [])
  const base = filterTableOptionsByBuiltin(tableOptions, enabledBuiltinKeys)
  const combined = [...base, ...buildCustomFieldColumns(customFields)]
  return orderDefectTableColumnsByFieldManage(
    combined,
    layout && layout.orderedEnabledFieldKeys
  )
}

/** 与 Cat2BugTable cache-key="defect-table" + TABLE_FIELD_LIST_CACHE_KEY 一致 */
export const DEFECT_TABLE_COLUMN_CACHE_KEY = 'defect-tabledefect-table-field-list'

let _cache = { projectId: null, layout: null, promise: null }

function mapEnabledFields(data) {
  return (data || [])
    .filter(f => f.enabled !== 0 && f.enabled !== false)
    .map(normalizeFieldRow)
}

/** 按项目缓存列布局；字段定义变更后请 force 或 clearCustomFieldColumnsCache */
export function loadDefectColumnLayout(projectId, options = {}) {
  const { force = false } = options
  const pid = Number(projectId)
  if (!pid) {
    _cache = { projectId: null, layout: null, promise: null }
    return Promise.resolve({ enabledBuiltinFieldKeys: [], customFields: [], orderedEnabledFieldKeys: [] })
  }
  if (!force && _cache.projectId === pid && _cache.layout) {
    return Promise.resolve(_cache.layout)
  }
  if (!force && _cache.projectId === pid && _cache.promise) {
    return _cache.promise
  }
  _cache.projectId = pid
  _cache.promise = listColumnLayout(pid).then(res => {
    const layout = res.data || { enabledBuiltinFieldKeys: [], customFields: [], orderedEnabledFieldKeys: [] }
    _cache.layout = layout
    _cache.promise = null
    return layout
  }).catch(() => {
    _cache.layout = { enabledBuiltinFieldKeys: null, customFields: [], orderedEnabledFieldKeys: [] }
    _cache.promise = null
    return _cache.layout
  })
  return _cache.promise
}

/** @deprecated 使用 loadDefectColumnLayout */
export function loadEnabledCustomFields(projectId, options = {}) {
  return loadDefectColumnLayout(projectId, options).then(layout => mapEnabledFields(layout.customFields))
}

export function clearCustomFieldColumnsCache() {
  _cache = { projectId: null, layout: null, promise: null }
}

/** 是否已有缺陷列表「显示字段」本地缓存 */
export function hasDefectTableColumnCache(localCache, storageKey = DEFECT_TABLE_COLUMN_CACHE_KEY) {
  const cached = readFieldListCache(localCache, storageKey)
  return !!(cached && cached.length)
}

function readFieldListCache(localCache, storageKey = DEFECT_TABLE_COLUMN_CACHE_KEY) {
  if (!localCache) return null
  let cached = localCache.getJSON(storageKey)
  if (cached == null) {
    const raw = localCache.get(storageKey)
    if (raw) {
      try {
        cached = typeof raw === 'string' ? JSON.parse(raw) : raw
      } catch (e) {
        cached = null
      }
    }
  }
  return cached && Array.isArray(cached) ? cached : null
}

/**
 * 合并缺陷表列配置：visible / fixed 来自本地缓存；
 * 缓存为对象数组时，列顺序与缓存一致（表格/Excel 拖列与「显示字段」列表同步）。
 */
export function mergeDefectTableColumnPrefs(localCache, defaults, storageKey = DEFECT_TABLE_COLUMN_CACHE_KEY) {
  const list = (defaults || []).map(d => ({ ...d }))
  const cached = readFieldListCache(localCache, storageKey)
  if (!cached || !cached.length) {
    return list
  }
  if (typeof cached[0] === 'string') {
    const visibleKeys = new Set(cached)
    return list.map(d => {
      const key = d && d.key
      if (visibleKeys.has(key)) {
        return { ...d, visible: true }
      }
      if (key && String(key).startsWith('custom:')) {
        return { ...d, visible: true }
      }
      return { ...d, visible: false }
    })
  }
  const prefByKey = {}
  cached.forEach(c => {
    if (c && c.key) prefByKey[c.key] = c
  })
  const defaultByKey = {}
  list.forEach(d => {
    if (d && d.key) defaultByKey[d.key] = d
  })
  const ordered = []
  const seen = new Set()
  cached.forEach(c => {
    if (!c || !c.key) return
    const base = defaultByKey[c.key]
    if (!base) return
    ordered.push({
      ...base,
      fixed: !!c.fixed,
      visible: c.visible !== false
    })
    seen.add(c.key)
  })
  list.forEach(d => {
    if (!d || !d.key || seen.has(d.key)) return
    const pref = prefByKey[d.key]
    ordered.push({
      ...d,
      fixed: pref ? !!pref.fixed : !!d.fixed,
      visible: pref ? pref.visible !== false : d.visible !== false
    })
  })
  return ordered
}

/**
 * 将 tableColumns 中尚未写入本地缓存的列默认加入「显示字段」并设为选中（visible）。
 * 用于新启用/新建的自定义字段及重新启用的内置字段。
 */
export function syncNewDefectTableColumnsIntoFieldListCache(
  localCache,
  tableColumns,
  storageKey = DEFECT_TABLE_COLUMN_CACHE_KEY
) {
  if (!localCache || !tableColumns || !tableColumns.length) return

  const cached = readFieldListCache(localCache, storageKey)
  const columns = tableColumns.map(c => ({ ...c }))

  if (!cached || !cached.length) {
    localCache.setJSON(
      storageKey,
      columns.map(c => ({
        key: c.key,
        prop: c.prop,
        visible: c.visible !== false,
        fixed: !!c.fixed
      }))
    )
    return
  }

  if (typeof cached[0] === 'string') {
    const keys = [...cached]
    let changed = false
    columns.forEach(col => {
      if (!col || !col.key || keys.includes(col.key)) return
      keys.push(col.key)
      changed = true
    })
    if (changed) {
      localCache.setJSON(storageKey, keys)
    }
    return
  }

  const existing = new Set(cached.map(c => c && c.key).filter(Boolean))
  const additions = []
  columns.forEach(col => {
    if (!col || !col.key || existing.has(col.key)) return
    additions.push({
      key: col.key,
      prop: col.prop,
      visible: col.visible !== false,
      fixed: !!col.fixed
    })
  })
  if (additions.length) {
    localCache.setJSON(storageKey, [...cached, ...additions])
  }
}

/** 从本地列缓存中移除已停用/删除的列 */
export function pruneDefectTableColumnCache(
  localCache,
  allowedColumnKeys,
  storageKey = DEFECT_TABLE_COLUMN_CACHE_KEY
) {
  if (!localCache || !allowedColumnKeys) return
  const allowed = new Set(allowedColumnKeys)
  let cached = localCache.getJSON(storageKey)
  if (cached == null) {
    const raw = localCache.get(storageKey)
    if (raw) {
      try {
        cached = typeof raw === 'string' ? JSON.parse(raw) : raw
      } catch (e) {
        cached = null
      }
    }
  }
  if (!cached || !Array.isArray(cached) || !cached.length) return

  const keep = (key) => allowed.has(key)

  let pruned
  if (typeof cached[0] === 'string') {
    pruned = cached.filter(keep)
  } else {
    pruned = cached.filter(c => c && c.key && keep(c.key))
  }
  if (pruned.length !== cached.length) {
    localCache.setJSON(storageKey, pruned)
  }
}

export function pruneDefectTableColumnCacheFromColumns(
  localCache,
  tableColumns,
  storageKey = DEFECT_TABLE_COLUMN_CACHE_KEY
) {
  pruneDefectTableColumnCache(
    localCache,
    buildAllowedDefectTableColumnKeys(tableColumns),
    storageKey
  )
}
