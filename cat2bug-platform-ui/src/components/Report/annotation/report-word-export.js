/**
 * Prepare markdown/HTML DOM for Word (html-docx-js) export:
 * - inline images as data URIs (Word cannot fetch app URLs)
 * - force tables into page width
 */

/** Usable content width hint for Word portrait (px @ 96dpi ≈ A4 minus margins). */
export const WORD_CONTENT_WIDTH_PX = 640

/**
 * @param {Blob} blob
 * @returns {Promise<string>}
 */
function blobToDataUri(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(reader.error || new Error('read failed'))
    reader.readAsDataURL(blob)
  })
}

/**
 * @param {string} src
 * @returns {Promise<string|null>}
 */
async function fetchSrcAsDataUri(src) {
  if (!src) {
    return null
  }
  if (src.startsWith('data:')) {
    return src
  }
  try {
    const res = await fetch(src, { credentials: 'include', mode: 'cors' })
    if (!res.ok) {
      return null
    }
    const blob = await res.blob()
    return await blobToDataUri(blob)
  } catch (_e) {
    return null
  }
}

/**
 * @param {HTMLImageElement} img
 * @returns {Promise<string|null>}
 */
async function canvasEncodeImage(img) {
  try {
    const canvas = document.createElement('canvas')
    const w = img.naturalWidth || img.width || 0
    const h = img.naturalHeight || img.height || 0
    if (!w || !h) {
      return null
    }
    canvas.width = w
    canvas.height = h
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      return null
    }
    ctx.drawImage(img, 0, 0)
    return canvas.toDataURL('image/png')
  } catch (_e) {
    return null
  }
}

/**
 * @param {string|null|undefined} value
 * @returns {number}
 */
export function parseCssPx(value) {
  if (value == null || value === '') {
    return 0
  }
  const raw = String(value).trim()
  if (!raw || raw === 'auto') {
    return 0
  }
  const px = raw.match(/^(-?[\d.]+)px$/i)
  if (px) {
    return Math.round(Number(px[1])) || 0
  }
  const num = Number(raw)
  return Number.isFinite(num) && num > 0 ? Math.round(num) : 0
}

/**
 * Resolve Word image size from page display / CSS, not intrinsic natural pixels.
 * Priority: inline style → width/height attrs → rendered box → natural (last resort).
 * @param {HTMLImageElement} img
 * @param {number} maxWidth
 * @returns {{ width: number, height: number }}
 */
export function resolveImageExportSize(img, maxWidth) {
  const styleAttr = img.getAttribute('style') || ''
  const styleWidthMatch = styleAttr.match(/(?:^|;)\s*width\s*:\s*([^;]+)/i)
  const styleHeightMatch = styleAttr.match(/(?:^|;)\s*height\s*:\s*([^;]+)/i)
  const styleW = parseCssPx(img.style && img.style.width) || parseCssPx(styleWidthMatch && styleWidthMatch[1])
  const styleH = parseCssPx(img.style && img.style.height) || parseCssPx(styleHeightMatch && styleHeightMatch[1])
  const attrW = parseCssPx(img.getAttribute('width'))
  const attrH = parseCssPx(img.getAttribute('height'))

  let displayW = 0
  let displayH = 0
  if (typeof img.getBoundingClientRect === 'function') {
    const rect = img.getBoundingClientRect()
    displayW = Math.round(rect.width || 0)
    displayH = Math.round(rect.height || 0)
  }
  if (!displayW) {
    displayW = Math.round(img.clientWidth || img.offsetWidth || 0)
  }
  if (!displayH) {
    displayH = Math.round(img.clientHeight || img.offsetHeight || 0)
  }

  const naturalW = img.naturalWidth || 0
  const naturalH = img.naturalHeight || 0

  let width = styleW || attrW || displayW || naturalW || 400
  let height = styleH || attrH || displayH || 0

  if (!height) {
    if (naturalW > 0 && naturalH > 0) {
      height = Math.max(1, Math.round((width * naturalH) / naturalW))
    } else if (displayW > 0 && displayH > 0) {
      height = Math.max(1, Math.round((width * displayH) / displayW))
    } else {
      height = Math.max(1, Math.round(width * 0.75))
    }
  }

  const cap = maxWidth > 0 ? maxWidth : WORD_CONTENT_WIDTH_PX
  if (width > cap) {
    height = Math.max(1, Math.round((height * cap) / width))
    width = cap
  }
  return { width, height }
}

/**
 * Inline every <img> as data URI and keep page display size for Word.
 * @param {ParentNode} root
 * @param {{ maxImageWidth?: number }} [options]
 */
