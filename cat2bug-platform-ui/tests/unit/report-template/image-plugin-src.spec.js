import { resolveMarkdownImageSrc } from '@/components/Cat2BugMarkdown/plugins/ImagePlugin'

jest.mock('@/utils/upload-asset', () => ({
  resolveProjectIconUrl: (raw) => `resolved-icon:${raw}`,
  resolveUploadUrl: (raw) => `resolved-upload:${raw}`,
  buildNativeImageOnerrorScript: () => 'this.onerror=null'
}))

jest.mock('@/utils/validate', () => ({
  isExternal: (url) => /^https?:\/\//.test(String(url))
}))

describe('resolveMarkdownImageSrc', () => {
  it('resolves projectIcon via resolveProjectIconUrl', () => {
    expect(resolveMarkdownImageSrc('3', 'projectIcon')).toBe('resolved-icon:3')
    expect(resolveMarkdownImageSrc('/profile/upload/a.png', 'projectIcon')).toBe(
      'resolved-icon:/profile/upload/a.png'
    )
  })

  it('resolves other upload paths via resolveUploadUrl', () => {
    expect(resolveMarkdownImageSrc('/profile/upload/b.png', 'banner')).toBe(
      'resolved-upload:/profile/upload/b.png'
    )
  })

  it('keeps absolute and data URLs', () => {
    expect(resolveMarkdownImageSrc('https://cdn.example/x.png', 'banner')).toBe(
      'https://cdn.example/x.png'
    )
    expect(resolveMarkdownImageSrc('data:image/png;base64,xx', 'banner')).toBe(
      'data:image/png;base64,xx'
    )
  })

  it('returns empty for missing values', () => {
    expect(resolveMarkdownImageSrc(null, 'projectIcon')).toBe('')
    expect(resolveMarkdownImageSrc('', 'projectIcon')).toBe('')
  })
})
