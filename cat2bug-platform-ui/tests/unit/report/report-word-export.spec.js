/**
 * @jest-environment jsdom
 */
import {
  constrainTablesForWordExport,
  disableRepeatingTableHeaders,
  buildWordExportDocument,
  resolveImageExportSize,
  WORD_CONTENT_WIDTH_PX
} from '@/components/Report/annotation/report-word-export'

describe('report-word-export', () => {
  it('constrains tables to 100% fixed layout and clears col widths', () => {
    const root = document.createElement('div')
    root.innerHTML = `
      <table style="width:2000px">
        <colgroup><col width="800" /><col width="800" /><col width="800" /></colgroup>
        <tr>
          <th width="500">缺陷发现率 (单位: %)</th>
          <th width="500">缺陷修复率 (单位: %)</th>
          <th width="500">缺陷密度 (单位: 个)</th>
        </tr>
        <tr><td>0%</td><td>0%</td><td>0个</td></tr>
      </table>
    `
    constrainTablesForWordExport(root)
    const table = root.querySelector('table')
    expect(table.getAttribute('width')).toBe('100%')
    expect(table.style.width).toBe('100%')
    expect(table.style.tableLayout).toBe('fixed')
    expect(root.querySelector('col').getAttribute('width')).toBeNull()
    expect(root.querySelectorAll('th').length).toBe(0)
    expect(root.querySelector('td').style.wordBreak).toBe('break-word')
    expect(root.querySelector('td').style.fontWeight).toBe('600')
  })

  it('moves thead rows into tbody so Word will not repeat headers per page', () => {
    const root = document.createElement('div')
    root.innerHTML = `
      <table>
        <thead>
          <tr><th>项目名称</th><th>图标</th></tr>
          <tr><th colspan="2">测试计划</th></tr>
        </thead>
        <tbody>
          <tr><td>A</td><td>B</td></tr>
          <tr><td>C</td><td>D</td></tr>
        </tbody>
      </table>
    `
    disableRepeatingTableHeaders(root)
    const table = root.querySelector('table')
    expect(table.querySelector('thead')).toBeNull()
    expect(table.querySelectorAll('th').length).toBe(0)
    const rows = table.querySelectorAll('tbody > tr')
    expect(rows.length).toBe(4)
    expect(rows[0].textContent).toContain('项目名称')
    expect(rows[1].textContent).toContain('测试计划')
    expect(rows[2].textContent).toContain('A')
    expect(rows[0].querySelector('td').style.fontWeight).toBe('600')
  })

  it('uses css display size instead of natural pixels for word images', () => {
    const img = document.createElement('img')
    img.setAttribute('style', 'width:120px;height:80px;')
    img.style.width = '120px'
    img.style.height = '80px'
    Object.defineProperty(img, 'naturalWidth', { value: 2000 })
    Object.defineProperty(img, 'naturalHeight', { value: 1500 })
    const size = resolveImageExportSize(img, WORD_CONTENT_WIDTH_PX)
    expect(size.width).toBe(120)
    expect(size.height).toBe(80)
  })

  it('caps only when display width exceeds word page width', () => {
    const img = document.createElement('img')
    img.setAttribute('style', 'width:900px;height:300px;')
    img.style.width = '900px'
    img.style.height = '300px'
    const size = resolveImageExportSize(img, 640)
    expect(size.width).toBe(640)
    expect(size.height).toBe(213)
  })

  it('builds word document with page-sized body wrapper', () => {
    const html = buildWordExportDocument('<p>hi</p><img src="data:image/png;base64,xx" />', {
      title: 'demo'
    })
    expect(html).toContain(`width: ${WORD_CONTENT_WIDTH_PX}px`)
    expect(html).toContain('table-layout: fixed')
    expect(html).toContain('thead')
    expect(html).toContain('table-row-group')
    expect(html).toContain('<p>hi</p>')
    expect(html).toContain('@page')
  })
})
