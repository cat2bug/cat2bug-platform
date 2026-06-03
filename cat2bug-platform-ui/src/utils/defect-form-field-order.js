import { TableOptions } from '@/views/system/defect/list/table-options'
import {
  buildDefectTableColumnDefaults,
  DEFECT_TABLE_COLUMN_CACHE_KEY,
  mergeDefectTableColumnPrefs
} from '@/utils/defect-custom-field-columns'
import { normalizeFieldRow } from '@/components/DefectCustomField/format'
import {
  ALL_BUILTIN_FIELD_KEYS,
  TABLE_KEY_TO_BUILTIN_FIELD_KEY
} from '@/utils/defect-field-layout'

/** 新增/编辑表单不展示的内置 fieldKey */
export const DEFECT_FORM_SKIP_BUILTIN_KEYS = Object.freeze(
  new Set(['projectNum', 'defectState', 'createMember', 'updateTime'])
)

/** 新增/编辑表单不展示的表格列 key */
const FORM_SKIP_TABLE_KEYS = new Set(['id', 'state', 'createBy', 'update-time'])

/** 表单默认内置字段顺序（无字段管理配置时） */
const DEFAULT_FORM_BUILTIN_ORDER = Object.freeze([
  'defectName',
  'defectType',
  'defectLevel',
  'handleBy',
  'moduleVersion',
  'moduleId',
  'planStartTime',
  'caseId',
  'defectDescribe',
  'imgUrls',
  'annexUrls'
])

function mapEnabledCustomFields(customFields) {
  return (customFields || [])
    .filter(f => f.enabled !== 0 && f.enabled !== false)
    .map(normalizeFieldRow)
}

function pushBuiltinFormField(out, seen, formKey, fieldKey) {
  if (!formKey || seen.has(formKey)) return
  seen.add(formKey)
  out.push({ kind: 'builtin', formKey, fieldKey })
}

function pushPlanTimeField(out, seen, isBuiltinVisible) {
  if (seen.has('planTime')) return
  if (!isBuiltinVisible('planStartTime') && !isBuiltinVisible('planEndTime')) return
  pushBuiltinFormField(out, seen, 'planTime', 'planStartTime')
}

function tryPushBuiltinFromTableKey(out, seen, tableKey, isBuiltinVisible) {
  if (!tableKey || FORM_SKIP_TABLE_KEYS.has(tableKey)) return

  if (String(tableKey).startsWith('custom:')) {
    return
  }

  const builtinKey = TABLE_KEY_TO_BUILTIN_FIELD_KEY[tableKey]
  if (!builtinKey || DEFECT_FORM_SKIP_BUILTIN_KEYS.has(builtinKey)) return

  if (builtinKey === 'planStartTime' || builtinKey === 'planEndTime') {
    pushPlanTimeField(out, seen, isBuiltinVisible)
    return
  }

  if (!isBuiltinVisible(builtinKey)) return
  pushBuiltinFormField(out, seen, builtinKey, builtinKey)
}

function tryPushCustomFromTableKey(out, seen, tableKey, customByFieldKey) {
  if (!tableKey || !String(tableKey).startsWith('custom:')) return
  const fieldKey = String(tableKey).slice(7)
  if (!fieldKey || seen.has(`custom:${fieldKey}`)) return
  const meta = customByFieldKey.get(fieldKey)
  if (!meta) return
  seen.add(`custom:${fieldKey}`)
  out.push({ kind: 'custom', formKey: fieldKey, fieldKey, meta })
}

function appendMissingBuiltinFields(out, seen, isBuiltinVisible) {
  DEFAULT_FORM_BUILTIN_ORDER.forEach(fieldKey => {
    if (DEFECT_FORM_SKIP_BUILTIN_KEYS.has(fieldKey)) return
    if (fieldKey === 'planStartTime') {
      pushPlanTimeField(out, seen, isBuiltinVisible)
      return
    }
    if (!isBuiltinVisible(fieldKey)) return
    pushBuiltinFormField(out, seen, fieldKey, fieldKey)
  })
}

function appendMissingCustomFields(out, seen, customFields) {
  customFields.forEach(meta => {
    const fieldKey = meta.fieldKey
    if (!fieldKey || seen.has(`custom:${fieldKey}`)) return
    seen.add(`custom:${fieldKey}`)
    out.push({ kind: 'custom', formKey: fieldKey, fieldKey, meta })
  })
}

/**
 * 按「显示字段」列顺序解析新增/编辑表单字段（内置 + 自定义）。
 * @param {object|null} localCache 与缺陷列表相同的 $cache.local
 * @param {object|null} layout listColumnLayout 数据
 * @param {{ isBuiltinVisible: (fieldKey: string) => boolean }} options
 */
export function resolveOrderedDefectFormFields(localCache, layout, options = {}) {
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
      if (DEFECT_FORM_SKIP_BUILTIN_KEYS.has(fk)) return
      if (fk === 'planStartTime' || fk === 'planEndTime') {
        pushPlanTimeField(out, seen, isBuiltinVisible)
        return
      }
      if (!isBuiltinVisible(fk)) return
      pushBuiltinFormField(out, seen, fk, fk)
    })
  } else {
    appendMissingBuiltinFields(out, seen, isBuiltinVisible)
    appendMissingCustomFields(out, seen, customFields)
    return out
  }

  appendMissingBuiltinFields(out, seen, isBuiltinVisible)
  appendMissingCustomFields(out, seen, customFields)
  return out
}

/** 无布局时的内置 fieldKey 全集（测试/回退） */
export function allDefectFormBuiltinFieldKeys() {
  return ALL_BUILTIN_FIELD_KEYS.filter(k => !DEFECT_FORM_SKIP_BUILTIN_KEYS.has(k))
}
