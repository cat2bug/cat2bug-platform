/**
 * @jest-environment jsdom
 */
import {
  REPORT_EXPORT_CONTENT_WIDTH,
  REPORT_EXPORT_HOST_ATTR,
  buildReportExportHostCss,
  createReportMarkdownExportHost,
  readMarkdownPreviewHtml,
  sanitizeReportExportHtml
} from '@/components/Report/annotation/report-export-surface'

describe('report-export-surface', () => {
  afterEach(() => {
    document.querySelectorAll(`[${REPORT_EXPORT_HOST_ATTR}]`).forEach((node) => node.remove())
  })

  it('builds light print css that left-aligns images and fills table width', () => {
    const css = buildReportExportHostCss()
    expect(css).toContain('background: #ffffff')
    expect(css).toContain('margin-left: 0')
    expect(css).toContain('table-layout: fixed')
    expect(css).toContain('width: 100%')
    expect(css).not.toContain('#1d1e1f')
  })

  it('creates fixed-width light host from markdown html', () => {
    const sample = `
      <table>
        <thead><tr>
          <th>缺陷发现率 (单位: %)</th>
          <th>缺陷修复率 (单位: %)</th>
          <th>缺陷密度 (单位: 个)</th>
        </tr></thead>
        <tbody><tr><td>0%</td><td>0%</td><td>0个</td></tr></tbody>
      </table>
      <p><img src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7" alt="icon" /></p>
    `
    const pack = createReportMarkdownExportHost({ html: sample })
    expect(pack).toBeTruthy()
    expect(pack.width).toBe(REPORT_EXPORT_CONTENT_WIDTH)
    expect(pack.element.style.width).toBe(`${REPORT_EXPORT_CONTENT_WIDTH}px`)
    expect(pack.element.style.background).toBe('rgb(255, 255, 255)')
    expect(pack.element.querySelector('table')).toBeTruthy()
    expect(pack.element.querySelector('img')).toBeTruthy()
    pack.destroy()
    expect(document.querySelector(`[${REPORT_EXPORT_HOST_ATTR}]`)).toBeNull()
  })

  it('rejects empty html', () => {
    expect(createReportMarkdownExportHost({ html: '   ' })).toBeNull()
  })

  it('reads markdown preview html from scope', () => {
    const root = document.createElement('div')
    root.innerHTML = '<div class="markdown-body"><p>hello</p></div>'
    expect(readMarkdownPreviewHtml(root)).toContain('hello')
  })

  it('strips annotation mark yellow highlights from export html', () => {
    const dirty = `
      <p>before <mark class="report-annotation-highlight-mark is-active">Cat2Bug-Platform</mark> after</p>
      <td class="report-annotation-highlight is-flash">cell</td>
    `
    const clean = sanitizeReportExportHtml(dirty)
    expect(clean).toContain('Cat2Bug-Platform')
    expect(clean).not.toContain('report-annotation-highlight-mark')
    expect(clean).not.toMatch(/<mark[\s>]/i)
    expect(clean).not.toContain('report-annotation-highlight')
  })

  it('export host css neutralizes mark background', () => {
    const css = buildReportExportHostCss()
    expect(css).toContain('mark')
    expect(css).toContain('background: transparent')
  })

  it('table uses full host width after layout', () => {
    const sample = `
      <table style="width:100%">
        <tr>
          <th>缺陷发现率 (单位: %)</th>
          <th>缺陷修复率 (单位: %)</th>
          <th>缺陷探测率 (单位: %)</th>
          <th>缺陷密度 (单位: 个)</th>
          <th>缺陷严重率 (单位: %)</th>
          <th>缺陷重开率 (单位: %)</th>
        </tr>
        <tr>
          <td>0%</td><td>0%</td><td>0%</td><td>0个</td><td>0%</td><td>0%</td>
        </tr>
      </table>
    `
    const pack = createReportMarkdownExportHost({ html: sample, width: REPORT_EXPORT_CONTENT_WIDTH })
    expect(pack.element.style.width).toBe('794px')
    expect(pack.element.querySelectorAll('th').length).toBe(6)
    expect(pack.element.querySelector('th').textContent).toContain('缺陷发现率')
    pack.destroy()
  })
})
