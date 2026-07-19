import {
  isReportContentDirty,
  REPORT_HISTORY_COMMENT_MODULE,
  REPORT_AUTOSAVE_INTERVAL_MS
} from '@/components/Report/annotation/report-edit-autosave'

describe('report-edit-autosave', () => {
  it('detects dirty content', () => {
    expect(isReportContentDirty('a', 'a')).toBe(false)
    expect(isReportContentDirty('a', 'b')).toBe(true)
    expect(isReportContentDirty(null, '')).toBe(false)
    expect(isReportContentDirty('x', null)).toBe(true)
  })

  it('ignores trailing newline-only differences', () => {
    expect(isReportContentDirty('hello\n', 'hello')).toBe(false)
    expect(isReportContentDirty('hello\n\n', 'hello\n')).toBe(false)
  })

  it('exposes history module and 3-minute autosave interval', () => {
    expect(REPORT_HISTORY_COMMENT_MODULE).toBe('report_history')
    expect(REPORT_AUTOSAVE_INTERVAL_MS).toBe(180000)
  })
})
