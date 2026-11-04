import { buildSystemDocUrl, docVersionSegment, SYSTEM_DOC_BASE_URL } from '@/utils/system-doc-url'

describe('system-doc-url', () => {
  it('maps semver to major.minor.* segment', () => {
    expect(docVersionSegment('1.1.0')).toBe('1.1.*')
    expect(docVersionSegment('1.2.0')).toBe('1.2.*')
    expect(docVersionSegment('v0.6.1')).toBe('0.6.*')
    expect(docVersionSegment('1.1')).toBe('1.1.*')
  })

  it('returns null for invalid version', () => {
    expect(docVersionSegment('')).toBeNull()
    expect(docVersionSegment(null)).toBeNull()
    expect(docVersionSegment('latest')).toBeNull()
  })

  it('builds versioned document url', () => {
    expect(buildSystemDocUrl('1.1.0')).toBe(
      'https://www.cat2bug.com/document/cat2bug-platform/1.1.*'
    )
    expect(buildSystemDocUrl('')).toBe(SYSTEM_DOC_BASE_URL)
  })
})
