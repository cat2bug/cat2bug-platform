import { TableOptions } from '@/views/system/defect/list/table-options'
import {
  buildDefectTableColumnDefaults,
  DEFECT_TABLE_COLUMN_CACHE_KEY,
  mergeDefectTableColumnPrefs
} from '@/utils/defect-custom-field-columns'
import { normalizeFieldRow } from '@/components/DefectCustomField/format'
import { TABLE_KEY_TO_BUILTIN_FIELD_KEY } from '@/utils/defect-field-layout'

/** 标签页筛选不展示的内置 fieldKey（名称关键字在弹框顶部单独配置） */
export const DEFECT_TAB_FILTER_SKIP_BUILTIN_KEYS = Object.freeze(
  new Set([
    'projectNum',
    'defectName',
    'defectDescribe',
    'imgUrls',
    'annexUrls',
    'planStartTime',
    'planEndTime',
    'updateTime'
  ])
)

/** 标签页筛选不展示的表格列 key */
const TAB_FILTER_SKIP_TABLE_KEYS = new Set([
  'id',
  'defect.name',
  'describe',
  'image',
  'annex',
  'plan-start-time',
  'plan-end-time',
  'update-time'
])

/** 自定义字段中不参与标签页筛选的类型 */
export const DEFECT_TAB_NON_FILTERABLE_CUSTOM_TYPES = Object.freeze(
  new Set(['image', 'file', 'object'])
)

const DEFAULT_TAB_FILTER_BUILTIN_ORDER = Object.freeze([
  'defectType',
  'defectLevel',
  'handleBy',
  'moduleVersion',
  'moduleId',
  'caseId',
  'defectState',
  'createMember'
])

function mapEnabledCustomFields(customFields) {
  return (customFields || [])
    .filter(f => f.enabled !== 0 && f.enabled !== false)
    .map(normalizeFieldRow)
    .filter(f => !DEFECT_TAB_NON_FILTERABLE_CUSTOM_TYPES.has(f.fieldType))
}

export function isCustomFieldTabFilterable(field) {
  return field && !DEFECT_TAB_NON_FILTERABLE_CUSTOM_TYPES.has(field.fieldType)
}

function pushBuiltin(out, seen, formKey, fieldKey) {
  if (!formKey || seen.has(formKey)) return
  seen.add(formKey)
  out.push({ kind: 'builtin', formKey, fieldKey })
}

function tryPushBuiltinFromTableKey(out, seen, tableKey, isBuiltinVisible) {
  if (!tableKey || TAB_FILTER_SKIP_TABLE_KEYS.has(tableKey)) return
  if (String(tableKey).startsWith('custom:')) return

  const builtinKey = TABLE_KEY_TO_BUILTIN_FIELD_KEY[tableKey]
  if (!builtinKey || DEFECT_TAB_FILTER_SKIP_BUILTIN_KEYS.has(builtinKey)) return
  if (!isBuiltinVisible(builtinKey)) return
  pushBuiltin(out, seen, builtinKey, builtinKey)
}

function tryPushCustomFromTableKey(out, seen, tableKey, customByFieldKey) {
  if (!tableKey || !String(tableKey).startsWith('custom:')) return
  const fieldKey = String(tableKey).slice(7)
  if (!fieldKey || seen.has(`custom:${fieldKey}`)) return
  const meta = customByFieldKey.get(fieldKey)
  if (!meta || !isCustomFieldTabFilterable(meta)) return
  seen.add(`custom:${fieldKey}`)
  out.push({ kind: 'custom', formKey: fieldKey, fieldKey, meta })
}

function appendMissingBuiltin(out, seen, isBuiltinVisible) {
  DEFAULT_TAB_FILTER_BUILTIN_ORDER.forEach(fieldKey => {
    if (DEFECT_TAB_FILTER_SKIP_BUILTIN_KEYS.has(fieldKey)) return
    if (!isBuiltinVisible(fieldKey)) return
    pushBuiltin(out, seen, fieldKey, fieldKey)
  })
}

function appendMissingCustom(out, seen, customFields) {
  customFields.forEach(meta => {
    const fieldKey = meta.fieldKey
    if (!fieldKey || seen.has(`custom:${fieldKey}`)) return
    seen.add(`custom:${fieldKey}`)
    out.push({ kind: 'custom', formKey: fieldKey, fieldKey, meta })
  })
}

/**
 * 按「显示字段」顺序解析新建标签页筛选表单项（内置 + 可筛选自定义字段）。
 */
export function resolveOrderedDefectTabFilterFields(localCache, layout, options = {}) {
  const isBuiltinVisible =
    typeof options.isBuiltinVisible === 'function'
      ? options.isBuiltinVisible
      : () => true

  const customFields = mapEnabledCustomFields(layout && layout.customFields)
  const customByFieldKey = new Map(customFields.map(f => [String(f.fieldKey), f]))
  const out = []
  const seen = new Set()

  const defaults = buildDefectTableColumnDefaults(
    TableOptions.map(c => ({ ...c })),
    layout || {}
  )
  const merged = mergeDefectTableColumnPrefs(
    localCache,
    defaults,
    DEFECT_TABLE_COLUMN_CACHE_KEY
  )

  if (merged && merged.length) {
    merged.forEach(col => {
      const tableKey = col && col.key
      if (!tableKey) return
      if (String(tableKey).startsWith('custom:')) {
        tryPushCustomFromTableKey(out, seen, tableKey, customByFieldKey)
      } else {
        tryPushBuiltinFromTableKey(out, seen, tableKey, isBuiltinVisible)
      }
    })
  } else if (layout && layout.orderedEnabledFieldKeys && layout.orderedEnabledFieldKeys.length) {
    layout.orderedEnabledFieldKeys.forEach(fieldKey => {
      const fk = String(fieldKey)
      if (customByFieldKey.has(fk)) {
        tryPushCustomFromTableKey(out, seen, `custom:${fk}`, customByFieldKey)
        return
      }
      if (DEFECT_TAB_FILTER_SKIP_BUILTIN_KEYS.has(fk)) return
      if (!isBuiltinVisible(fk)) return
      pushBuiltin(out, seen, fk, fk)
    })
  } else {
    appendMissingBuiltin(out, seen, isBuiltinVisible)
    appendMissingCustom(out, seen, customFields)
    return out
  }

  appendMissingBuiltin(out, seen, isBuiltinVisible)
  appendMissingCustom(out, seen, customFields)
  return out
}
