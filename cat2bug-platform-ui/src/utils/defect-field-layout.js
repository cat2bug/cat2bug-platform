/**
 * 缺陷内置字段启用配置与 TableOptions / Excel COLS 的映射。
 */

/** TableOptions.key → 内置 fieldKey */
export const TABLE_KEY_TO_BUILTIN_FIELD_KEY = Object.freeze({
  id: 'projectNum',
  type: 'defectType',
  'defect.name': 'defectName',
  'handle-by': 'handleBy',
  priority: 'defectLevel',
  state: 'defectState',
  module: 'moduleId',
  version: 'moduleVersion',
  case: 'caseId',
  describe: 'defectDescribe',
  image: 'imgUrls',
  annex: 'annexUrls',
  'plan-start-time': 'planStartTime',
  'plan-end-time': 'planEndTime',
  createBy: 'createMember',
  'update-time': 'updateTime'
})

/** Excel COLS.key → 内置 fieldKey */
export const EXCEL_COL_KEY_TO_BUILTIN_FIELD_KEY = Object.freeze({
  projectNum: 'projectNum',
  defectType: 'defectType',
  defectName: 'defectName',
  excelHandleByMemberId: 'handleBy',
  defectStateText: 'defectState',
  moduleName: 'moduleId',
  moduleVersion: 'moduleVersion',
  defectDescribe: 'defectDescribe',
  excelImgUrlsText: 'imgUrls',
  excelAnnexUrlsText: 'annexUrls',
  planStartTime: 'planStartTime',
  planEndTime: 'planEndTime',
  createByText: 'createMember',
  updateTime: 'updateTime'
})

/** 内置 fieldKey → Excel COLS.key */
export const BUILTIN_FIELD_KEY_TO_EXCEL_COL_KEY = Object.freeze(
  Object.fromEntries(
    Object.entries(EXCEL_COL_KEY_TO_BUILTIN_FIELD_KEY).map(([excelKey, fieldKey]) => [fieldKey, excelKey])
  )
)

/** 内置 fieldKey 全集（API 失败时回退为全部启用） */
export const ALL_BUILTIN_FIELD_KEYS = Object.freeze(
  Object.values(TABLE_KEY_TO_BUILTIN_FIELD_KEY).filter(Boolean)
)

/** 字段管理中不允许关闭「启用」的内置 fieldKey（显示字段勾选不受此限制） */
export const ALWAYS_ENABLED_BUILTIN_FIELD_KEYS = Object.freeze(['defectName', 'handleBy'])

export function isBuiltinFieldAlwaysEnabled(fieldKey) {
  return ALWAYS_ENABLED_BUILTIN_FIELD_KEYS.includes(String(fieldKey))
}

/** 内置 fieldKey → 缺陷列表 TableOptions.key */
export const BUILTIN_FIELD_KEY_TO_TABLE_KEY = Object.freeze(
  Object.fromEntries(
    Object.entries(TABLE_KEY_TO_BUILTIN_FIELD_KEY).map(([tableKey, fieldKey]) => [fieldKey, tableKey])
  )
)

/** 字段管理中的 fieldKey 转为表格列 key（custom:xxx 或内置 table key） */
export function defectTableColumnKeyFromFieldKey(fieldKey) {
  if (!fieldKey) return null
  const builtinTableKey = BUILTIN_FIELD_KEY_TO_TABLE_KEY[fieldKey]
  if (builtinTableKey) return builtinTableKey
  return `custom:${fieldKey}`
}

/**
 * 按字段管理 orderedEnabledFieldKeys 重排 Excel COLS 顺序；无配置时保持原序。
 */
