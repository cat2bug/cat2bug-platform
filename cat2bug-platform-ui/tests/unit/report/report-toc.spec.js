/** @jest-environment jsdom */

import {
  extractAndAssignHeadingToc,
  splitMarkdownHeadingBlocks,
  reorderMarkdownHeadingBlocks
} from '@/components/Report/annotation/report-toc'

function renderBody(html) {
  const el = document.createElement('div')
  el.innerHTML = html
  return el
}

describe('extractAndAssignHeadingToc', () => {
  it('returns [] for a null/undefined root', () => {
    expect(extractAndAssignHeadingToc(null)).toEqual([])
    expect(extractAndAssignHeadingToc(undefined)).toEqual([])
  })

  it('returns [] when the body has no headings', () => {
    const root = renderBody('<p>只有一段普通文字，没有标题。</p>')
    expect(extractAndAssignHeadingToc(root)).toEqual([])
  })

  it('extracts headings in document order with correct levels', () => {
    const root = renderBody(`
      <h1>概述</h1>
      <p>正文</p>
      <h2>缺陷列表</h2>
      <h3>详情</h3>
    `)
    const toc = extractAndAssignHeadingToc(root)
    expect(toc.map((item) => item.text)).toEqual(['概述', '缺陷列表', '详情'])
    expect(toc.map((item) => item.level)).toEqual([1, 2, 3])
  })

  it('assigns a stable id to headings that have none', () => {
    const root = renderBody('<h2>测试计划</h2>')
    const toc = extractAndAssignHeadingToc(root)
    expect(toc).toHaveLength(1)
    expect(toc[0].id).toMatch(/^report-heading-/)
    // The id must be written back onto the actual DOM node for scroll-to-anchor.
    expect(root.querySelector('h2').getAttribute('id')).toBe(toc[0].id)
  })

  it('re-running on the same DOM keeps ids stable (idempotent)', () => {
    const root = renderBody('<h1>概述</h1><h1>概述</h1>')
    const first = extractAndAssignHeadingToc(root)
    const second = extractAndAssignHeadingToc(root)
    expect(second).toEqual(first)
  })

  it('respects an existing heading id instead of overwriting it', () => {
    const root = renderBody('<h2 id="custom-anchor">已存在的锚点</h2>')
    const toc = extractAndAssignHeadingToc(root)
    expect(toc[0].id).toBe('custom-anchor')
  })

  it('disambiguates duplicate heading text with a numeric suffix', () => {
    const root = renderBody('<h2>步骤</h2><h2>步骤</h2>')
    const toc = extractAndAssignHeadingToc(root)
    const ids = toc.map((item) => item.id)
    expect(new Set(ids).size).toBe(2)
    expect(ids[0]).not.toBe(ids[1])
  })
})

describe('reorderMarkdownHeadingBlocks', () => {
  const sample = [
    '前言段落',
    '',
    '# 饭撒的',
    '内容一',
    '',
    '## 标题二',
    '内容二',
    '',
    '## 飞书地方就哭了',
    '内容三'
  ].join('\n')

  it('splits preamble and heading blocks', () => {
    const { preamble, headingBlocks } = splitMarkdownHeadingBlocks(sample)
    expect(preamble.join('\n')).toContain('前言段落')
    expect(headingBlocks.map(b => b.title)).toEqual(['饭撒的', '标题二', '飞书地方就哭了'])
    expect(headingBlocks.map(b => b.level)).toEqual([1, 2, 2])
  })

  it('moves a heading block with its body content', () => {
    const next = reorderMarkdownHeadingBlocks(sample, 2, 1)
    const { headingBlocks } = splitMarkdownHeadingBlocks(next)
    expect(headingBlocks.map(b => b.title)).toEqual(['饭撒的', '飞书地方就哭了', '标题二'])
    expect(headingBlocks[1].lines.join('\n')).toContain('内容三')
    expect(headingBlocks[2].lines.join('\n')).toContain('内容二')
  })

  it('ignores # inside fenced code', () => {
    const md = ['# A', '```', '# not-heading', '```', '# B', 'body'].join('\n')
    const { headingBlocks } = splitMarkdownHeadingBlocks(md)
    expect(headingBlocks.map(b => b.title)).toEqual(['A', 'B'])
    expect(headingBlocks[0].lines.join('\n')).toContain('# not-heading')
  })

  it('returns original when index out of range', () => {
    expect(reorderMarkdownHeadingBlocks(sample, 0, 99)).toBe(sample)
  })
})
