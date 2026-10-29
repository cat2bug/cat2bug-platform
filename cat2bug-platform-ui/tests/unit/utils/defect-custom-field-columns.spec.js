import { mergeDefectTableColumnPrefs } from '@/utils/defect-custom-field-columns'

function mockLocalCache(entries) {
  return {
    getJSON() {
      return entries
    },
    get() {
      return null
    }
  }
}

describe('mergeDefectTableColumnPrefs', () => {
  const defaults = [
    { key: 'type', prop: 'defectType', visible: true, width: 100 },
    { key: 'handle-by', prop: 'handleByList', visible: true, width: 120 },
    { key: 'custom:tag', prop: 'custom_tag', visible: true, width: 80 }
  ]

  it('keeps field-manage (defaults) order and only overlays visible/fixed/width', () => {
    const cached = [
      { key: 'custom:tag', visible: false, fixed: true, width: 200 },
      { key: 'handle-by', visible: true, fixed: false, width: 150 },
      { key: 'type', visible: false, fixed: false, width: 90 }
    ]
    const merged = mergeDefectTableColumnPrefs(mockLocalCache(cached), defaults)
    expect(merged.map(c => c.key)).toEqual(['type', 'handle-by', 'custom:tag'])
    expect(merged[0].visible).toBe(false)
    expect(merged[0].width).toBe(90)
    expect(merged[1].width).toBe(150)
    expect(merged[2].visible).toBe(false)
    expect(merged[2].fixed).toBe(true)
    expect(merged[2].width).toBe(200)
  })

  it('maps legacy string cache to visibility without reordering', () => {
    const merged = mergeDefectTableColumnPrefs(mockLocalCache(['handle-by', 'type']), defaults)
    expect(merged.map(c => c.key)).toEqual(['type', 'handle-by', 'custom:tag'])
    expect(merged[0].visible).toBe(true)
    expect(merged[1].visible).toBe(true)
    expect(merged[2].visible).toBe(true)
  })

  it('returns defaults clone when cache is empty', () => {
    const merged = mergeDefectTableColumnPrefs(mockLocalCache(null), defaults)
    expect(merged.map(c => c.key)).toEqual(['type', 'handle-by', 'custom:tag'])
    expect(merged).not.toBe(defaults)
  })
})