export function orderExcelColsByFieldManage(cols, orderedEnabledFieldKeys) {
  if (!cols || !cols.length || !orderedEnabledFieldKeys || !orderedEnabledFieldKeys.length) {
    return cols || []
  }
  const byKey = {}
  cols.forEach(col => {
    if (col && col.key) byKey[col.key] = col
  })
  const ordered = []
  const used = new Set()
  if (byKey.projectNum) {
    ordered.push(byKey.projectNum)
    used.add('projectNum')
  }
  orderedEnabledFieldKeys.forEach(fieldKey => {
    let excelKey = BUILTIN_FIELD_KEY_TO_EXCEL_COL_KEY[fieldKey]
    if (!excelKey && fieldKey) {
      excelKey = `custom:${fieldKey}`
    }
    if (!excelKey || used.has(excelKey) || !byKey[excelKey]) return
    ordered.push(byKey[excelKey])
    used.add(excelKey)
  })
  cols.forEach(col => {
    if (col && col.key && !used.has(col.key)) {
      ordered.push(col)
    }
  })
  return ordered
}

/**
 * 按字段管理 orderedEnabledFieldKeys 重排列顺序；无配置时保持原序。
 */
export function orderDefectTableColumnsByFieldManage(columns, orderedEnabledFieldKeys) {
  if (!columns || !columns.length || !orderedEnabledFieldKeys || !orderedEnabledFieldKeys.length) {
    return columns || []
  }
  const byKey = {}
  columns.forEach(col => {
    if (col && col.key) byKey[col.key] = col
  })
  const ordered = []
  const used = new Set()
  orderedEnabledFieldKeys.forEach(fieldKey => {
    const tableKey = defectTableColumnKeyFromFieldKey(fieldKey)
    if (!tableKey || used.has(tableKey) || !byKey[tableKey]) return
    ordered.push(byKey[tableKey])
    used.add(tableKey)
  })
  columns.forEach(col => {
    if (col && col.key && !used.has(col.key)) {
      ordered.push(col)
    }
  })
  return ordered
}

/** keys 为 null/undefined 时回退为全部启用；空数组表示全部关闭 */
export function normalizeEnabledBuiltinKeys(keys) {
  if (keys == null) {
    return new Set(ALL_BUILTIN_FIELD_KEYS)
  }
  const set = new Set((keys || []).map(k => String(k)))
  ALWAYS_ENABLED_BUILTIN_FIELD_KEYS.forEach(k => set.add(k))
  return set
}

export function isBuiltinTableColumnEnabled(columnKey, enabledBuiltinKeys) {
  if (!columnKey || String(columnKey).startsWith('custom:')) {
    return true
  }
  const builtinKey = TABLE_KEY_TO_BUILTIN_FIELD_KEY[columnKey]
  if (builtinKey === null) {
    return true
  }
  if (builtinKey === undefined) {
    return true
  }
  if (isBuiltinFieldAlwaysEnabled(builtinKey)) {
    return true
  }
  return enabledBuiltinKeys.has(builtinKey)
}

export function filterTableOptionsByBuiltin(options, enabledBuiltinKeys) {
  const enabled = normalizeEnabledBuiltinKeys(enabledBuiltinKeys)
  return (options || []).filter(col => isBuiltinTableColumnEnabled(col.key, enabled))
}

export function filterExcelColsByBuiltin(cols, enabledBuiltinKeys) {
  const enabled = normalizeEnabledBuiltinKeys(enabledBuiltinKeys)
  return (cols || []).filter(col => {
    const builtinKey = EXCEL_COL_KEY_TO_BUILTIN_FIELD_KEY[col.key]
    if (builtinKey === null) {
      return true
    }
    if (builtinKey === undefined) {
      return String(col.key || '').startsWith('custom:') || true
    }
    if (isBuiltinFieldAlwaysEnabled(builtinKey)) {
      return true
    }
    return enabled.has(builtinKey)
  })
}

/** 允许写入列缓存的 key 集合（编号 + 启用的内置/自定义列） */
export function buildAllowedDefectTableColumnKeys(tableColumns) {
  return (tableColumns || []).map(c => c && c.key).filter(Boolean)
}

export function isBuiltinFormFieldVisible(fieldKey, enabledBuiltinKeys) {
  if (!fieldKey) {
    return false
  }
  const enabled = normalizeEnabledBuiltinKeys(
    enabledBuiltinKeys instanceof Set
      ? Array.from(enabledBuiltinKeys)
      : enabledBuiltinKeys
  )
  if (isBuiltinFieldAlwaysEnabled(fieldKey)) {
    return true
  }
  return enabled.has(String(fieldKey))
}
