import { resolveOrderedDefectFormFields } from '@/utils/defect-form-field-order'

/** 任意 cache key 都返回同一份个人 field-list（对齐 defect-custom-field-columns 单测） */
function mockLocalCache(entries) {
  return {
    getJSON() {
      return entries
    },
    get() {
      return null
    },
    setJSON() {}
  }
}

describe('resolveOrderedDefectFormFields', () => {
  const alwaysVisible = () => true
  const layout = {
    enabledBuiltinFieldKeys: [
      'defectName',
      'defectType',
      'defectLevel',
      'handleBy',
      'moduleVersion',
      'moduleId',
      'planStartTime',
      'planEndTime',
      'caseId',
      'defectDescribe',
      'imgUrls',
      'annexUrls'
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

  it('orders custom fields between builtins per team order when no personal cache', () => {
    const fields = resolveOrderedDefectFormFields(null, layout, {
      isBuiltinVisible: alwaysVisible
    })
    expect(fields.map(f => (f.kind === 'custom' ? `custom:${f.fieldKey}` : f.formKey))).toEqual([
      'defectName',
      'custom:custom_priority',
      'defectType',
      'handleBy',
      'moduleId',
      'defectLevel',
      'moduleVersion',
      'planTime',
      'caseId',
      'defectDescribe',
      'imgUrls',
      'annexUrls'
    ])
  })

  it('prefers personal display-field key order over team orderedEnabledFieldKeys', () => {
    const localCache = mockLocalCache([
      { key: 'type', visible: true },
      { key: 'custom:custom_priority', visible: true },
      { key: 'defect.name', visible: true },
      { key: 'handle-by', visible: true },
      { key: 'module', visible: true }
    ])
    const fields = resolveOrderedDefectFormFields(localCache, layout, {
      isBuiltinVisible: alwaysVisible
    })
    const keys = fields.map(f => (f.kind === 'custom' ? `custom:${f.fieldKey}` : f.formKey))
    expect(keys.slice(0, 5)).toEqual([
      'defectType',
      'custom:custom_priority',
      'defectName',
      'handleBy',
      'moduleId'
    ])
  })

  it('keeps team-enabled fields on form even when personal cache marks them invisible', () => {
    const localCache = mockLocalCache([
      { key: 'defect.name', visible: true },
      { key: 'type', visible: false },
      { key: 'handle-by', visible: true }
    ])
    const fields = resolveOrderedDefectFormFields(localCache, layout, {
      isBuiltinVisible: alwaysVisible
    })
    expect(fields.some(f => f.formKey === 'defectType')).toBe(true)
    expect(fields.findIndex(f => f.formKey === 'defectName')).toBeLessThan(
      fields.findIndex(f => f.formKey === 'defectType')
    )
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
