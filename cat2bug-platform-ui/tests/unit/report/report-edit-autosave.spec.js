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

  it('exposes history module and autosave interval', () => {
    expect(REPORT_HISTORY_COMMENT_MODULE).toBe('report_history')
    expect(REPORT_AUTOSAVE_INTERVAL_MS).toBe(60000)
  })
})
