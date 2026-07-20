/**
 * Render report-template markdown into a light document card and capture a PNG
 * (same 480×360 footprint as classpath preset previews).
 */

export const TEMPLATE_PREVIEW_WIDTH = 480
export const TEMPLATE_PREVIEW_HEIGHT = 360

function escapeHtml(text) {
  return String(text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

/**
 * Lightweight markdown → HTML for thumbnail only (not a full renderer).
 */
export function markdownToPreviewHtml(markdown) {
  const lines = String(markdown || '').replace(/\r\n/g, '\n').split('\n')
  const parts = []
  let inList = false
  let inQuote = false

  const closeList = () => {
    if (inList) {
      parts.push('</ul>')
      inList = false
    }
  }
  const closeQuote = () => {
    if (inQuote) {
      parts.push('</blockquote>')
      inQuote = false
    }
  }

  for (let i = 0; i < lines.length; i += 1) {
    const line = lines[i]
    const trimmed = line.trim()
    if (!trimmed) {
      closeList()
      closeQuote()
      continue
    }
    if (/^---+$/.test(trimmed)) {
      closeList()
      closeQuote()
      parts.push('<hr/>')
      continue
    }
    const heading = /^(#{1,3})\s+(.+)$/.exec(trimmed)
    if (heading) {
      closeList()
      closeQuote()
      const level = heading[1].length
      parts.push(`<h${level}>${escapeHtml(heading[2])}</h${level}>`)
      continue
    }
    if (/^>\s?/.test(trimmed)) {
      closeList()
      if (!inQuote) {
        parts.push('<blockquote>')
        inQuote = true
      }
      parts.push(`<p>${escapeHtml(trimmed.replace(/^>\s?/, '').replace(/\*\*/g, ''))}</p>`)
      continue
    }
    if (/^[-*+]\s+/.test(trimmed)) {
      closeQuote()
      if (!inList) {
        parts.push('<ul>')
        inList = true
      }
      parts.push(`<li>${escapeHtml(trimmed.replace(/^[-*+]\s+/, ''))}</li>`)
      continue
    }
    if (/^\|/.test(trimmed)) {
      closeList()
      closeQuote()
      // skip table separator rows
      if (/^\|?\s*:?-+:?\s*\|/.test(trimmed)) continue
      const cells = trimmed
        .replace(/^\|/, '')
        .replace(/\|$/, '')
        .split('|')
        .map((c) => escapeHtml(c.trim()))
      parts.push(`<p class="row">${cells.join(' · ')}</p>`)
      continue
    }
    closeList()
    closeQuote()
    parts.push(`<p>${escapeHtml(trimmed.replace(/\*\*/g, ''))}</p>`)
  }
  closeList()
  closeQuote()
  if (!parts.length) {
    parts.push('<p class="empty"> </p>')
  }
  return parts.join('')
}

function buildPreviewHost(html) {
  const host = document.createElement('div')
  host.setAttribute('data-template-preview-capture', '1')
  host.style.cssText = [
    'position:fixed',
    'left:-10000px',
    'top:0',
    `width:${TEMPLATE_PREVIEW_WIDTH}px`,
    `height:${TEMPLATE_PREVIEW_HEIGHT}px`,
    'overflow:hidden',
    'background:#ffffff',
    'color:#303133',
    'box-sizing:border-box',
    'padding:28px 32px',
    'font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"PingFang SC","Microsoft YaHei",sans-serif',
    'font-size:13px',
    'line-height:1.55',
    'text-align:left',
    'z-index:-1',
    'pointer-events:none'
  ].join(';')

  const inner = document.createElement('div')
  inner.innerHTML = html
  inner.style.cssText = 'width:100%;height:100%;overflow:hidden;'
  host.appendChild(inner)

  const style = document.createElement('style')
  style.textContent = `
    [data-template-preview-capture] h1 { font-size: 22px; font-weight: 700; margin: 0 0 14px; color: #1f2329; }
    [data-template-preview-capture] h2 { font-size: 16px; font-weight: 700; margin: 14px 0 8px; color: #1f2329; }
    [data-template-preview-capture] h3 { font-size: 14px; font-weight: 600; margin: 12px 0 6px; color: #303133; }
    [data-template-preview-capture] p { margin: 0 0 8px; color: #606266; }
    [data-template-preview-capture] p.row { font-size: 12px; color: #909399; }
    [data-template-preview-capture] ul { margin: 0 0 10px; padding-left: 18px; color: #606266; }
    [data-template-preview-capture] li { margin: 2px 0; }
    [data-template-preview-capture] blockquote {
      margin: 0 0 12px; padding: 8px 12px; background: #f5f7fa; border-left: 3px solid #dcdfe6; color: #606266; font-size: 12px;
    }
    [data-template-preview-capture] blockquote p { margin: 0; color: inherit; }
    [data-template-preview-capture] hr { border: none; border-top: 1px solid #e4e7ed; margin: 12px 0; }
  `
  host.appendChild(style)
  return host
}

function canvasToPngBlob(canvas) {
  return new Promise((resolve) => {
    if (typeof canvas.toBlob === 'function') {
      canvas.toBlob((blob) => resolve(blob || null), 'image/png')
      return
    }
    try {
      const dataUrl = canvas.toDataURL('image/png')
      const base64 = dataUrl.replace(/^data:image\/png;base64,/, '')
      const binary = atob(base64)
      const bytes = new Uint8Array(binary.length)
      for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i)
      resolve(new Blob([bytes], { type: 'image/png' }))
    } catch (_e) {
      resolve(null)
    }
  })
}

/**
 * @param {string} markdown
 * @returns {Promise<Blob|null>}
 */
export async function captureTemplateContentPreview(markdown) {
  if (typeof document === 'undefined') return null
  const host = buildPreviewHost(markdownToPreviewHtml(markdown))
  document.body.appendChild(host)
  try {
    const html2canvas = (await import('html2canvas')).default
    const canvas = await html2canvas(host, {
      backgroundColor: '#ffffff',
      scale: 1,
      logging: false,
      width: TEMPLATE_PREVIEW_WIDTH,
      height: TEMPLATE_PREVIEW_HEIGHT,
      windowWidth: TEMPLATE_PREVIEW_WIDTH,
      windowHeight: TEMPLATE_PREVIEW_HEIGHT
    })
    return await canvasToPngBlob(canvas)
  } catch (_e) {
    return null
  } finally {
    if (host.parentNode) host.parentNode.removeChild(host)
  }
}
