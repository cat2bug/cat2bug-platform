import {
  formatMarkdownLink,
  requestLinkInsertMarkdown,
  setLinkInsertRequester
} from '@/components/ReportTemplateEditor/milkdown/link-insert-bridge'

describe('link-insert-bridge', () => {
  afterEach(() => {
    setLinkInsertRequester(null)
  })

  it('formats markdown links', () => {
    expect(formatMarkdownLink('文档', 'https://example.com')).toBe('[文档](https://example.com)')
    expect(formatMarkdownLink('', 'https://example.com')).toBe('[https://example.com](https://example.com)')
    expect(formatMarkdownLink('x', '')).toBe('[x]()')
  })

  it('resolves markdown from registered requester', async () => {
    setLinkInsertRequester(async () => '[a](b)')
    await expect(requestLinkInsertMarkdown()).resolves.toBe('[a](b)')
  })

  it('returns null when no requester is registered', async () => {
    await expect(requestLinkInsertMarkdown()).resolves.toBeNull()
  })
})
