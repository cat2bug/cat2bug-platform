import { listColumnLayout } from '@/api/system/defect-field'
import { normalizeFieldRow } from '@/components/DefectCustomField/format'
import {
  buildAllowedDefectTableColumnKeys,
  filterTableOptionsByBuiltin,
  orderDefectTableColumnsByFieldManage
} from '@/utils/defect-field-layout'
import { getCurrentUserId } from '@/utils/store-context'

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

/** 列表格内水平居中的自定义字段类型 */
const CENTER_ALIGNED_CUSTOM_FIELD_TYPES = new Set(['number', 'enum', 'datetime', 'image'])

export function resolveCustomFieldColumnAlign(fieldType) {
  return CENTER_ALIGNED_CUSTOM_FIELD_TYPES.has(fieldType) ? 'center' : 'left'
}

export function resolveCustomFieldColumnClassName(fieldType) {
  if (fieldType === 'image') return 'cat2bug-table-image-col'
  if (fieldType === 'number' || fieldType === 'enum' || fieldType === 'datetime') {
    return 'cat2bug-table-center-col'
  }
  return undefined
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
      align: resolveCustomFieldColumnAlign(row.fieldType),
      className: resolveCustomFieldColumnClassName(row.fieldType),
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

/** 与 Cat2BugTable cache-key="defect-table" + TABLE_FIELD_LIST_CACHE_KEY 一致（基名，实际读写见 getDefectTableColumnCacheKey） */
export const DEFECT_TABLE_COLUMN_CACHE_KEY = 'defect-tabledefect-table-field-list'

/**
 * 与 Cat2BugTable：cache-key + 'defect-table-field-list' 对齐。
 * 按登录用户隔离，避免同浏览器换账号仍用上一人的列显隐。
 */
export function getDefectTableCacheKeyPrefix(userId) {
  const uid = userId != null && userId !== '' ? userId : getCurrentUserId()
  if (uid == null || uid === '') return 'defect-table'
  return `defect-table:u${uid}`
}

export function getDefectTableColumnCacheKey(userId) {
  return `${getDefectTableCacheKeyPrefix(userId)}defect-table-field-list`
}

const MIN_DEFECT_TABLE_COLUMN_WIDTH = 60

function sanitizeColumnWidth(value) {
  if (value == null || value === '') return undefined
  const n = Number(value)
  if (!Number.isFinite(n)) return undefined
  return Math.max(MIN_DEFECT_TABLE_COLUMN_WIDTH, Math.round(n))
}

/** 从 field-list 缓存条目解析用户列宽；无效或未设置时返回 undefined（保留 base 默认） */
export function resolveColumnWidthFromPrefs(pref, base, locale) {
  const fromPref = sanitizeColumnWidth(pref && pref.width)
  if (fromPref !== undefined) return fromPref
  if (base && locale) {
    const localeWidth = sanitizeColumnWidth(base['width_' + locale])
    if (localeWidth !== undefined) return localeWidth
  }
  return sanitizeColumnWidth(base && base.width)
}

export function buildDefectTableColumnPrefEntry(col) {
  if (!col || col.key == null || col.key === '') return null
  const entry = {
    key: col.key,
    prop: col.prop,
    visible: col.visible !== false,
    fixed: !!col.fixed
  }
  const w = sanitizeColumnWidth(col.width)
  if (w !== undefined) entry.width = w
  return entry
}

/**
 * 按当前列数组顺序写入个人 field-list 缓存（key 序即个人权威列序）。
 * 表格拖序 / Excel 拖序 / 显示字段拖序均应走此方法，避免只改内存不落盘。
 */
export function persistDefectTableColumnPrefs(localCache, columns, storageKey) {
  if (!localCache || typeof localCache.setJSON !== 'function') return
  const key = storageKey || getDefectTableColumnCacheKey()
  const payload = (columns || [])
    .map((c) => buildDefectTableColumnPrefEntry(c))
    .filter(Boolean)
  if (!payload.length) return
  localCache.setJSON(key, payload)
}

function mergeColumnWithCachedPrefs(base, pref) {
  const merged = {
    ...base,
    fixed: pref ? !!pref.fixed : !!base.fixed,
    visible: pref ? pref.visible !== false : base.visible !== false
  }
  const w = pref ? sanitizeColumnWidth(pref.width) : undefined
  if (w !== undefined) merged.width = w
  return merged
}

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
  }).catch(err => {
    /* 失败不写入「全开默认」缓存，否则同项目后续读会一直看到默认属性 */
    _cache.promise = null
    if (_cache.projectId === pid) {
      _cache.layout = null
      _cache.projectId = null
    }
    return Promise.reject(err)
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
export function hasDefectTableColumnCache(localCache, storageKey) {
  const key = storageKey || getDefectTableColumnCacheKey()
  const cached = readFieldListCache(localCache, key)
  return !!(cached && cached.length)
}

function readFieldListCache(localCache, storageKey) {
  if (!localCache) return null
  const key = storageKey || getDefectTableColumnCacheKey()
  let cached = localCache.getJSON(key)
  if (cached == null) {
    const raw = localCache.get(key)
    if (raw) {
      try {
        cached = typeof raw === 'string' ? JSON.parse(raw) : raw
      } catch (e) {
        cached = null
      }
    }
  }
  /* 兼容升级前未按用户隔离的旧键，读到后迁到用户键 */
  if ((!cached || !Array.isArray(cached) || !cached.length) && key !== DEFECT_TABLE_COLUMN_CACHE_KEY) {
    const legacy = localCache.getJSON(DEFECT_TABLE_COLUMN_CACHE_KEY)
    if (legacy && Array.isArray(legacy) && legacy.length) {
      localCache.setJSON(key, legacy)
      return legacy
    }
  }
  return cached && Array.isArray(cached) ? cached : null
}

/**
 * 将 newKey 插入 orderedKeys：跟在团队序中「仍出现在 orderedKeys 里的最近前驱」之后；
 * 无前驱则追加末尾。
 * @param {string[]} orderedKeys
 * @param {string} newKey
 * @param {string[]} teamKeys defaults 顺序
 */
export function insertKeyAfterTeamPredecessor(orderedKeys, newKey, teamKeys) {
  if (!orderedKeys || newKey == null || newKey === '') return
  if (orderedKeys.includes(newKey)) return
  const team = teamKeys || []
  const teamIdx = team.indexOf(newKey)
  if (teamIdx > 0) {
    for (let i = teamIdx - 1; i >= 0; i--) {
      const pred = team[i]
      const pos = orderedKeys.indexOf(pred)
      if (pos >= 0) {
        orderedKeys.splice(pos + 1, 0, newKey)
        return
      }
    }
  }
  orderedKeys.push(newKey)
}

/**
 * 合并缺陷表列配置：对象缓存的 key 顺序为个人权威列序；
 * 团队 defaults 提供列定义、新字段落点（前驱后插入）与旧 string[] 缓存时的列序；
 * visible / fixed / width 仍按 key 从缓存覆盖。
 */
export function mergeDefectTableColumnPrefs(localCache, defaults, storageKey) {
  const key = storageKey || getDefectTableColumnCacheKey()
  const list = (defaults || []).map(d => ({ ...d }))
  const cached = readFieldListCache(localCache, key)
  if (!cached || !cached.length) {
    return list
  }
  if (typeof cached[0] === 'string') {
    const visibleKeys = new Set(cached)
    return list.map(d => {
      const colKey = d && d.key
      if (visibleKeys.has(colKey)) {
        return { ...d, visible: true }
      }
      if (colKey && String(colKey).startsWith('custom:')) {
        return { ...d, visible: true }
      }
      return { ...d, visible: false }
    })
  }
  const byKey = new Map()
  list.forEach(d => {
    if (d && d.key) byKey.set(d.key, d)
  })
  const teamKeys = list.map(d => d.key).filter(Boolean)
  const prefByKey = {}
  cached.forEach(c => {
    if (c && c.key) prefByKey[c.key] = c
  })
  const outKeys = []
  cached.forEach(c => {
    if (c && c.key && byKey.has(c.key) && !outKeys.includes(c.key)) {
      outKeys.push(c.key)
    }
  })
  teamKeys.forEach(k => {
    if (outKeys.includes(k)) return
    insertKeyAfterTeamPredecessor(outKeys, k, teamKeys)
  })
  return outKeys.map(k => mergeColumnWithCachedPrefs(byKey.get(k), prefByKey[k] || null))
}

/**
 * 将 tableColumns 中尚未写入本地缓存的列默认加入「显示字段」并设为选中（visible）。
 * 用于新启用/新建的自定义字段及重新启用的内置字段。
 */
export function syncNewDefectTableColumnsIntoFieldListCache(
  localCache,
  tableColumns,
  storageKey
) {
  if (!localCache || !tableColumns || !tableColumns.length) return
  const key = storageKey || getDefectTableColumnCacheKey()

  const cached = readFieldListCache(localCache, key)
  const columns = tableColumns.map(c => ({ ...c }))

  if (!cached || !cached.length) {
    localCache.setJSON(
      key,
      columns.map(buildDefectTableColumnPrefEntry)
    )
    return
  }

  if (typeof cached[0] === 'string') {
    /* 旧版仅显隐：升级为对象数组，列序暂用团队 defaults（首次拖序后再记个人序） */
    const visibleKeys = new Set(cached)
    const upgraded = columns.map((col) => {
      const entry = buildDefectTableColumnPrefEntry(col)
      if (!entry) return null
      entry.visible = visibleKeys.has(col.key) || String(col.key).startsWith('custom:')
      return entry
    }).filter(Boolean)
    if (upgraded.length) {
      localCache.setJSON(key, upgraded)
    }
    return
  }

  const teamKeys = columns.map(c => c && c.key).filter(Boolean)
  const teamKeySet = new Set(teamKeys)
  const next = cached
    .filter(c => c && c.key && teamKeySet.has(c.key))
    .map(c => ({ ...c }))
  /* 勿在 teamKeys 暂空/错位时把个人缓存写成 []，否则刷新后只剩团队默认序 */
  if (!next.length && cached.length && teamKeys.length) {
    return
  }
  if (!next.length && !teamKeys.length) {
    return
  }
  const existing = new Set(next.map(c => c.key))
  let changed = next.length !== cached.length
  columns.forEach(col => {
    if (!col || !col.key || existing.has(col.key)) return
    const entry = buildDefectTableColumnPrefEntry(col)
    if (!entry) return
    const keys = next.map(c => c.key)
    insertKeyAfterTeamPredecessor(keys, col.key, teamKeys)
    const at = keys.indexOf(col.key)
    if (at < 0) {
      next.push(entry)
    } else {
      next.splice(at, 0, entry)
    }
    existing.add(col.key)
    changed = true
  })
  if (changed && next.length) {
    localCache.setJSON(key, next)
  }
}

/** 从本地列缓存中移除已停用/删除的列 */
export function pruneDefectTableColumnCache(
  localCache,
  allowedColumnKeys,
  storageKey
) {
  if (!localCache || !allowedColumnKeys) return
  const key = storageKey || getDefectTableColumnCacheKey()
  const allowed = new Set(allowedColumnKeys)
  let cached = readFieldListCache(localCache, key)
  if (!cached || !Array.isArray(cached) || !cached.length) return

  const keep = (k) => allowed.has(k)

  let pruned
  if (typeof cached[0] === 'string') {
    pruned = cached.filter(keep)
  } else {
    pruned = cached.filter(c => c && c.key && keep(c.key))
  }
  if (pruned.length !== cached.length) {
    localCache.setJSON(key, pruned)
  }
}

export function pruneDefectTableColumnCacheFromColumns(
  localCache,
  tableColumns,
  storageKey
) {
  pruneDefectTableColumnCache(
    localCache,
    buildAllowedDefectTableColumnKeys(tableColumns),
    storageKey || getDefectTableColumnCacheKey()
  )
}
