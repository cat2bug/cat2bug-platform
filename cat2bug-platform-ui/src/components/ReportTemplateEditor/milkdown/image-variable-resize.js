import {
  applyImageTransformToStyle,
  buildDynamicImageRaw,
  parseInlineStyle,
  parseRotateDeg,
  resetImageTransformStyle
} from './preview-image-style'

const MIN_SIZE = 40
const MAX_SIZE = 960

/** @typedef {'nw'|'n'|'ne'|'e'|'se'|'s'|'sw'|'w'} ResizeHandleName */

export const RESIZE_HANDLES = /** @type {const} */ (['nw', 'n', 'ne', 'e', 'se', 's', 'sw', 'w'])

const HANDLE_CURSOR = {
  nw: 'nwse-resize',
  n: 'ns-resize',
  ne: 'nesw-resize',
  e: 'ew-resize',
  se: 'nwse-resize',
  s: 'ns-resize',
  sw: 'nesw-resize',
  w: 'ew-resize',
  rotate: 'grab'
}

/**
 * @param {string} value
 * @returns {number|null}
 */
export function parseCssPx(value) {
  if (value == null || value === '') {
    return null
  }
  const match = String(value).trim().match(/^(-?\d+(?:\.\d+)?)px$/i)
  if (!match) {
    return null
  }
  const n = Number(match[1])
  return Number.isFinite(n) ? n : null
}

/**
 * @param {string} styleText
 * @param {{ width: number, height: number }} fallback
 * @returns {{ width: number, height: number, rotate: number }}
 */
export function resolveImageBoxSize(styleText, fallback) {
  const style = parseInlineStyle(styleText)
  const width = parseCssPx(style.width) || fallback.width || MIN_SIZE
  const height = parseCssPx(style.height) || fallback.height || MIN_SIZE
  return {
    width: Math.max(MIN_SIZE, width),
    height: Math.max(MIN_SIZE, height),
    rotate: parseRotateDeg(styleText)
  }
}

/**
 * Axis-aligned bounding box for a w×h rectangle rotated by `rotateDeg`.
 * Used so CSS rotate does not paint outside the layout slot.
 *
 * @param {number} width
 * @param {number} height
 * @param {number} rotateDeg
 * @returns {{ width: number, height: number }}
 */
export function rotatedBoundingBox(width, height, rotateDeg) {
  const w = Math.max(0, Number(width) || 0)
  const h = Math.max(0, Number(height) || 0)
  const rad = ((Number(rotateDeg) || 0) * Math.PI) / 180
  const cos = Math.abs(Math.cos(rad))
  const sin = Math.abs(Math.sin(rad))
  return {
    width: Math.max(1, Math.round(w * cos + h * sin)),
    height: Math.max(1, Math.round(w * sin + h * cos))
  }
}

/**
 * Size wrap to the rotated AABB; stage holds the unrotated box + transform.
 *
 * @param {HTMLElement} wrap
 * @param {HTMLElement} stage
 * @param {HTMLElement | null} img
 * @param {number} width
 * @param {number} height
 * @param {number} rotate
 */
export function applyImageLayoutDom(wrap, stage, img, width, height, rotate) {
  const w = Math.max(1, Math.round(Number(width) || 0))
  const h = Math.max(1, Math.round(Number(height) || 0))
  const deg = Number(rotate) || 0
  const aabb = rotatedBoundingBox(w, h, deg)
  wrap.style.width = `${aabb.width}px`
  wrap.style.height = `${aabb.height}px`
  stage.style.width = `${w}px`
  stage.style.height = `${h}px`
  stage.style.transform = deg ? `rotate(${deg}deg)` : ''
  if (img) {
    img.style.width = `${w}px`
    img.style.height = `${h}px`
    img.style.maxWidth = 'none'
    img.style.transform = 'none'
  }
}

/**
 * Keep aspect ratio while scaling from a SE corner drag.
 * @param {number} startWidth
 * @param {number} startHeight
 * @param {number} deltaX
 * @param {number} [minSize]
 * @param {number} [maxSize]
 * @returns {{ width: number, height: number }}
 */
