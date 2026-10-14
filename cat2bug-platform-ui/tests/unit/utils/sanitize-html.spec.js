/**
 * @jest-environment jsdom
 */
import MarkdownIt from 'markdown-it'
import { escapeHtml, filterInlineStyle, sanitizeHtml, textToHtml } from '@/utils/sanitize-html'
import { commentToHtml } from '@/utils/comment-html'

function parse(html) {
  const div = document.createElement('div')
  div.innerHTML = html
  return div
}

describe('sanitizeHtml', () => {
  it('removes script, style, iframe, object and embed elements', () => {
    const out = sanitizeHtml('<p>ok</p><script>alert(1)</script><style>body{}</style>'
      + '<iframe src="https://evil"></iframe><object data="x"></object><embed src="x">')
    expect(out).toBe('<p>ok</p>')
  })

  it('removes event handler attributes', () => {
    const root = parse(sanitizeHtml('<img src="x" onerror="alert(1)"><div onclick="alert(1)" onmouseover="x()">t</div>'))
    root.querySelectorAll('*').forEach((el) => {
      Array.from(el.attributes).forEach((attr) => {
        expect(attr.name.startsWith('on')).toBe(false)
      })
    })
    expect(root.querySelector('img').getAttribute('src')).toBe('x')
  })

  it('removes javascript:, vbscript: and non-image data: links', () => {
    const root = parse(sanitizeHtml(
      '<a href="javascript:alert(1)">a</a><a href="vbscript:msgbox(1)">b</a>'
      + '<a href="data:text/html;base64,PHNjcmlwdD4=">c</a><img src="data:image/png;base64,iVBORw0KGgo=">'
    ))
    root.querySelectorAll('a').forEach((a) => {
      expect(a.getAttribute('href')).toBeNull()
    })
    expect(root.querySelector('img').getAttribute('src')).toBe('data:image/png;base64,iVBORw0KGgo=')
  })

  it('keeps markdown elements, alignment and colors', () => {
    const md = new MarkdownIt({ html: true, linkify: true, breaks: true })
    const source = '# 标题\n\n<p style="text-align:center"><span style="color: #f56c6c; background-color: #fff">红字</span></p>\n\n'
      + '| a | b |\n|---|---|\n| 1 | 2 |\n\n- item\n\n> quote\n\n`code`\n\n![img](/profile/a.png)\n\n[link](https://example.com "t")'
    const rendered = md.render(source)
    const root = parse(sanitizeHtml(rendered))
    expect(root.querySelector('h1').textContent).toBe('标题')
    expect(root.querySelector('p[style]').getAttribute('style')).toBe('text-align: center')
    expect(root.querySelector('span').getAttribute('style')).toBe('color: #f56c6c; background-color: #fff')
    expect(root.querySelector('table td').textContent).toBe('1')
    expect(root.querySelector('ul li').textContent).toBe('item')
    expect(root.querySelector('blockquote')).not.toBeNull()
    expect(root.querySelector('code').textContent).toBe('code')
    expect(root.querySelector('img').getAttribute('src')).toBe('/profile/a.png')
    expect(root.querySelector('a').getAttribute('href')).toBe('https://example.com')
    expect(root.querySelector('a').getAttribute('title')).toBe('t')
  })

  it('drops the javascript: href from markdown links', () => {
    const md = new MarkdownIt({ html: true })
    const root = parse(sanitizeHtml(md.render('[点我](javascript:alert(1)) <a href="javascript:alert(1)">x</a>')))
    root.querySelectorAll('a').forEach((a) => {
      expect(String(a.getAttribute('href') || '')).not.toMatch(/javascript:/i)
    })
  })

  it('adds rel to target=_blank links', () => {
    const root = parse(sanitizeHtml('<a href="https://example.com" target="_blank">x</a>'))
    const a = root.querySelector('a')
    expect(a.getAttribute('target')).toBe('_blank')
    expect(a.getAttribute('rel')).toBe('noopener noreferrer')
  })

  it('keeps report image sizing and rotation but drops unsafe css', () => {
    const root = parse(sanitizeHtml(
      '<img src="/a.png" style="width: 120px; height: 80px; object-fit: cover; transform: rotate(90deg)">'
      + '<div style="position: fixed; top: 0; background: url(https://evil/x.png); color: red">x</div>'
    ))
    expect(root.querySelector('img').getAttribute('style'))
      .toBe('width: 120px; height: 80px; object-fit: cover; transform: rotate(90deg)')
    expect(root.querySelector('div').getAttribute('style')).toBe('color: red')
  })
})

describe('filterInlineStyle', () => {
  it('rejects url(), expression() and non-rotate transforms', () => {
    expect(filterInlineStyle('background-color: url(x)')).toBe('')
    expect(filterInlineStyle('width: expression(alert(1))')).toBe('')
    expect(filterInlineStyle('transform: scale(50)')).toBe('')
    expect(filterInlineStyle('display: inline-flex;justify-content: center;width:100%;'))
      .toBe('display: inline-flex; justify-content: center; width: 100%')
  })
})

describe('escapeHtml / textToHtml', () => {
  it('escapes markup and keeps line breaks', () => {
    expect(escapeHtml('<b>"a" & \'b\'</b>')).toBe('&lt;b&gt;&quot;a&quot; &amp; &#39;b&#39;&lt;/b&gt;')
    expect(textToHtml('a<b>\nc')).toBe('a&lt;b&gt;<br />c')
  })
})

describe('commentToHtml', () => {
  const emoji = new Map([['smile', '/emoji/smile.png']])

  it('shows raw html as text', () => {
    const root = parse(commentToHtml('<script>alert(1)</script>你好', emoji))
    expect(root.querySelector('script')).toBeNull()
    expect(root.textContent).toBe('<script>alert(1)</script>你好')
  })

  it('keeps entities produced by the contenteditable input', () => {
    const root = parse(commentToHtml('&lt;img src=x onerror=alert(1)&gt;&nbsp;a &amp; b', emoji))
    expect(root.querySelector('img')).toBeNull()
    expect(root.textContent).toBe('<img src=x onerror=alert(1)>\u00a0a & b')
  })

  it('blocks injected attributes and keeps emoji and lines', () => {
    const root = parse(commentToHtml('第一行\n<img src=x onerror=alert(1)>[img:smile]<div>第三行</div>', emoji))
    const lines = root.querySelectorAll(':scope > div')
    expect(lines.length).toBe(3)
    expect(lines[0].textContent).toBe('第一行')
    const imgs = root.querySelectorAll('img')
    expect(imgs.length).toBe(1)
    expect(imgs[0].getAttribute('src')).toBe('/emoji/smile.png')
    expect(imgs[0].getAttribute('onerror')).toBeNull()
    expect(lines[2].textContent).toBe('第三行')
  })
})
