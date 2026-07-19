/** @jest-environment jsdom */

import {
  buildPlainTextIndex,
  captureContextAroundSelection,
  rematchAnchor,
  rematchAnnotation,
  getAnnotatableBlocks,
  highlightQuoteInElement,
  clearAnnotationHighlights,
  ANNOTATABLE_SELECTOR
} from '@/components/Report/annotation/report-anchor-rematch'

function renderBody(html) {
  const el = document.createElement('div')
  el.innerHTML = html
  document.body.appendChild(el)
  return el
}

describe('buildPlainTextIndex', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('concatenates text nodes and records their [start, end) segments', () => {
    const root = renderBody('<p>Hello</p><p>World</p>')
    const index = buildPlainTextIndex(root)
    expect(index.text).toBe('HelloWorld')
    expect(index.segments).toHaveLength(2)
    expect(index.segments[0]).toMatchObject({ start: 0, end: 5 })
    expect(index.segments[1]).toMatchObject({ start: 5, end: 10 })
  })

  it('returns an empty index for a null/empty root', () => {
    expect(buildPlainTextIndex(null)).toEqual({ text: '', segments: [] })
    const empty = renderBody('')
    expect(buildPlainTextIndex(empty).text).toBe('')
  })
})

describe('captureContextAroundSelection', () => {
  it('slices up to radius characters before/after the given range', () => {
    const text = '0123456789quoteABCDEFGHIJ'
    const { contextBefore, contextAfter } = captureContextAroundSelection(text, 10, 15, 5)
    expect(contextBefore).toBe('56789')
    expect(contextAfter).toBe('ABCDE')
  })

  it('does not read past the start of the string', () => {
    const { contextBefore } = captureContextAroundSelection('abc', 1, 2, 20)
    expect(contextBefore).toBe('a')
  })
})

describe('rematchAnchor', () => {
  it('matches when the quote appears exactly once', () => {
    const plainText = '开头一段话。这是需要匹配的引用文本。结尾一段话。'
    const result = rematchAnchor({ quote: '这是需要匹配的引用文本。' }, plainText)
    expect(result.status).toBe('matched')
    expect(plainText.slice(result.start, result.end)).toBe('这是需要匹配的引用文本。')
  })

  it('accepts the buildPlainTextIndex result directly as the second argument', () => {
    const root = renderAndDetach('<p>先看这里，</p><p>需要匹配，</p><p>后面还有。</p>')
    const index = buildPlainTextIndex(root)
    const result = rematchAnchor({ quote: '需要匹配，' }, index)
    expect(result.status).toBe('matched')
    expect(index.text.slice(result.start, result.end)).toBe('需要匹配，')
  })

  it('disambiguates duplicate quotes using contextBefore/contextAfter', () => {
    const plainText = 'AAA重复文本BBB###CCC重复文本DDD'
    const result = rematchAnchor(
      {
        quote: '重复文本',
        contextBefore: 'CCC',
        contextAfter: 'DDD'
      },
      plainText
    )
    const expectedStart = plainText.lastIndexOf('重复文本')
    expect(result.status).toBe('matched')
    expect(result.start).toBe(expectedStart)
    expect(result.end).toBe(expectedStart + '重复文本'.length)
  })

  it('picks the first occurrence when context does not disambiguate at all', () => {
    const plainText = 'X重复Y重复Z'
    const result = rematchAnchor({ quote: '重复', contextBefore: '', contextAfter: '' }, plainText)
    expect(result.status).toBe('matched')
    expect(result.start).toBe(plainText.indexOf('重复'))
  })

  it('returns orphaned when the quote can no longer be found', () => {
    const plainText = '正文已经被完全改写，不再包含旧的引用内容。'
    const result = rematchAnchor({ quote: '这段话已经被删除' }, plainText)
    expect(result).toEqual({ status: 'orphaned', start: null, end: null })
  })

  it('returns orphaned for an empty quote or empty plain text instead of guessing a position', () => {
    expect(rematchAnchor({ quote: '' }, '任意正文').status).toBe('orphaned')
    expect(rematchAnchor({ quote: '任意引用' }, '').status).toBe('orphaned')
    expect(rematchAnchor(null, '任意正文').status).toBe('orphaned')
  })
})

describe('table selection rematch', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('matches table cell (td) instead of whole row (tr)', () => {
    expect(ANNOTATABLE_SELECTOR).toContain('td')
    expect(ANNOTATABLE_SELECTOR).not.toContain('tr')
    const root = renderBody(
      '<table><tr><td>左边</td><td>选中的关键字</td><td>右边</td></tr></table>'
    )
    const blocks = getAnnotatableBlocks(root)
    expect(blocks.every(b => b.tagName !== 'TR')).toBe(true)
    expect(blocks.some(b => b.tagName === 'TD')).toBe(true)

    const result = rematchAnnotation(root, {
      quote: '选中的关键字',
      anchorKind: 'selection'
    })
    expect(result.matched).toBe(true)
    expect(result.element.tagName).toBe('TD')
    expect(result.element.textContent).toContain('选中的关键字')
  })

  it('wraps only the selected quote text for selection highlight', () => {
    const root = renderBody(
      '<table><tr><td>前缀选中的关键字后缀</td></tr></table>'
    )
    const td = root.querySelector('td')
    const mark = highlightQuoteInElement(td, { quote: '选中的关键字' }, { active: true })
    expect(mark).toBeTruthy()
    expect(mark.tagName).toBe('MARK')
    expect(mark.textContent).toBe('选中的关键字')
    expect(mark.classList.contains('is-active')).toBe(true)
    expect(td.textContent).toBe('前缀选中的关键字后缀')
    clearAnnotationHighlights(root)
    expect(root.querySelector('mark')).toBeNull()
    expect(td.textContent).toBe('前缀选中的关键字后缀')
  })
})

function renderAndDetach(html) {
  const el = document.createElement('div')
  el.innerHTML = html
  return el
}
