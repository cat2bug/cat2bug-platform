import { resolveOrderedDefectFormFields } from '@/utils/defect-form-field-order'

describe('resolveOrderedDefectFormFields', () => {
  const alwaysVisible = () => true
  const layout = {
    enabledBuiltinFieldKeys: [
      'defectName',
      'defectType',
      'handleBy',
      'custom_priority'
    ],
    orderedEnabledFieldKeys: [
      'defectName',
      'custom_priority',
      'defectType',
      'handleBy',
      'moduleId'
    ],
    customFields: [
      {
        fieldKey: 'custom_priority',
        fieldLabel: '优先级扩展',
        fieldType: 'string',
        enabled: 1
      }
    ]
  }

  it('orders custom fields between builtins per display field order', () => {
    const fields = resolveOrderedDefectFormFields(null, layout, {
      isBuiltinVisible: alwaysVisible
    })
    expect(fields.map(f => (f.kind === 'custom' ? `custom:${f.fieldKey}` : f.formKey))).toEqual([
      'defectName',
      'custom:custom_priority',
      'defectType',
      'handleBy',
      'moduleId'
    ])
  })

  it('merges plan start/end into single planTime field', () => {
    const planLayout = {
      orderedEnabledFieldKeys: ['defectName', 'planEndTime', 'planStartTime'],
      customFields: []
    }
    const fields = resolveOrderedDefectFormFields(null, planLayout, {
      isBuiltinVisible: alwaysVisible
    })
    expect(fields.filter(f => f.formKey === 'planTime')).toHaveLength(1)
    expect(fields.findIndex(f => f.formKey === 'planTime')).toBe(1)
  })

  it('skips disabled builtin fields', () => {
    const fields = resolveOrderedDefectFormFields(null, layout, {
      isBuiltinVisible: key => key !== 'moduleId'
    })
    expect(fields.some(f => f.formKey === 'moduleId')).toBe(false)
  })
})
