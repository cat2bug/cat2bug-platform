/**
 * When both width and height are set, default to cover so non-square bitmaps
 * do not leave dark-theme letterbox bars in the forced box.
 *
 * @param {Record<string, string>} style
 * @returns {Record<string, string>}
 */
export function withPreviewImageFit(style) {
  const next = { ...(style || {}) }
  const hasBox = !!(next.width && next.height)
  const hasFit = !!(next['object-fit'] || next.objectFit)
  if (hasBox && !hasFit) {
    next['object-fit'] = 'cover'
    next['object-position'] = 'center'
  }
  return next
}

/**
 * @param {string} styleText
 * @returns {Record<string, string>}
 */
export function parseInlineStyle(styleText) {
  const style = {}
  if (!styleText) {
    return style
  }
  styleText.split(';').forEach((chunk) => {
    const part = chunk.trim()
    if (!part) {
      return
    }
    const colon = part.indexOf(':')
    if (colon === -1) {
      return
    }
    const key = part.slice(0, colon).trim()
    const value = part.slice(colon + 1).trim()
    if (key) {
      style[key] = value
    }
  })
  return style
}

/**
 * @param {Record<string, string>} style
 * @returns {string}
 */
export function serializeInlineStyle(style) {
  return Object.entries(style || {})
    .filter(([, value]) => value != null && value !== '')
    .map(([key, value]) => `${key}:${value}`)
    .join(';')
}

/**
 * @param {string} styleText
 * @returns {number}
 */
export function parseRotateDeg(styleText) {
  const style = parseInlineStyle(styleText)
  const transform = style.transform || ''
  const match = transform.match(/rotate\(\s*(-?\d+(?:\.\d+)?)\s*deg\s*\)/i)
  if (!match) {
    return 0
  }
  const n = Number(match[1])
  return Number.isFinite(n) ? n : 0
}

/**
 * Merge rotate(...) into transform, preserving other transform functions.
 * @param {string} transform
 * @param {number} rotateDeg
 * @returns {string}
 */
export function withRotateTransform(transform, rotateDeg) {
  const base = String(transform || '').replace(/rotate\(\s*-?\d+(?:\.\d+)?\s*deg\s*\)/gi, '').trim()
  const deg = Number(rotateDeg) || 0
  if (!deg) {
    return base.replace(/\s+/g, ' ').trim()
  }
  const rotate = `rotate(${deg}deg)`
  return base ? `${base} ${rotate}`.replace(/\s+/g, ' ').trim() : rotate
}

/**
 * @param {string} styleText
 * @param {number} widthPx
 * @param {number} heightPx
 * @returns {string}
 */
export function applyBoxSizeToStyle(styleText, widthPx, heightPx) {
  const style = parseInlineStyle(styleText)
  const width = Math.max(1, Math.round(Number(widthPx) || 0))
  const height = Math.max(1, Math.round(Number(heightPx) || 0))
  style.width = `${width}px`
  style.height = `${height}px`
  return serializeInlineStyle(withPreviewImageFit(style))
}

/**
 * @param {string} styleText
 * @param {number} widthPx
 * @param {number} heightPx
 * @param {number} [rotateDeg]
 * @returns {string}
 */
export function applyImageTransformToStyle(styleText, widthPx, heightPx, rotateDeg = 0) {
  const style = parseInlineStyle(applyBoxSizeToStyle(styleText, widthPx, heightPx))
  const transform = withRotateTransform(style.transform || '', rotateDeg)
  if (transform) {
    style.transform = transform
  } else {
    delete style.transform
  }
  return serializeInlineStyle(style)
}

/**
 * Clear size / rotate (and auto-added fit) so the image returns to natural layout.
 * Preserves unrelated custom style keys if present.
 *
 * @param {string} styleText
 * @returns {string}
 */
export function resetImageTransformStyle(styleText) {
  const style = parseInlineStyle(styleText)
  delete style.width
  delete style.height
  delete style.transform
  delete style['object-fit']
  delete style['object-position']
  delete style.objectFit
  delete style.objectPosition
  return serializeInlineStyle(style)
}

/**
 * @param {string} path
 * @param {string} style
 * @returns {string}
 */
export function buildDynamicImageRaw(path, style) {
  const stylePart = style ? `["style":"${style}"]` : ''
  return `$image${stylePart}{${path || ''}}`
}

/**
 * @param {CSSStyleDeclaration} target
 * @param {Record<string, string>} style
 */
export function assignImgStyle(target, style) {
  Object.entries(style || {}).forEach(([key, value]) => {
    if (value == null || value === '') {
      return
    }
    if (key.includes('-')) {
      target.setProperty(key, String(value))
      return
    }
    target[key] = value
  })
}
