import {
  insertKeyAfterTeamPredecessor,
  mergeDefectTableColumnPrefs
} from '@/utils/defect-custom-field-columns'

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

describe('insertKeyAfterTeamPredecessor', () => {
  it('inserts after nearest personal predecessor from team order', () => {
    const keys = ['A', 'C']
    insertKeyAfterTeamPredecessor(keys, 'B', ['A', 'B', 'C'])
    expect(keys).toEqual(['A', 'B', 'C'])
  })

  it('appends when no predecessor exists in personal order', () => {
    const keys = ['C']
    insertKeyAfterTeamPredecessor(keys, 'B', ['A', 'B', 'C'])
    expect(keys).toEqual(['C', 'B'])
  })
})

describe('mergeDefectTableColumnPrefs', () => {
  const defaults = [
    { key: 'type', prop: 'defectType', visible: true, width: 100 },
    { key: 'handle-by', prop: 'handleByList', visible: true, width: 120 },
    { key: 'custom:tag', prop: 'custom_tag', visible: true, width: 80 }
  ]

  it('uses personal cache key order and overlays visible/fixed/width', () => {
    const cached = [
      { key: 'custom:tag', visible: false, fixed: true, width: 200 },
      { key: 'handle-by', visible: true, fixed: false, width: 150 },
      { key: 'type', visible: false, fixed: false, width: 90 }
    ]
    const merged = mergeDefectTableColumnPrefs(mockLocalCache(cached), defaults)
    expect(merged.map(c => c.key)).toEqual(['custom:tag', 'handle-by', 'type'])
    expect(merged[0].visible).toBe(false)
    expect(merged[0].fixed).toBe(true)
    expect(merged[0].width).toBe(200)
    expect(merged[1].width).toBe(150)
    expect(merged[2].visible).toBe(false)
    expect(merged[2].width).toBe(90)
  })

  it('inserts new team fields after personal predecessor', () => {
    const defaultsWithExtra = [
      { key: 'type', prop: 'defectType', visible: true },
      { key: 'name', prop: 'defectName', visible: true },
      { key: 'handle-by', prop: 'handleByList', visible: true }
    ]
    const cached = [
      { key: 'type', visible: true },
      { key: 'handle-by', visible: true }
    ]
    const merged = mergeDefectTableColumnPrefs(mockLocalCache(cached), defaultsWithExtra)
    expect(merged.map(c => c.key)).toEqual(['type', 'name', 'handle-by'])
  })

  it('drops personal keys removed from defaults', () => {
    const cached = [
      { key: 'gone', visible: true },
      { key: 'type', visible: true },
      { key: 'handle-by', visible: false }
    ]
    const merged = mergeDefectTableColumnPrefs(mockLocalCache(cached), defaults)
    expect(merged.map(c => c.key)).toEqual(['type', 'handle-by', 'custom:tag'])
    expect(merged[1].visible).toBe(false)
  })

  it('maps legacy string cache to visibility without personal reorder', () => {
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
