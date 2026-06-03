import { defectTableColumnKeyFromFieldKey } from '@/utils/defect-field-layout'
import { defectDisplayFieldPickerOptions } from '@/utils/defect-display-field'

/** 详情「缺陷字段」中不展示的属性（另有独立区块或已在标题区展示） */
export const DEFECT_DETAIL_SKIP_TABLE_KEYS = new Set([
  'id',
  'defect.name',
  'describe'
])

/** 详情区固定展示的统计项（不在「显示字段」勾选列表中） */
export const DEFECT_DETAIL_EXTRA_ROWS = Object.freeze([
  { fieldKey: '__lifeTime', labelKey: 'defect.life-time' },
  { fieldKey: '__rejectCount', labelKey: 'reject' }
])

/**
 * 按字段管理顺序 + 「显示字段」显隐，解析详情页要展示的列配置。
 * @param {Array} mergedColumns resolveDefectMergedColumns 结果
 * @param {{ orderedEnabledFieldKeys?: string[], customFields?: Array }} layout listColumnLayout 数据
 */
export function resolveOrderedVisibleDetailColumns(mergedColumns, layout) {
  const picker = defectDisplayFieldPickerOptions(mergedColumns)
  const pickerByKey = new Map(picker.map((c) => [String(c.key), c]))
  const customFields = (layout && layout.customFields) || []
  const customByFieldKey = new Map(customFields.map((f) => [String(f.fieldKey), f]))

  const orderedFieldKeys =
    layout && layout.orderedEnabledFieldKeys && layout.orderedEnabledFieldKeys.length
      ? layout.orderedEnabledFieldKeys
      : null

  const out = []
  const seen = new Set()

  const tryPushTableKey = (tableKey) => {
    if (!tableKey || seen.has(tableKey) || DEFECT_DETAIL_SKIP_TABLE_KEYS.has(tableKey)) {
      return
    }
    let col = pickerByKey.get(String(tableKey))
    if (!col && String(tableKey).startsWith('custom:')) {
      const fk = String(tableKey).slice(7)
      const def = customByFieldKey.get(fk)
      if (def) {
        col = {
          key: tableKey,
          prop: tableKey,
          visible: true,
          isCustomField: true,
          customFieldKey: fk,
          fieldLabel: def.fieldLabel,
          customFieldMeta: def,
          fieldType: def.fieldType,
          typeConfig: def.typeConfig
        }
      }
    }
    if (!col && tableKey === 'case') {
      col = { key: 'case', prop: 'caseId', visible: true, label: 'case' }
    }
    if (!col || col.visible === false) return
    seen.add(tableKey)
    out.push(col)
  }

  if (orderedFieldKeys) {
    orderedFieldKeys.forEach((fieldKey) => {
      tryPushTableKey(defectTableColumnKeyFromFieldKey(fieldKey))
    })
  } else {
    picker.forEach((c) => tryPushTableKey(c.key))
  }

  return out
}
