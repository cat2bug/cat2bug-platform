import { createApp, h } from 'vue'
import Cat2BugLoading from '@/components/Cat2BugLoading/index.vue'
import i18n from '@/utils/i18n/i18n.js'
import { pinia } from '@/store/pinia.js'

const ATTR = 'data-cat2bug-loading'
/** 已调度、尚未真正 mount，避免 MutationObserver 重复排队 */
const ATTR_PENDING = 'pending'
const ATTR_DONE = '1'
/** @type {WeakMap<Element, () => void>} */
const disposers = new WeakMap()
/** @type {WeakMap<Element, number>} */
const pendingTimers = new WeakMap()

function parseRgbChannels(color) {
  if (!color || color === 'transparent') return null
  const m = String(color).match(/rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)/i)
  if (!m) return null
  return [Number(m[1]), Number(m[2]), Number(m[3])]
}

function isDarkMask(mask) {
  if (document.documentElement.classList.contains('dark')) return true
  const bg = getComputedStyle(mask).backgroundColor
  const rgb = parseRgbChannels(bg)
  if (!rgb) return false
  // 遮罩偏黑时用浅色猫（dark variant = 白轮廓）
  const [r, g, b] = rgb
  return (r + g + b) / 3 < 80
}

function readLoadingText(spinner) {
  const textEl = spinner.querySelector('.el-loading-text')
  if (!textEl) return undefined
  const t = (textEl.textContent || '').trim()
  return t || undefined
}

function clearPendingTimer(mask) {
  const id = pendingTimers.get(mask)
  if (id != null) {
    clearTimeout(id)
    pendingTimers.delete(mask)
  }
}

function teardown(mask) {
  clearPendingTimer(mask)
  const dispose = disposers.get(mask)
  if (dispose) {
    try {
      dispose()
    } catch (_e) {
      /* ignore */
    }
    disposers.delete(mask)
  }
  if (mask && mask.removeAttribute) {
    mask.removeAttribute(ATTR)
  }
}

/**
 * 实际替换 spinner 内容。必须在 Vue 当前 flush 之外调用，
 * 否则嵌套 createApp().mount 会触发 ElOverlay「Maximum recursive updates」。
 */
function mountCatLoading(mask) {
  if (!mask || mask.nodeType !== 1) return
  if (!mask.isConnected) {
    mask.removeAttribute(ATTR)
    return
  }
  if (!mask.classList || !mask.classList.contains('el-loading-mask')) return
  if (mask.getAttribute(ATTR) === ATTR_DONE) return

  const spinner = mask.querySelector('.el-loading-spinner')
  if (!spinner) {
    mask.removeAttribute(ATTR)
    return
  }

  mask.setAttribute(ATTR, ATTR_DONE)
  const text = readLoadingText(spinner)
  const variant = isDarkMask(mask) ? 'dark' : 'light'

  spinner.innerHTML = ''
  const host = document.createElement('div')
  host.className = 'cat2bug-el-loading-host'
  spinner.appendChild(host)

  const app = createApp({
    name: 'Cat2BugElLoadingHost',
    render() {
      return h(Cat2BugLoading, {
        text,
        size: 44,
        variant
      })
    }
  })
  app.use(pinia)
  app.use(i18n)
  app.mount(host)

  disposers.set(mask, () => {
    app.unmount()
  })
}

/**
 * 把 Element Plus Loading 遮罩里的默认转圈替换为 Cat2BugLoading。
 * 通过 setTimeout(0) 推迟到宏任务，避开父组件（尤其 ElDrawer/ElOverlay）的同步更新周期。
 */
export function enhanceElLoadingMask(mask) {
  if (!mask || mask.nodeType !== 1) return
  if (!mask.classList || !mask.classList.contains('el-loading-mask')) return
  const state = mask.getAttribute(ATTR)
  if (state === ATTR_DONE || state === ATTR_PENDING) return
  if (!mask.querySelector('.el-loading-spinner')) return

  mask.setAttribute(ATTR, ATTR_PENDING)
  clearPendingTimer(mask)
  const timer = setTimeout(() => {
    pendingTimers.delete(mask)
    mountCatLoading(mask)
  }, 0)
  pendingTimers.set(mask, timer)
}

function scanNode(node) {
  if (!node || node.nodeType !== 1) return
  if (node.classList && node.classList.contains('el-loading-mask')) {
    enhanceElLoadingMask(node)
  }
  if (node.querySelectorAll) {
    node.querySelectorAll('.el-loading-mask').forEach(enhanceElLoadingMask)
  }
}

function onRemoved(node) {
  if (!node || node.nodeType !== 1) return
  if (node.classList && node.classList.contains('el-loading-mask')) {
    teardown(node)
  }
  if (node.querySelectorAll) {
    node.querySelectorAll('.el-loading-mask').forEach(teardown)
  }
}

/**
 * 全局观察 DOM，自动增强所有 v-loading / ElLoading.service 遮罩。
 * @returns {() => void} 停止观察
 */
export function installCat2BugLoadingEnhance() {
  if (typeof document === 'undefined') return () => {}

  document.querySelectorAll('.el-loading-mask').forEach(enhanceElLoadingMask)

  const observer = new MutationObserver((mutations) => {
    for (const m of mutations) {
      m.addedNodes.forEach(scanNode)
      m.removedNodes.forEach(onRemoved)
    }
  })
  observer.observe(document.body, { childList: true, subtree: true })
  return () => {
    observer.disconnect()
    document.querySelectorAll(`[${ATTR}]`).forEach(teardown)
  }
}
