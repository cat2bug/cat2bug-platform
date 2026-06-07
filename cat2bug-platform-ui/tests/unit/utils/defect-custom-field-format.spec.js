import {
  enumOptionsFromTypeConfig,
  normalizeEnumFieldValue
} from '@/components/DefectCustomField/format'

describe('defect custom field enum format', () => {
  it('parses object options with key/label', () => {
    const opts = enumOptionsFromTypeConfig({
      options: [{ key: 'high', label: '高', color: '#f00' }]
    })
    expect(opts).toEqual([{ key: 'high', label: '高', color: '#f00' }])
  })

  it('falls back to value/label when key missing', () => {
    const opts = enumOptionsFromTypeConfig({
      options: [{ value: 'p1', label: '优先级1' }]
    })
    expect(opts[0].key).toBe('p1')
    expect(opts[0].label).toBe('优先级1')
  })

  it('normalizes enum stored values to string', () => {
    expect(normalizeEnumFieldValue(1)).toBe('1')
    expect(normalizeEnumFieldValue(null)).toBe(null)
  })
})
