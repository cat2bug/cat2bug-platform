import { isExternal } from '@/utils/validate'
import {
  buildNativeImageOnerrorScript,
  resolveProjectIconUrl,
  resolveUploadUrl
} from '@/utils/upload-asset'

/**
 * Resolve $image{...} field value to a browser-loadable URL.
 * Project icons may be preset indexes or upload-relative paths.
 *
 * @param {unknown} raw
 * @param {string} fieldKey last path segment, e.g. projectIcon
 * @returns {string}
 */
export function resolveMarkdownImageSrc(raw, fieldKey = '') {
  if (raw == null || raw === '') {
    return ''
  }
  const key = String(fieldKey || '')
  if (key === 'projectIcon' || key.endsWith('projectIcon')) {
    return resolveProjectIconUrl(raw)
  }
  const text = String(raw).trim()
  if (!text) {
    return ''
  }
  if (isExternal(text) || text.startsWith('data:')) {
    return text
  }
  return resolveUploadUrl(text)
}

/**
 * @param {import('markdown-it')} md
 * @param {{ name?: string, value?: object }} options
 */
export function ImagePlugin(md, options) {
  const opt = options || {}
  const values = opt.value || {}

  function image(state, silent) {
    if (state.src.charCodeAt(state.pos) !== 0x24 /* $ */) {
      return false
    }
    // Allow style attrs such as ["style":"width:100px;height:100px;"]
    const rg = /^\$(img|image)((?:\[[^\]]*\])*)?\{(.*?)\}/
    const match = state.src.substr(state.pos).match(rg)
    if (!match) {
      return false
    }
    let result = match[3]
    const attrChunk = match[2] || ''
    if (!result || result.indexOf(opt.name) !== 0) {
      return false
    }

    if (!silent) {
      let content = opt.value
      let fieldKey = ''
      if (result.length > opt.name.length) {
        result = result.substring(opt.name.length + 1)
        const fields = result.split('.')
        for (let i = 0; i < fields.length; i += 1) {
          if (!fields[i]) {
            return false
          }
          fieldKey = fields[i]
          content = content?.[fields[i]]
          if (content == null || content === '') {
            return false
          }
        }
      }

      const src = resolveMarkdownImageSrc(content, fieldKey)
      if (!src) {
        return false
      }

      const vToken = state.push('image', 'img', 0)
      const attrs = [
        ['src', src],
        ['alt', fieldKey || ''],
        ['onerror', buildNativeImageOnerrorScript()]
      ]

      // Parse ["style":"..."] or ["style":"...","key":"..."] chunks
      const attrMatches = attrChunk.match(/\[([^\]]*)\]/g) || []
      attrMatches.forEach((bracket) => {
        const inner = bracket.slice(1, -1).trim()
        if (!inner) {
          return
        }
        try {
          const vs = JSON.parse(`{${inner}}`)
          Object.keys(vs).forEach((k) => {
            const key = k.trim()
            const value = vs[k]
            if (key && value != null) {
              attrs.push([key, String(value).trim()])
            }
          })
        } catch (_e) {
          // ignore malformed attr JSON
        }
      })

      vToken.attrs = attrs
      vToken.content = ''
      vToken.children = []
    }
    state.pos += match[0].length
    return true
  }

  md.inline.ruler.before('text', 'cat2bug_image_plugin', image)
}
