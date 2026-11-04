jest.mock('@/utils/i18n/i18n', () => ({
  __esModule: true,
  default: {
    t: (key) =>
      ({
        day: '天',
        hour: '小时',
        minute: '分钟'
      }[key] || key)
  }
}))

jest.mock('../../../src/plugins/cache', () => ({
  __esModule: true,
  default: { local: { getJSON: () => null, setJSON: () => {}, remove: () => {} } }
}))

import { lifeTime } from '@/utils/defect'

describe('lifeTime', () => {
  it('uses createTime when handleStartTime missing and shows non-zero minutes', () => {
    const create = new Date(Date.now() - 90 * 60 * 1000).toISOString()
    const text = lifeTime({ createTime: create })
    expect(text).not.toBe('0分钟')
    expect(text).toMatch(/小时|分钟/)
  })

  it('freezes when handlePassTime is set', () => {
    const start = '2026-01-01 10:00:00'
    const pass = '2026-01-01 12:30:00'
    expect(lifeTime({ handleStartTime: start, handlePassTime: pass })).toBe('2小时 30分钟')
  })

  it('does not collapse to zero when both API fields missing but createTime exists', () => {
    const create = new Date(Date.now() - 5 * 60 * 1000).toISOString()
    const text = lifeTime({ createTime: create })
    expect(text).toBe('5分钟')
  })

  it('returns zero minutes when no start can be resolved', () => {
    expect(lifeTime({})).toBe('0分钟')
  })
})