export async function inlineImagesForWordExport(root, options = {}) {
  if (!root || typeof root.querySelectorAll !== 'function') {
    return
  }
  // Cap only to Word page content width — do not shrink below page display size otherwise.
  const maxImageWidth = options.maxImageWidth || WORD_CONTENT_WIDTH_PX
  const images = Array.from(root.querySelectorAll('img'))
  for (const img of images) {
    // Capture display size before src swap (layout may change after data-uri).
    const sizeBefore = resolveImageExportSize(img, maxImageWidth)
    const src = img.getAttribute('src') || img.currentSrc || ''
    let dataUri = src.startsWith('data:') ? src : await fetchSrcAsDataUri(src)
    if (!dataUri) {
      dataUri = await canvasEncodeImage(img)
    }
    if (dataUri) {
      img.setAttribute('src', dataUri)
    }
    img.setAttribute('width', String(sizeBefore.width))
    img.setAttribute('height', String(sizeBefore.height))
    img.style.width = `${sizeBefore.width}px`
    img.style.height = `${sizeBefore.height}px`
    img.style.maxWidth = '100%'
    img.style.display = 'inline-block'
    img.style.margin = '8px 0'
  }
}

/**
 * Word repeats <thead> rows on every page. Move those rows into <tbody>
 * so long tables do not reprint the first block as page headers.
 * Also demote <th> → styled <td> so WPS/Word won't mark rows as title rows.
 * @param {ParentNode} root
 */
export function disableRepeatingTableHeaders(root) {
  if (!root || typeof root.querySelectorAll !== 'function') {
    return
  }
  root.querySelectorAll('table').forEach((table) => {
    const theads = Array.from(table.children).filter((el) => el.tagName === 'THEAD')
    if (theads.length) {
      let tbody = Array.from(table.children).find((el) => el.tagName === 'TBODY')
      if (!tbody) {
        tbody = document.createElement('tbody')
        table.appendChild(tbody)
      }
      theads.forEach((thead) => {
        const rows = Array.from(thead.children).filter((el) => el.tagName === 'TR')
        rows.reverse().forEach((row) => {
          tbody.insertBefore(row, tbody.firstChild)
        })
        thead.remove()
      })
    }

    table.querySelectorAll('th').forEach((th) => {
      const td = document.createElement('td')
      Array.from(th.attributes).forEach((attr) => {
        td.setAttribute(attr.name, attr.value)
      })
      td.style.cssText = th.style.cssText
      td.style.fontWeight = td.style.fontWeight || '600'
      if (!td.style.backgroundColor) {
        td.style.backgroundColor = '#f6f8fa'
      }
      while (th.firstChild) {
        td.appendChild(th.firstChild)
      }
      th.parentNode.replaceChild(td, th)
    })
  })
}

/**
 * Force tables to fit Word page width (drop oversized col widths).
 * @param {ParentNode} root
 */
export function constrainTablesForWordExport(root) {
  if (!root || typeof root.querySelectorAll !== 'function') {
    return
  }
  disableRepeatingTableHeaders(root)
  root.querySelectorAll('table').forEach((table) => {
    table.setAttribute('width', '100%')
    table.style.width = '100%'
    table.style.maxWidth = '100%'
    table.style.tableLayout = 'fixed'
    table.style.borderCollapse = 'collapse'
    table.style.wordBreak = 'break-word'

    table.querySelectorAll('colgroup col, col').forEach((col) => {
      col.removeAttribute('width')
      col.style.width = ''
      col.style.minWidth = '0'
    })

    table.querySelectorAll('th, td').forEach((cell) => {
      cell.removeAttribute('width')
      cell.style.width = ''
      cell.style.minWidth = '0'
      cell.style.maxWidth = '0'
      cell.style.wordBreak = 'break-word'
      cell.style.overflowWrap = 'anywhere'
      cell.style.whiteSpace = 'normal'
      cell.style.verticalAlign = 'top'
    })
  })
}

/**
 * @param {HTMLElement} root
 * @param {{ maxImageWidth?: number }} [options]
 * @returns {Promise<string>}
 */
export async function prepareWordExportHtml(root, options = {}) {
  if (!root) {
    return ''
  }
  await inlineImagesForWordExport(root, options)
  constrainTablesForWordExport(root)
  return root.innerHTML || ''
}

/**
 * Build full HTML document string for html-docx-js.
 * @param {string} bodyHtml
 * @param {{ title?: string }} [meta]
 */
export function buildWordExportDocument(bodyHtml, meta = {}) {
  const title = meta.title || 'report'
  return `<!DOCTYPE html><html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <title>${String(title).replace(/</g, '')}</title>
  <style>
    @page { size: A4; margin: 2cm; }
    body {
      font-family: "Microsoft YaHei", SimSun, sans-serif;
      font-size: 12pt;
      color: #24292e;
      background: #ffffff;
      width: ${WORD_CONTENT_WIDTH_PX}px;
      max-width: 100%;
    }
    table {
      width: 100% !important;
      max-width: 100% !important;
      table-layout: fixed !important;
      border-collapse: collapse !important;
    }
    /* Prevent Word from treating thead as repeating page headers */
    thead {
      display: table-row-group !important;
    }
    th, td {
      border: 1px solid #dfe2e5;
      padding: 4pt 6pt;
      word-break: break-word;
      overflow-wrap: anywhere;
      white-space: normal;
      vertical-align: top;
      max-width: 0;
    }
    th { background: #f6f8fa; font-weight: 600; }
    img {
      max-width: 100% !important;
      height: auto !important;
    }
  </style>
</head>
<body>
  <div style="width:${WORD_CONTENT_WIDTH_PX}px;max-width:100%;">
    ${bodyHtml || ''}
  </div>
</body>
</html>`
}
