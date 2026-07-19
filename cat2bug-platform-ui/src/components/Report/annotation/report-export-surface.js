/**
 * Printable export surface for report PDF/Word.
 * Always light theme + fixed content width (A4-ish), built from markdown-it HTML
 * (not Milkdown live DOM — that caused narrow/dark/broken captures).
 */

/** ~210mm at 96dpi; stable printable column. */
export const REPORT_EXPORT_CONTENT_WIDTH = 794

export const REPORT_EXPORT_HOST_ATTR = 'data-report-export-host'

const ANNOTATION_MARK_SELECTOR = 'mark.report-annotation-highlight-mark, mark.report-annotation-highlight'
const ANNOTATION_BLOCK_CLASS = 'report-annotation-highlight'

/**
 * Unwrap annotation <mark> / strip highlight classes so PDF won't show browser-default yellow marks.
 * @param {ParentNode} root
 */
export function stripAnnotationHighlightsFromDom(root) {
  if (!root || typeof root.querySelectorAll !== 'function') {
    return
  }
  root.querySelectorAll(ANNOTATION_MARK_SELECTOR).forEach((mark) => {
    const parent = mark.parentNode
    if (!parent) {
      return
    }
    while (mark.firstChild) {
      parent.insertBefore(mark.firstChild, mark)
    }
    parent.removeChild(mark)
    if (typeof parent.normalize === 'function') {
      parent.normalize()
    }
  })
  root.querySelectorAll('.' + ANNOTATION_BLOCK_CLASS).forEach((el) => {
    el.classList.remove(ANNOTATION_BLOCK_CLASS, 'is-active', 'is-flash')
  })
}

/**
 * @param {string} html
 * @returns {string}
 */
export function sanitizeReportExportHtml(html) {
  const raw = html == null ? '' : String(html)
  if (!raw.trim()) {
    return ''
  }
  const wrap = document.createElement('div')
  wrap.innerHTML = raw
  stripAnnotationHighlightsFromDom(wrap)
  return wrap.innerHTML
}

/**
 * Print stylesheet injected into the off-screen export host.
 * Images left-aligned (override global .markdown-body img centering).
 * Tables fill host width with wrapping similar to editor.
 */
export function buildReportExportHostCss() {
  return `
.${REPORT_EXPORT_HOST_ATTR},
[${REPORT_EXPORT_HOST_ATTR}] {
  box-sizing: border-box;
  background: #ffffff !important;
  color: #24292e !important;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, "PingFang SC",
    "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  font-size: 14px;
  line-height: 1.6;
  padding: 24px 28px;
}

[${REPORT_EXPORT_HOST_ATTR}] * {
  color-scheme: light;
}

/* Neutralize leftover annotation marks (browser default mark is yellow). */
[${REPORT_EXPORT_HOST_ATTR}] mark,
[${REPORT_EXPORT_HOST_ATTR}] .report-annotation-highlight,
[${REPORT_EXPORT_HOST_ATTR}] .report-annotation-highlight-mark {
  background: transparent !important;
  background-color: transparent !important;
  color: inherit !important;
  text-decoration: none !important;
  outline: none !important;
  box-shadow: none !important;
  padding: 0 !important;
}

[${REPORT_EXPORT_HOST_ATTR}] img {
  display: inline-block !important;
  max-width: 100% !important;
  height: auto !important;
  margin: 12px 0 !important;
  margin-left: 0 !important;
  margin-right: 0 !important;
  background: transparent !important;
  vertical-align: middle;
}

[${REPORT_EXPORT_HOST_ATTR}] p:has(> img:only-child) {
  text-align: left !important;
}

[${REPORT_EXPORT_HOST_ATTR}] table {
  display: table !important;
  width: 100% !important;
  max-width: 100% !important;
  table-layout: fixed !important;
  border-collapse: collapse !important;
  overflow: visible !important;
  background: #ffffff !important;
  color: #24292e !important;
}

[${REPORT_EXPORT_HOST_ATTR}] th,
[${REPORT_EXPORT_HOST_ATTR}] td {
  border: 1px solid #dfe2e5 !important;
  padding: 6px 8px !important;
  word-break: break-word !important;
  overflow-wrap: anywhere !important;
  white-space: normal !important;
  min-width: 0 !important;
  vertical-align: top !important;
  text-align: left !important;
  background: #ffffff !important;
  color: #24292e !important;
}

[${REPORT_EXPORT_HOST_ATTR}] th {
  background: #f6f8fa !important;
  font-weight: 600 !important;
}

[${REPORT_EXPORT_HOST_ATTR}] tr:nth-child(even) td {
  background: #fafbfc !important;
}

[${REPORT_EXPORT_HOST_ATTR}] .markdown-it-vue-alter,
[${REPORT_EXPORT_HOST_ATTR}] pre,
[${REPORT_EXPORT_HOST_ATTR}] code {
  background: #f6f8fa !important;
  color: #24292e !important;
}
`.trim()
}

/**
 * @param {object} options
 * @param {string} options.html - rendered markdown HTML (from markdown-it preview)
 * @param {number} [options.width]
 * @returns {{ element: HTMLElement, body?: HTMLElement, width?: number, destroy: () => void } | null}
 */
export function createReportMarkdownExportHost(options = {}) {
  const html = sanitizeReportExportHtml(options.html == null ? '' : String(options.html))
  if (!html.trim()) {
    return null
  }
  const width = Math.max(480, Number(options.width) || REPORT_EXPORT_CONTENT_WIDTH)

  const host = document.createElement('div')
  host.className = 'markdown-body md report-markdown-export-host'
  host.setAttribute(REPORT_EXPORT_HOST_ATTR, '1')
  host.setAttribute('aria-hidden', 'true')

  const style = document.createElement('style')
  style.textContent = buildReportExportHostCss()
  host.appendChild(style)

  const body = document.createElement('div')
  body.className = 'report-markdown-export-body'
  body.innerHTML = html
  host.appendChild(body)

  Object.assign(host.style, {
    position: 'fixed',
    left: '-12000px',
    top: '0',
    width: `${width}px`,
    zIndex: '-1',
    background: '#ffffff',
    color: '#24292e',
    overflow: 'visible',
    pointerEvents: 'none',
    opacity: '1',
    visibility: 'visible'
  })

  document.body.appendChild(host)

  return {
    element: host,
    body,
    width,
    destroy() {
      if (host.parentNode) {
        host.parentNode.removeChild(host)
      }
    }
  }
}

/**
 * Read rendered HTML from the on-page markdown-it preview root.
 * @param {ParentNode|null|undefined} scope
 * @returns {string}
 */
export function readMarkdownPreviewHtml(scope) {
  if (!scope || typeof scope.querySelector !== 'function') {
    return ''
  }
  const body =
    scope.querySelector('.markdown-body') ||
    scope.querySelector('.md') ||
    (scope instanceof HTMLElement ? scope : null)
  if (!body) {
    return ''
  }
  return body.innerHTML || ''
}
