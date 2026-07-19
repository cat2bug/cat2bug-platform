import {
  parseImgHtml,
  serializeImgHtml
} from '@/components/ReportTemplateEditor/milkdown/static-html-image'

describe('static html image parse/serialize', () => {
  it('parses img with style from materializer output', () => {
    const html =
      '<img src="/profile/avatar/a.png" alt="projectIcon" style="width:200px;height:200px;" />'
    expect(parseImgHtml(html)).toEqual({
      src: '/profile/avatar/a.png',
      alt: 'projectIcon',
      style: 'width:200px;height:200px;'
    })
  })

  it('rejects non-img html atoms', () => {
    expect(parseImgHtml('<br />')).toBeNull()
    expect(parseImgHtml('<span>x</span>')).toBeNull()
  })

  it('round-trips style after resize commit', () => {
    const next = serializeImgHtml({
      src: 'https://example.com/a.png',
      alt: 'icon',
      style: 'width:120px;height:90px;object-fit:cover;object-position:center'
    })
    expect(next).toBe(
      '<img src="https://example.com/a.png" alt="icon" style="width:120px;height:90px;object-fit:cover;object-position:center" />'
    )
    expect(parseImgHtml(next).style).toContain('width:120px')
  })
})
