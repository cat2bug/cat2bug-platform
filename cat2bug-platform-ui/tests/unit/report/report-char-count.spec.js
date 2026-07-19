import { countReportChars } from '@/components/Report/annotation/report-char-count'

describe('countReportChars', () => {
  it('returns 0 for empty input', () => {
    expect(countReportChars('')).toBe(0)
    expect(countReportChars(null)).toBe(0)
    expect(countReportChars(undefined)).toBe(0)
  })

  it('counts Chinese characters and ignores whitespace', () => {
    expect(countReportChars('饭撒的\n\n标题二')).toBe(6)
  })

  it('strips markdown heading markers before counting', () => {
    expect(countReportChars('# 饭撒的\n## 标题二')).toBe(6)
  })

  it('counts link/image label text only', () => {
    expect(countReportChars('[文档](https://example.com)')).toBe(2)
    expect(countReportChars('![图](a.png)')).toBe(1)
  })
})