export function scaleBoxFromDelta(startWidth, startHeight, deltaX, minSize = MIN_SIZE, maxSize = MAX_SIZE) {
  const ratio = startWidth > 0 ? startHeight / startWidth : 1
  let width = startWidth + deltaX
  width = Math.max(minSize, Math.min(maxSize, width))
  let height = width * ratio
  if (height > maxSize) {
    height = maxSize
    width = height / (ratio || 1)
  }
  if (height < minSize) {
    height = minSize
    width = height / (ratio || 1)
  }
  return {
    width: Math.round(width),
    height: Math.round(height)
  }
}

/**
 * Map screen-space delta into the image's local (unrotated) axes.
 * @param {number} deltaX
 * @param {number} deltaY
 * @param {number} rotateDeg
 * @returns {{ x: number, y: number }}
 */
export function toLocalDelta(deltaX, deltaY, rotateDeg) {
  const rad = (-(Number(rotateDeg) || 0) * Math.PI) / 180
  const cos = Math.cos(rad)
  const sin = Math.sin(rad)
  return {
    x: deltaX * cos - deltaY * sin,
    y: deltaX * sin + deltaY * cos
  }
}

/**
 * Free (non-proportional) resize from a named handle.
 * Hold Shift (keepAspect) to lock the starting aspect ratio.
 *
 * @param {ResizeHandleName} handle
 * @param {number} startWidth
 * @param {number} startHeight
 * @param {number} localDx
 * @param {number} localDy
 * @param {{ keepAspect?: boolean, minSize?: number, maxSize?: number }} [options]
 * @returns {{ width: number, height: number }}
 */
export function resizeBoxFromHandle(handle, startWidth, startHeight, localDx, localDy, options = {}) {
  const minSize = options.minSize ?? MIN_SIZE
  const maxSize = options.maxSize ?? MAX_SIZE
  let width = startWidth
  let height = startHeight

  if (handle.includes('e')) {
    width = startWidth + localDx
  }
  if (handle.includes('w')) {
    width = startWidth - localDx
  }
  if (handle.includes('s')) {
    height = startHeight + localDy
  }
  if (handle.includes('n')) {
    height = startHeight - localDy
  }

  if (options.keepAspect && startWidth > 0 && startHeight > 0) {
    const ratio = startHeight / startWidth
    const isCorner = handle.length === 2
    if (isCorner) {
      const next = scaleBoxFromDelta(startWidth, startHeight, width - startWidth, minSize, maxSize)
      return next
    }
    if (handle === 'e' || handle === 'w') {
      width = Math.max(minSize, Math.min(maxSize, width))
      height = width * ratio
    } else {
      height = Math.max(minSize, Math.min(maxSize, height))
      width = height / ratio
    }
  }

  width = Math.max(minSize, Math.min(maxSize, width))
  height = Math.max(minSize, Math.min(maxSize, height))
  return {
    width: Math.round(width),
    height: Math.round(height)
  }
}

/**
 * @param {number} deg
 * @returns {number}
 */
export function normalizeRotateDeg(deg) {
  let next = Number(deg) || 0
  next %= 360
  if (next > 180) {
    next -= 360
  }
  if (next <= -180) {
    next += 360
  }
  return Math.round(next * 10) / 10
}

/**
 * @param {number} centerX
 * @param {number} centerY
 * @param {number} clientX
 * @param {number} clientY
 * @returns {number}
 */
export function pointerAngleDeg(centerX, centerY, clientX, clientY) {
  return (Math.atan2(clientY - centerY, clientX - centerX) * 180) / Math.PI
}

/**
 * Attach 8 resize handles + 1 rotate handle; updates style/raw on the node.
 *
 * @param {HTMLElement} host
 * @param {{
 *   getAttrs: () => { style?: string, path?: string, raw?: string },
 *   getView: () => import('prosemirror-view').EditorView|null,
 *   getPos: () => number|boolean|undefined,
 *   onLiveResize?: (width: number, height: number, rotate: number) => void,
 *   onCommit?: (payload: { style: string, width: number, height: number, rotate: number }) => void,
 *   onReset?: (payload: { style: string }) => void
 * }} options
 * @returns {{ root: HTMLElement, destroy: () => void }}
 */
