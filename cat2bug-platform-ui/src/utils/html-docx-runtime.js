/**
 * Load html-docx-js UMD at runtime.
 * Bundlers cannot parse its legacy `with` templates; the browser can.
 */

const SCRIPT_SRC = `${typeof import.meta !== 'undefined' && import.meta.env && import.meta.env.BASE_URL ? import.meta.env.BASE_URL : '/'}vendor/html-docx.js`

/** @type {Promise<{ asBlob: Function }>|null} */
let loading = null

/**
 * @returns {Promise<{ asBlob: (html: string, options?: object) => Blob }>}
 */
export function loadHtmlDocx() {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('html-docx requires browser'))
  }
  if (window.htmlDocx && typeof window.htmlDocx.asBlob === 'function') {
    return Promise.resolve(window.htmlDocx)
  }
  if (loading) {
    return loading
  }
  loading = new Promise((resolve, reject) => {
    const existing = document.querySelector('script[data-html-docx-runtime="1"]')
    if (existing && window.htmlDocx) {
      resolve(window.htmlDocx)
      return
    }
    const script = document.createElement('script')
    script.src = SCRIPT_SRC
    script.async = true
    script.dataset.htmlDocxRuntime = '1'
    script.onload = () => {
      if (window.htmlDocx && typeof window.htmlDocx.asBlob === 'function') {
        resolve(window.htmlDocx)
      } else {
        loading = null
        reject(new Error('html-docx runtime missing asBlob'))
      }
    }
    script.onerror = () => {
      loading = null
      reject(new Error('failed to load html-docx runtime'))
    }
    document.head.appendChild(script)
  })
  return loading
}

/**
 * @param {string} html
 * @param {object} [options]
 * @returns {Promise<Blob>}
 */
export async function htmlDocxAsBlob(html, options) {
  const api = await loadHtmlDocx()
  return api.asBlob(html, options)
}
