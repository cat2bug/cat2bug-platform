import { resolveOrderedDefectTabFilterFields, DEFECT_TAB_NON_FILTERABLE_CUSTOM_TYPES } from '@/utils/defect-tab-filter-fields'

describe('resolveOrderedDefectTabFilterFields', () => {
  const alwaysVisible = () => true
  const layout = {
    enabledBuiltinFieldKeys: ['defectType', 'handleBy', 'moduleId', 'custom_tag'],
    orderedEnabledFieldKeys: [
      'handleBy',
      'custom_tag',
      'defectType',
      'moduleId',
      'img_field'
    ],
    customFields: [
      { fieldKey: 'custom_tag', fieldLabel: '标签', fieldType: 'string', enabled: 1 },
      { fieldKey: 'img_field', fieldLabel: '截图', fieldType: 'image', enabled: 1 },
      { fieldKey: 'obj_field', fieldLabel: '对象', fieldType: 'object', enabled: 1 }
    ]
  }

  it('orders builtin and custom filters by display field order', () => {
    const fields = resolveOrderedDefectTabFilterFields(null, layout, {
      isBuiltinVisible: alwaysVisible
    })
    expect(fields.map(f => (f.kind === 'custom' ? `custom:${f.fieldKey}` : f.formKey))).toEqual([
      'handleBy',
      'custom:custom_tag',
      'defectType',
      'moduleId'
    ])
  })

  it('excludes image file object custom types', () => {
    const fields = resolveOrderedDefectTabFilterFields(null, layout, {
      isBuiltinVisible: alwaysVisible
    })
    expect(fields.some(f => f.fieldKey === 'img_field')).toBe(false)
    expect(fields.some(f => f.fieldKey === 'obj_field')).toBe(false)
  })

  it('respects DEFECT_TAB_NON_FILTERABLE_CUSTOM_TYPES export', () => {
    expect(DEFECT_TAB_NON_FILTERABLE_CUSTOM_TYPES.has('image')).toBe(true)
    expect(DEFECT_TAB_NON_FILTERABLE_CUSTOM_TYPES.has('boolean')).toBe(false)
  })
})
