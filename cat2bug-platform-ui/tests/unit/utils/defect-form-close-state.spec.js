import { serializeDefectFormCloseState } from '@/utils/defect-form-close-state'

describe('serializeDefectFormCloseState', () => {
  it('treats identical form and plan range as unchanged', () => {
    const form = {
      defectName: 'bug',
      handleBy: [3, 1, 2],
      customFields: { b: '2', a: '1' }
    }
    const range = ['2026-01-01', '2026-01-31']
    const baseline = serializeDefectFormCloseState(form, range)
    const current = serializeDefectFormCloseState(
      { ...form, handleBy: [1, 2, 3], customFields: { a: '1', b: '2' } },
      [...range]
    )
    expect(current).toBe(baseline)
  })

  it('detects field edits', () => {
    const baseline = serializeDefectFormCloseState({ defectName: 'a' }, [])
    const current = serializeDefectFormCloseState({ defectName: 'b' }, [])
    expect(current).not.toBe(baseline)
  })

  it('detects plan time range edits', () => {
    const form = { defectName: 'a' }
    const baseline = serializeDefectFormCloseState(form, ['2026-01-01'])
    const current = serializeDefectFormCloseState(form, ['2026-01-02'])
    expect(current).not.toBe(baseline)
  })
})
