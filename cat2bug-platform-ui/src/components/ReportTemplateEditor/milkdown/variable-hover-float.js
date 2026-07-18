/**
 * Float only the hover chrome + dashed frame with position:fixed so table
 * overflow cannot clip them. Body content stays in normal document flow so
 * cell layout / scroll position never jumps.
 */

export const VAR_HOVER_PAD_X = 5
export const VAR_HOVER_PAD_Y = 3
export const VAR_HOVER_CHROME_H = 15

/**
 * @param {HTMLElement} host
 * @param {{ frame: HTMLElement, chrome: HTMLElement, body: HTMLElement }} parts
 */
export function createVariableHoverFloatController(host, { frame, chrome, body }) {
  let active = false
  let raf = 0

  function applyFromRect(rect) {
    const top = rect.top - VAR_HOVER_PAD_Y
    const left = rect.left - VAR_HOVER_PAD_X
    const width = Math.max(rect.width + VAR_HOVER_PAD_X * 2, 12)
    const height = Math.max(rect.height + VAR_HOVER_PAD_Y * 2, 12)

    frame.style.top = `${top}px`
    frame.style.left = `${left}px`
    frame.style.width = `${width}px`
    frame.style.height = `${height}px`

    chrome.style.top = `${top - VAR_HOVER_CHROME_H}px`
    chrome.style.left = `${left}px`
  }

  function sync() {
    if (!active) {
      return
    }
    applyFromRect((body || host).getBoundingClientRect())
  }

  function onScrollOrResize() {
    if (raf) {
      cancelAnimationFrame(raf)
    }
    raf = requestAnimationFrame(() => {
      raf = 0
      sync()
    })
  }

  function activate() {
    if (active) {
      sync()
      return
    }
    active = true
    host.classList.add('is-hovered')
    sync()
    window.addEventListener('scroll', onScrollOrResize, true)
    window.addEventListener('resize', onScrollOrResize)
  }

  function deactivate() {
    if (!active) {
      return
    }
    active = false
    host.classList.remove('is-hovered')
    if (raf) {
      cancelAnimationFrame(raf)
      raf = 0
    }
    window.removeEventListener('scroll', onScrollOrResize, true)
    window.removeEventListener('resize', onScrollOrResize)
    frame.style.top = ''
    frame.style.left = ''
    frame.style.width = ''
    frame.style.height = ''
    chrome.style.top = ''
    chrome.style.left = ''
  }

  return {
    activate,
    deactivate,
    sync,
    destroy() {
      deactivate()
    }
  }
}

/**
 * Build the shared frame + chrome + body skeleton for a variable unit.
 * @param {{ isPlan?: boolean, isBlock?: boolean }} [options]
 */
export function createVariableUnitDomShell(options = {}) {
  const { isPlan = false, isBlock = false } = options
  const tag = isBlock ? 'div' : 'span'
  const bodyTag = isBlock ? 'div' : 'span'

  const dom = document.createElement(tag)
  dom.className = `cat2bug-dsl-var__view${isPlan ? ' cat2bug-dsl-var__view--plan' : ''}${isBlock ? ' cat2bug-dsl-var__view--block' : ''}`
  dom.contentEditable = 'false'

  const frame = document.createElement(tag === 'div' ? 'div' : 'span')
  frame.className = 'cat2bug-dsl-var__frame'
  frame.setAttribute('aria-hidden', 'true')

  const chrome = document.createElement('span')
  chrome.className = 'cat2bug-dsl-var__chrome'

  const toggle = document.createElement('button')
  toggle.type = 'button'
  toggle.className = 'cat2bug-dsl-var__toggle'
  toggle.contentEditable = 'false'

  const title = document.createElement('span')
  title.className = 'cat2bug-dsl-var__title'

  chrome.append(toggle, title)

  const body = document.createElement(bodyTag)
  body.className = isBlock ? 'cat2bug-dsl-var__body cat2bug-dsl-var__body--block' : 'cat2bug-dsl-var__body'

  dom.append(frame, chrome, body)

  return { dom, frame, chrome, toggle, title, body }
}
