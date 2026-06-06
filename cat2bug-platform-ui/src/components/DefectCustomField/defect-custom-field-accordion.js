/** 处理缺陷抽屉：自定义属性风箱在 el-collapse 中的 name 前缀 */

import { isDefectFieldDisplayEmpty } from '@/components/DefectCustomField/DefectCustomFieldEmpty'

export const CUSTOM_FIELD_ACCORDION_PREFIX = 'custom:'

export function customFieldAccordionCollapseName(fieldKey) {
  return `${CUSTOM_FIELD_ACCORDION_PREFIX}${fieldKey}`
}

export function isCustomFieldAccordionCollapseName(name) {
  return typeof name === 'string' && name.startsWith(CUSTOM_FIELD_ACCORDION_PREFIX)
}

export function accordionRowHasContent(row) {
  if (!row) return false
  if (row.fieldType === 'image' || row.fieldType === 'file') {
    return row.urls && row.urls.length > 0
  }
  if (row.fieldType === 'object') {
    return !isDefectFieldDisplayEmpty(row.display)
  }
  return false
}
