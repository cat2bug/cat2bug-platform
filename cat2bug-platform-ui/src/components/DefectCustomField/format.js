/** 不支持在字段管理中配置默认值的类型 */
export const FIELD_TYPES_WITHOUT_DEFAULT = ['image', 'file']

export function supportsFieldDefaultValue(fieldType) {
  return fieldType && FIELD_TYPES_WITHOUT_DEFAULT.indexOf(fieldType) === -1
}

/** 仅字符串类型可配置最大长度 */
export function supportsFieldMaxLength(fieldType) {
  return fieldType === 'string'
}

/** 解析 type_config JSON */
export function parseTypeConfig(field) {
  if (!field || !field.typeConfig) return {}
  if (typeof field.typeConfig === 'object') return field.typeConfig
  try {
    return JSON.parse(field.typeConfig)
  } catch (e) {
    return {}
  }
}

/** 解析 type_config 原始值（字符串或对象） */
export function parseTypeConfigRaw(typeConfig) {
  if (!typeConfig) return {}
  if (typeof typeConfig === 'object') return typeConfig
  try {
    return JSON.parse(typeConfig)
  } catch (e) {
    return {}
  }
}

/** 从 type_config 提取枚举选项行（编辑表单用） */
export function enumOptionsFromTypeConfig(typeConfig) {
  const cfg = parseTypeConfigRaw(typeConfig)
  const raw = cfg && cfg.options
  if (raw == null) return []
  let list = []
  if (Array.isArray(raw)) {
    list = raw
  } else if (typeof raw === 'object') {
    list = Object.values(raw)
  } else if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      return enumOptionsFromTypeConfig({ options: parsed })
    } catch (e) {
      return []
    }
  }
  return list
    .filter(opt => opt != null)
    .map((opt, index) => {
      if (typeof opt === 'string' || typeof opt === 'number' || typeof opt === 'boolean') {
        const key = String(opt).trim()
        return { key, label: key, color: '#409EFF' }
      }
      const label = opt.label != null ? String(opt.label).trim() : ''
      let key = opt.key != null ? String(opt.key).trim() : ''
      if (!key && opt.value != null) key = String(opt.value).trim()
      if (!key && label) key = label
      if (!key) key = String(index)
      return {
        key,
        label: label || key,
        color: opt.color || '#409EFF'
      }
    })
    .filter(opt => opt.key !== '')
}

/** 枚举存库/表单值统一为字符串，便于与 el-option value 严格匹配 */
export function normalizeEnumFieldValue(value) {
  if (value == null || value === '') return value
  return String(value)
}

export function enumOptions(field) {
  return enumOptionsFromTypeConfig(field && field.typeConfig)
}

export function enumLabel(field, key) {
  const opt = enumOptions(field).find(o => o.key === key)
  return opt ? (opt.label || opt.key) : key
}

/** 枚举项展示色（下拉选项文字、已选值） */
export function enumOptionTextStyle(opt) {
  const color = opt && opt.color
  if (!color) return {}
  return { color }
}

/** 已选枚举值：通过 CSS 变量作用于 el-select 输入框文字色 */
export function enumSelectCssVars(fieldOrTypeConfig, selectedKey) {
  if (selectedKey == null || selectedKey === '') return {}
  const opts = fieldOrTypeConfig && fieldOrTypeConfig.fieldType
    ? enumOptions(fieldOrTypeConfig)
    : enumOptionsFromTypeConfig(fieldOrTypeConfig && fieldOrTypeConfig.typeConfig != null
      ? fieldOrTypeConfig.typeConfig
      : fieldOrTypeConfig)
  const opt = opts.find(o => o.key === normalizeEnumFieldValue(selectedKey))
  if (!opt || !opt.color) return {}
  return { '--defect-enum-color': opt.color }
}

export function formatCustomFieldValue(field, value) {
  if (value === undefined || value === null || value === '') return ''
  const type = field && field.fieldType
  if (type === 'boolean') {
    return value === true || value === 'true' ? 'true' : 'false'
  }
  if (type === 'enum') {
    return enumLabel(field, value)
  }
  if (type === 'datetime') {
    return String(value)
  }
  if (type === 'image' || type === 'file') {
    const arr = Array.isArray(value) ? value : String(value).split(',').filter(Boolean)
    return arr.join(', ')
  }
  if (type === 'array') {
    if (!Array.isArray(value)) return String(value)
    return value.map(v => (typeof v === 'object' ? JSON.stringify(v) : String(v))).join(', ')
  }
  if (type === 'object') {
    return typeof value === 'string' ? value : JSON.stringify(value, null, 0)
  }
  return String(value)
}

/** 提交前将 image/file 逗号串转为 URL 数组（与后端 string[] 约定一致） */
export function urlListFromCustomFieldValue(value) {
  if (value == null || value === '') return []
  if (Array.isArray(value)) {
    return value.map(v => String(v).trim()).filter(Boolean)
  }
  return String(value).split(',').map(s => s.trim()).filter(Boolean)
}

export function normalizeCustomFieldsForSubmit(customFields, fieldList) {
  const out = { ...(customFields || {}) }
  if (!fieldList || !fieldList.length) return out
  fieldList.forEach(f => {
    if (f.fieldType !== 'image' && f.fieldType !== 'file') return
    const key = f.fieldKey
    if (!(key in out)) return
    const raw = out[key]
    if (raw == null || raw === '') {
      delete out[key]
      return
    }
    const urls = urlListFromCustomFieldValue(raw)
    if (urls.length) {
      out[key] = urls
    } else {
      delete out[key]
    }
  })
  return out
}

export function isEmptyCustomFieldValue(value) {
  if (value === undefined || value === null || value === '') return true
  if (Array.isArray(value)) return value.length === 0
  if (typeof value === 'object') return Object.keys(value).length === 0
  return false
}

/** 将字段定义中的默认值转为表单初始值 */
export function cloneDefaultFieldValue(field, defaultValue) {
  if (isEmptyCustomFieldValue(defaultValue)) return undefined
  const type = field && field.fieldType
  if (type === 'image' || type === 'file') {
    const urls = urlListFromCustomFieldValue(defaultValue)
    return urls.length ? urls : undefined
  }
  if (type === 'object' || type === 'array') {
    try {
      return JSON.parse(JSON.stringify(defaultValue))
    } catch (e) {
      return defaultValue
    }
  }
  return defaultValue
}

export function normalizeFieldRow(f) {
  return {
    fieldId: f.fieldId,
    fieldKey: f.fieldKey,
    fieldLabel: f.fieldLabel,
    fieldType: f.fieldType,
    maxLength: f.maxLength,
    required: f.required === 1 || f.required === true,
    nullable: f.nullable === 1 || f.nullable === true,
    enabled: f.enabled !== 0 && f.enabled !== false,
    sortOrder: f.sortOrder,
    typeConfig: f.typeConfig,
    defaultValue: f.defaultValue
  }
}
