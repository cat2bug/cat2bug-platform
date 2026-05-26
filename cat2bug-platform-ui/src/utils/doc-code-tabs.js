import hljs from 'highlight.js/lib/core'
import { resolveExportAssetHost } from '@/utils/ruoyi'
import java from 'highlight.js/lib/languages/java'
import python from 'highlight.js/lib/languages/python'
import javascript from 'highlight.js/lib/languages/javascript'
import php from 'highlight.js/lib/languages/php'
import csharp from 'highlight.js/lib/languages/csharp'
import bash from 'highlight.js/lib/languages/bash'

hljs.registerLanguage('java', java)
hljs.registerLanguage('python', python)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('php', php)
hljs.registerLanguage('csharp', csharp)
hljs.registerLanguage('bash', bash)

export const CODE_TAB_TITLES = ['cURL', 'Java', 'Python', 'Node.js', 'PHP', 'C#']

const COPY_ICON_SVG = '<svg class="code-tabs__copy-icon" viewBox="0 0 1024 1024" width="16" height="16" aria-hidden="true"><path fill="currentColor" d="M832 64H296c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h516v516c0 4.4 3.6 8 8 8h56c4.4 0 8-3.6 8-8V96c0-17.7-14.3-32-32-32z"/><path fill="currentColor" d="M704 192H192c-17.7 0-32 14.3-32 32v531c0 17.7 14.3 32 32 32h512c17.7 0 32-14.3 32-32V224c0-17.7-14.3-32-32-32zm-40 528H232V272h432v448z"/></svg>'

const FENCE_RE = /```(\w+)\s+title=([^\n`]+)\n([\s\S]*?)```/g
const BLOCK_RE = /::: code-tabs(?:\s+title=([^\n\r]+))?\s*\r?\n([\s\S]*?)\r?\n:::\s*/g

const LANG_ALIAS = {
  bash: 'bash',
  curl: 'bash',
  java: 'java',
  python: 'python',
  javascript: 'javascript',
  js: 'javascript',
  php: 'php',
  csharp: 'csharp',
  cs: 'csharp'
}

/** highlight.js 10.x: highlight(language, code, ignoreIllegals) */
function normalizeMarkdownLang(lang) {
  if (!lang) return null
  const token = String(lang).trim().split(/\s+/)[0].toLowerCase()
  return LANG_ALIAS[token] || token
}

function safeHighlight(code, lang) {
  const hlLang = normalizeMarkdownLang(lang)
  if (!hlLang || !hljs.getLanguage(hlLang)) {
    return escapeHtml(code)
  }
  try {
    return hljs.highlight(hlLang, code, true).value
  } catch (e) {
    return escapeHtml(code)
  }
}

function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function applyPlaceholders(code, baseUrl, apiKey) {
  let out = code
  if (baseUrl) {
    out = out.split('${baseUrl}').join(baseUrl)
  }
  if (apiKey) {
    out = out.split('${apiKey}').join(apiKey)
  }
  return out
}

function highlightWithLineNumbers(code, lang) {
  const hlLang = LANG_ALIAS[lang] || lang
  const lines = code.split('\n')
  const lineHtml = lines.map((line, index) => {
    const inner = line.length === 0 ? '' : safeHighlight(line, hlLang)
    return `<span class="hljs-line" data-line="${index + 1}">${inner || '\n'}</span>`
  })
  return `<code class="hljs language-${hlLang} hljs-line-numbers">${lineHtml.join('')}</code>`
}

function parsePanels(inner) {
  const panels = []
  let m
  FENCE_RE.lastIndex = 0
  while ((m = FENCE_RE.exec(inner)) !== null) {
    panels.push({
      lang: m[1].trim(),
      title: m[2].trim(),
      code: m[3].replace(/\n$/, '')
    })
  }
  return panels
}

/** 从 code-tabs 之前的 ## 小节推断标题（接口名 + 方法 + 路径） */
function inferCodeTabsCaption(markdown, blockStart) {
  const before = markdown.slice(0, blockStart)
  const h2Matches = [...before.matchAll(/^## (.+)$/gm)]
  if (!h2Matches.length) {
    return '示例代码'
  }
  const last = h2Matches[h2Matches.length - 1]
  const heading = last[1].trim()
  const sectionStart = last.index
  const section = markdown.slice(sectionStart, blockStart)
  const method =
    section.match(/\*\*方法\*\*[：:]\s*([A-Za-z]+)/)?.[1] ||
    section.match(/^\*\s*方法[：:]\s*([A-Za-z]+)/m)?.[1]
  const path =
    section.match(/\*\*路径\*\*[：:]\s*`([^`]+)`/)?.[1] ||
    section.match(/^\*\s*接口路径[：:]\s*`([^`]+)`/m)?.[1] ||
    section.match(/^\*\s*路径[：:]\s*`([^`]+)`/m)?.[1]
  if (method && path) {
    return `${heading}（${method.toUpperCase()} ${path}）`
  }
  return heading
}