export function mountImageResizeHandle(host, options) {
  const root = document.createElement('span')
  root.className = 'cat2bug-dsl-preview-image__controls'
  root.contentEditable = 'false'
  root.setAttribute('aria-hidden', 'false')

  /** @type {HTMLElement[]} */
  const handles = []

  RESIZE_HANDLES.forEach((name) => {
    const el = document.createElement('span')
    el.className = `cat2bug-dsl-preview-image__resize cat2bug-dsl-preview-image__resize--${name}`
    el.dataset.handle = name
    el.setAttribute('aria-label', `缩放图片（${name}）`)
    el.title = '拖动调整尺寸（按住 Shift 等比）'
    el.contentEditable = 'false'
    el.style.cursor = HANDLE_CURSOR[name]
    handles.push(el)
    root.appendChild(el)
  })

  const rotateHandle = document.createElement('span')
  rotateHandle.className = 'cat2bug-dsl-preview-image__resize cat2bug-dsl-preview-image__resize--rotate'
  rotateHandle.dataset.handle = 'rotate'
  rotateHandle.setAttribute('aria-label', '旋转图片')
  rotateHandle.title = '拖动旋转'
  rotateHandle.contentEditable = 'false'
  rotateHandle.style.cursor = HANDLE_CURSOR.rotate
  handles.push(rotateHandle)
  root.appendChild(rotateHandle)

  const resetBtn = document.createElement('button')
  resetBtn.type = 'button'
  resetBtn.className = 'cat2bug-dsl-preview-image__reset'
  resetBtn.dataset.handle = 'reset'
  resetBtn.setAttribute('aria-label', '重置图片尺寸与旋转')
  resetBtn.title = '重置尺寸与旋转'
  resetBtn.contentEditable = 'false'
  resetBtn.textContent = '↺'
  root.appendChild(resetBtn)

  /**
   * @type {{
   *   mode: 'resize'|'rotate',
   *   handle: string,
   *   startX: number,
   *   startY: number,
   *   startWidth: number,
   *   startHeight: number,
   *   startRotate: number,
   *   startPointerAngle: number,
   *   centerX: number,
   *   centerY: number,
   *   pointerId: number,
   *   keepAspect: boolean
   * } | null}
   */
  let drag = null

  function getImg() {
    const img = host.querySelector('.cat2bug-dsl-preview-image')
    return img instanceof HTMLImageElement || img instanceof HTMLElement ? img : null
  }

  function ensureStage() {
    let stage = host.querySelector('.cat2bug-dsl-preview-image-stage')
    if (stage instanceof HTMLElement) {
      return stage
    }
    stage = document.createElement('div')
    stage.className = 'cat2bug-dsl-preview-image-stage'
    const img = getImg()
    if (img) {
      host.insertBefore(stage, img)
      stage.appendChild(img)
    } else {
      host.appendChild(stage)
    }
    return stage
  }

  const stage = ensureStage()

  /**
   * @param {number} width
   * @param {number} height
   * @param {number} rotate
   */
  function applyLiveTransform(width, height, rotate) {
    options.onLiveResize?.(width, height, rotate)
    applyImageLayoutDom(host, stage, getImg(), width, height, rotate)
  }

  function clearLayoutToNatural() {
    const img = getImg()
    host.style.width = ''
    host.style.height = ''
    stage.style.width = ''
    stage.style.height = ''
    stage.style.transform = ''
    if (img) {
      img.style.width = ''
      img.style.height = ''
      img.style.maxWidth = ''
      img.style.transform = ''
      img.style.removeProperty('object-fit')
      img.style.removeProperty('object-position')
    }
  }

  /**
   * @param {number} width
   * @param {number} height
   * @param {number} rotate
   */
  function commitTransform(width, height, rotate) {
    const attrs = options.getAttrs?.() || {}
    const style = applyImageTransformToStyle(attrs.style || '', width, height, rotate)
    if (typeof options.onCommit === 'function') {
      options.onCommit({ style, width, height, rotate })
      return
    }
    const view = options.getView?.()
    const pos = options.getPos?.()
    if (!view || typeof pos !== 'number') {
      return
    }
    const path = attrs.path || ''
    const raw = buildDynamicImageRaw(path, style)
    const node = view.state.doc.nodeAt(pos)
    if (!node) {
      return
    }
    const tr = view.state.tr.setNodeMarkup(pos, undefined, {
      ...node.attrs,
      style,
      path,
      raw
    })
    view.dispatch(tr)
  }

  function commitReset() {
    const attrs = options.getAttrs?.() || {}
    const style = resetImageTransformStyle(attrs.style || '')
    clearLayoutToNatural()
    if (typeof options.onReset === 'function') {
      options.onReset({ style })
      return
    }
    if (typeof options.onCommit === 'function') {
      options.onCommit({ style, width: 0, height: 0, rotate: 0 })
      return
    }
    const view = options.getView?.()
    const pos = options.getPos?.()
    if (!view || typeof pos !== 'number') {
      return
    }
    const path = attrs.path || ''
    const raw = buildDynamicImageRaw(path, style)
    const node = view.state.doc.nodeAt(pos)
    if (!node) {
      return
    }
    const tr = view.state.tr.setNodeMarkup(pos, undefined, {
      ...node.attrs,
      style,
      path,
      raw
    })
    view.dispatch(tr)
  }

  /**
   * @param {PointerEvent} event
   */
  function onResetPointerDown(event) {
    if (event.button != null && event.button !== 0) {
      return
    }
    event.preventDefault()
    event.stopPropagation()
  }

  /**
   * @param {MouseEvent} event
   */
  function onResetClick(event) {
    event.preventDefault()
    event.stopPropagation()
    commitReset()
  }

  function onPointerMove(event) {
    if (!drag || event.pointerId !== drag.pointerId) {
      return
    }
    event.preventDefault()
    if (drag.mode === 'rotate') {
      const angle = pointerAngleDeg(drag.centerX, drag.centerY, event.clientX, event.clientY)
      let rotate = normalizeRotateDeg(drag.startRotate + (angle - drag.startPointerAngle))
      if (event.shiftKey) {
        rotate = Math.round(rotate / 15) * 15
      }
      applyLiveTransform(drag.startWidth, drag.startHeight, rotate)
      return
    }
    const screenDx = event.clientX - drag.startX
    const screenDy = event.clientY - drag.startY
    const local = toLocalDelta(screenDx, screenDy, drag.startRotate)
    const next = resizeBoxFromHandle(
      /** @type {ResizeHandleName} */ (drag.handle),
      drag.startWidth,
      drag.startHeight,
      local.x,
      local.y,
      { keepAspect: drag.keepAspect || event.shiftKey }
    )
    applyLiveTransform(next.width, next.height, drag.startRotate)
  }

  function onPointerUp(event) {
    if (!drag || event.pointerId !== drag.pointerId) {
      return
    }
    const current = drag
    drag = null
    window.removeEventListener('pointermove', onPointerMove)
    window.removeEventListener('pointerup', onPointerUp)
    window.removeEventListener('pointercancel', onPointerUp)
    document.body.classList.remove('is-image-transform-dragging')

    if (current.mode === 'rotate') {
      const angle = pointerAngleDeg(current.centerX, current.centerY, event.clientX, event.clientY)
      let rotate = normalizeRotateDeg(current.startRotate + (angle - current.startPointerAngle))
      if (event.shiftKey) {
        rotate = Math.round(rotate / 15) * 15
      }
      commitTransform(current.startWidth, current.startHeight, rotate)
      return
    }
    const screenDx = event.clientX - current.startX
    const screenDy = event.clientY - current.startY
    const local = toLocalDelta(screenDx, screenDy, current.startRotate)
    const next = resizeBoxFromHandle(
      /** @type {ResizeHandleName} */ (current.handle),
      current.startWidth,
      current.startHeight,
      local.x,
      local.y,
      { keepAspect: current.keepAspect || event.shiftKey }
    )
    commitTransform(next.width, next.height, current.startRotate)
  }

  /**
   * @param {PointerEvent} event
   */
  function onPointerDown(event) {
    if (event.button != null && event.button !== 0) {
      return
    }
    const target = event.target
    if (!(target instanceof HTMLElement)) {
      return
    }
    const handleEl = target.closest('[data-handle]')
    if (!(handleEl instanceof HTMLElement) || !root.contains(handleEl)) {
      return
    }
    const handleName = handleEl.dataset.handle || ''
    if (handleName === 'reset') {
      event.preventDefault()
      event.stopPropagation()
      return
    }
    event.preventDefault()
    event.stopPropagation()

    const attrs = options.getAttrs?.() || {}
    const img = getImg()
    const fallbackW = parseCssPx(img?.style.width || '') || stage.offsetWidth || MIN_SIZE
    const fallbackH = parseCssPx(img?.style.height || '') || stage.offsetHeight || MIN_SIZE
    const box = resolveImageBoxSize(attrs.style || '', {
      width: fallbackW,
      height: fallbackH
    })
    const stageRect = stage.getBoundingClientRect()
    const centerX = stageRect.left + stageRect.width / 2
    const centerY = stageRect.top + stageRect.height / 2

    drag = {
      mode: handleName === 'rotate' ? 'rotate' : 'resize',
      handle: handleName,
      startX: event.clientX,
      startY: event.clientY,
      startWidth: box.width,
      startHeight: box.height,
      startRotate: box.rotate,
      startPointerAngle: pointerAngleDeg(centerX, centerY, event.clientX, event.clientY),
      centerX,
      centerY,
      pointerId: event.pointerId,
      keepAspect: event.shiftKey
    }
    document.body.classList.add('is-image-transform-dragging')
    window.addEventListener('pointermove', onPointerMove)
    window.addEventListener('pointerup', onPointerUp)
    window.addEventListener('pointercancel', onPointerUp)
  }

  root.addEventListener('pointerdown', onPointerDown)
  resetBtn.addEventListener('pointerdown', onResetPointerDown)
  resetBtn.addEventListener('click', onResetClick)
  stage.appendChild(root)

  // Sync layout so AABB reserves space and handles sit on the image box.
  const attrs = options.getAttrs?.() || {}
  const img = getImg()
  const styleMap = parseInlineStyle(attrs.style || '')
  const fallbackW = parseCssPx(img?.style.width || '') || stage.offsetWidth || MIN_SIZE
  const fallbackH = parseCssPx(img?.style.height || '') || stage.offsetHeight || MIN_SIZE
  const initial = resolveImageBoxSize(attrs.style || '', {
    width: fallbackW,
    height: fallbackH
  })
  if (styleMap.width || styleMap.height || initial.rotate) {
    applyLiveTransform(initial.width, initial.height, initial.rotate)
  }

  return {
    root,
    /** @deprecated use root */
    handle: root,
    destroy() {
      window.removeEventListener('pointermove', onPointerMove)
      window.removeEventListener('pointerup', onPointerUp)
      window.removeEventListener('pointercancel', onPointerUp)
      document.body.classList.remove('is-image-transform-dragging')
      root.removeEventListener('pointerdown', onPointerDown)
      resetBtn.removeEventListener('pointerdown', onResetPointerDown)
      resetBtn.removeEventListener('click', onResetClick)
      root.remove()
      const latest = options.getAttrs?.() || {}
      const latestStyle = parseInlineStyle(latest.style || '')
      if (!latestStyle.width && !latestStyle.height && !parseRotateDeg(latest.style || '')) {
        clearLayoutToNatural()
        return
      }
      const box = resolveImageBoxSize(latest.style || '', {
        width: initial.width,
        height: initial.height
      })
      applyImageLayoutDom(host, stage, getImg(), box.width, box.height, box.rotate)
    }
  }
}
