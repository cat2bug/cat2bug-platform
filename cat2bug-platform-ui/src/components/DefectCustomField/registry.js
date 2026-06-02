/**
 * 缺陷自定义字段类型注册表（editor/display 由 DefectCustomFieldsSection / DefectCustomFieldsDisplay 统一渲染）
 */
export const CUSTOM_FIELD_TYPES = [
  'string',
  'number',
  'boolean',
  'datetime',
  'enum',
  'object',
  'image',
  'file',
  'array'
]

export function isCustomFieldType(type) {
  return CUSTOM_FIELD_TYPES.includes(type)
}