function buildCodeTabsHtml(panels, baseUrl, apiKey, caption = '') {
  if (!panels.length) {
    return '<div class="code-tabs code-tabs--empty">（示例代码块为空）</div>'
  }
  const captionHtml = caption
    ? `<div class="code-tabs__caption">${escapeHtml(caption)}</div>\n`
    : ''
  const tabButtons = panels.map((p, i) => {
    const active = i === 0 ? ' is-active' : ''
    const safeTitle = escapeHtml(p.title)
    return `<button type="button" class="code-tabs__tab${active}" data-tab-index="${i}">${safeTitle}</button>`
  }).join('')
  const tabPanels = panels.map((p, i) => {
    const active = i === 0 ? ' is-active' : ''
    const code = applyPlaceholders(p.code, baseUrl, apiKey)
    const highlighted = highlightWithLineNumbers(code, p.lang)
    return `<div class="code-tabs__panel${active}" data-tab-index="${i}">
      <pre class="code-tabs__pre">${highlighted}</pre>
    </div>`
  }).join('')
  const copyBtn = `<button type="button" class="code-tabs__copy" title="复制代码" aria-label="复制代码">${COPY_ICON_SVG}</button>`
  return `<div class="code-tabs">\n${captionHtml}<div class="code-tabs__header"><div class="code-tabs__tabs">${tabButtons}</div>${copyBtn}</div>\n<div class="code-tabs__body">${tabPanels}</div>\n</div>`
}

/**
 * 将 ::: code-tabs 容器转为 HTML，再交给 markdown-it 渲染其余内容
 */
export function preprocessCodeTabs(markdown, options = {}) {
  const { baseUrl = '', apiKey = '' } = options
  return markdown.replace(BLOCK_RE, (match, explicitTitle, inner, offset) => {
    const panels = parsePanels(inner)
    const caption = (explicitTitle && explicitTitle.trim()) || inferCodeTabsCaption(markdown, offset)
    return buildCodeTabsHtml(panels, baseUrl, apiKey, caption) + '\n\n'
  })
}

/** 文档页展示用：经前台代理的 Open API 根地址（如 http://192.168.10.178:2222/dev-api） */
export function resolveApiDocBaseUrl() {
  const viaFrontend = resolveExportAssetHost()
  if (viaFrontend) {
    return viaFrontend
  }
  if (typeof window !== 'undefined' && window.location) {
    return window.location.origin.replace(/\/+$/, '')
  }
  return ''
}

/** 直连后端 Open API 根地址（与 application.yml server.port 默认 2020 一致） */
export function resolveApiDocDirectBaseUrl() {
  if (typeof window === 'undefined' || !window.location) {
    return ''
  }
  const { protocol, hostname } = window.location
  return `${protocol}//${hostname}:2020`
}

export function resolveApiDocBaseUrls() {
  return {
    baseUrl: resolveApiDocBaseUrl(),
    baseUrlDirect: resolveApiDocDirectBaseUrl()
  }
}

/**
 * 将 API 文档中的占位符与默认 localhost 示例替换为当前访问域名
 */
export function applyApiDocPlaceholders(markdown, options = {}) {
  const { baseUrl = '', baseUrlDirect = '', apiKey = '' } = options
  let out = markdown
  if (baseUrlDirect) {
    out = out.split('${baseUrlDirect}').join(baseUrlDirect)
    out = out.replace(/https?:\/\/localhost:2020/gi, baseUrlDirect)
  }
  if (baseUrl) {
    out = out.split('${baseUrl}').join(baseUrl)
    const apiPath = (process.env.VUE_APP_BASE_API || '/dev-api').replace(/\/+$/, '')
    const escaped = apiPath.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    out = out.replace(new RegExp(`https?:\\/\\/localhost:2222${escaped}`, 'gi'), baseUrl)
  }
  if (apiKey) {
    out = out.split('${apiKey}').join(apiKey)
  }
  return out
}

export { normalizeMarkdownLang, safeHighlight }

export function initCodeTabs(root) {
  if (!root) return
  root.querySelectorAll('.code-tabs').forEach((tabsEl) => {
    const tabs = tabsEl.querySelectorAll('.code-tabs__tab')
    const panels = tabsEl.querySelectorAll('.code-tabs__panel')
    tabs.forEach((tab) => {
      tab.addEventListener('click', () => {
        const idx = tab.getAttribute('data-tab-index')
        tabs.forEach((t) => t.classList.toggle('is-active', t.getAttribute('data-tab-index') === idx))
        panels.forEach((p) => p.classList.toggle('is-active', p.getAttribute('data-tab-index') === idx))
      })
    })
    const copyBtn = tabsEl.querySelector('.code-tabs__copy')
    if (copyBtn) {
      copyBtn.addEventListener('click', () => {
        const activePanel = tabsEl.querySelector('.code-tabs__panel.is-active')
        const pre = activePanel && activePanel.querySelector('.code-tabs__pre')
        if (!pre) return
        const text = pre.textContent || ''
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(text).catch(() => {})
        }
      })
    }
  })
}
