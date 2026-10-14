import DOMPurify from 'dompurify'

/** 报告图片（宽高、裁切、旋转）、DSL 插件（居中、卡片表格）与评论行布局依赖的内联样式 */
const ALLOWED_STYLE_PROPS = new Set([
  'color', 'background-color', 'text-align', 'vertical-align',
  'font-weight', 'font-style', 'font-size', 'line-height', 'text-decoration', 'white-space',
  'width', 'height', 'min-width', 'max-width', 'min-height', 'max-height',
  'margin', 'margin-top', 'margin-right', 'margin-bottom', 'margin-left',
  'padding', 'padding-top', 'padding-right', 'padding-bottom', 'padding-left',
  'border', 'border-top', 'border-right', 'border-bottom', 'border-left',
  'border-color', 'border-style', 'border-width', 'border-radius', 'border-collapse',
  'display', 'flex', 'flex-direction', 'flex-wrap', 'justify-content', 'align-items', 'gap',
  'box-sizing', 'object-fit', 'float', 'transform', 'list-style-type'
])

const UNSAFE_STYLE_VALUE = /url\s*\(|expression\s*\(|javascript:|vbscript:|@import|[\\<>]/i
const ROTATE_ONLY_TRANSFORM = /^(\s*rotate\(\s*-?\d+(?:\.\d+)?deg\s*\)\s*)+$/i

const FORBID_TAGS = ['script', 'style', 'iframe', 'frame', 'frameset', 'object', 'embed', 'form', 'base', 'link', 'meta']

let purifier = null

/**
 * 只保留白名单内、值不含外部资源与脚本的 CSS 声明。
 * @param {string} styleText
 * @returns {string}
 */
export function filterInlineStyle(styleText) {
  if (!styleText) {
    return ''
  }
  const kept = []
  String(styleText).split(';').forEach((decl) => {
    const idx = decl.indexOf(':')
    if (idx <= 0) {
      return
    }
    const prop = decl.slice(0, idx).trim().toLowerCase()
    const value = decl.slice(idx + 1).trim()
    if (!value || !ALLOWED_STYLE_PROPS.has(prop) || UNSAFE_STYLE_VALUE.test(value)) {
      return
    }
    if (prop === 'transform' && !ROTATE_ONLY_TRANSFORM.test(value)) {
      return
    }
    kept.push(`${prop}: ${value}`)
  })
  return kept.join('; ')
}

function getPurifier() {
  if (purifier) {
    return purifier
  }
  purifier = DOMPurify(window)
  purifier.addHook('afterSanitizeAttributes', (node) => {
    if (node.hasAttribute('style')) {
      const style = filterInlineStyle(node.getAttribute('style'))
      if (style) {
        node.setAttribute('style', style)
      } else {
        node.removeAttribute('style')
      }
    }
    if (node.tagName === 'A' && node.getAttribute('target') === '_blank') {
      node.setAttribute('rel', 'noopener noreferrer')
    }
  })
  return purifier
}

/**
 * 用户内容经 v-html 插入页面前的统一清洗：去掉脚本类元素、事件属性、危险协议与不安全的内联样式。
 * @param {string} html
 * @returns {string}
 */
export function sanitizeHtml(html) {
  if (html == null || html === '') {
    return ''
  }
  return getPurifier().sanitize(String(html), {
    FORBID_TAGS,
    ADD_ATTR: ['target']
  })
}

/**
 * @param {unknown} text
 * @returns {string}
 */
export function escapeHtml(text) {
  return String(text == null ? '' : text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

/**
 * 纯文本按 HTML 展示：转义后把换行变成 `<br />`。
 * @param {unknown} text
 * @returns {string}
 */
export function textToHtml(text) {
  if (text == null || text === '') {
    return ''
  }
  return escapeHtml(text).replace(/\r?\n/g, '<br />')
}
