import { createApp, h } from 'vue'
import Cat2BugLoading, { CAT2BUG_LOADING_SIZE } from '@/components/Cat2BugLoading/index.vue'
import i18n from '@/utils/i18n/i18n.js'
import { pinia } from '@/store/pinia.js'

const ATTR = 'data-cat2bug-loading'
/** 已调度、尚未真正 mount，避免 MutationObserver 重复排队 */
const ATTR_PENDING = 'pending'
const ATTR_DONE = '1'
const SPINNER_HIDE_CLASS = 'is-cat2bug-replaced'
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

/**
 * 加载猫高度：列表/表格等主内容区固定 list 标准尺寸，不随遮罩高度变化；
 * 仅统计卡等矮格用 stat。见 docs/04-UI规范/组件与交互约定.md。
 */
function isStatisticMask(mask) {
  return !!(mask && typeof mask.closest === 'function'
    && mask.closest('.statistic-item, .statistic-box, .defect-tools-statistic'))
}

function resolveLoadingSize(mask) {
  if (isStatisticMask(mask)) {
    return CAT2BUG_LOADING_SIZE.stat
  }
  return CAT2BUG_LOADING_SIZE.list
}

/**
 * EP close() 后遮罩仍留在 DOM 淡出约 400ms（v-show + Transition）。
 * 猫挂在 body 上不会跟着淡，内容已透出时还会硬显示；一旦开始 leave 就立刻收掉。
 * 勿用 opacity 判断：enter 淡入时也会很低，会误拆。
 */
function isMaskHiding(mask) {
  if (!mask || !mask.isConnected) return true
  const cls = typeof mask.className === 'string' ? mask.className : String(mask.className || '')
  if (/\bleave\b/i.test(cls)) return true
  const cs = getComputedStyle(mask)
  return cs.display === 'none' || cs.visibility === 'hidden'
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
 * 在 body 上挂浮动层展示 Cat2BugLoading，只隐藏 EP 原转圈，不改 spinner 子树。
 * 旧实现 spinner.innerHTML='' + 嵌套 createApp 会拆掉 Loading 组件的 vnode，
 * 分享弹层等场景开关 loading 时触发 emitsOptions/vnode null，页面卡死。
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
  spinner.classList.add(SPINNER_HIDE_CLASS)

  const text = readLoadingText(spinner)
  const variant = isDarkMask(mask) ? 'dark' : 'light'
  const size = resolveLoadingSize(mask)

  const host = document.createElement('div')
  host.className = 'cat2bug-el-loading-float'
  host.setAttribute('aria-hidden', 'true')
  document.body.appendChild(host)

  const app = createApp({
    name: 'Cat2BugElLoadingHost',
    render() {
      return h(Cat2BugLoading, {
        text,
        size,
        variant
      })
    }
  })
  app.use(pinia)
  app.use(i18n)
  app.mount(host)

  /**
   * 高度未稳定前不显示猫；稳定后再锁定视口纵坐标。
   * 避免空表矮遮罩「居中贴顶」→ 列表区 / max-height 撑开后再跳到中间。
   * 统计卡较矮，阈值用更小值。
   */
  let lockedCatCenterY = null
  let closedEarly = false
  let lastHeight = -1
  let stableFrames = 0
  const MIN_LOCK_HEIGHT = isStatisticMask(mask) ? 48 : 120
  const STABLE_FRAMES = 2
  const syncPos = () => {
    if (closedEarly || !host.isConnected) return
    if (!mask.isConnected || isMaskHiding(mask)) {
      closedEarly = true
      teardown(mask)
      return
    }
    const r = mask.getBoundingClientRect()
    const cs = getComputedStyle(mask)
    const zRaw = cs.zIndex
    const zNum = Number.parseInt(zRaw, 10)
    const h = Math.max(r.height, 0)
    const w = Math.max(r.width, 0)
    if (lastHeight >= 0 && Math.abs(h - lastHeight) <= 2) {
      stableFrames += 1
    } else {
      stableFrames = 0
      lastHeight = h
      lockedCatCenterY = null
      // 高度刚变化时再采两帧，避免只触发一次 RO 时 stableFrames 永远到不了阈值
      requestAnimationFrame(() => {
        requestAnimationFrame(syncPos)
      })
    }
    const ready = h >= MIN_LOCK_HEIGHT && stableFrames >= STABLE_FRAMES
    const liveCenter = r.top + h / 2
    if (ready && lockedCatCenterY == null) {
      lockedCatCenterY = liveCenter
    }
    const catApprox = Math.max(host.offsetHeight || 0, size || 72)
    const half = catApprox / 2
    const targetY =
      lockedCatCenterY != null
        ? Math.min(Math.max(lockedCatCenterY, r.top + half), r.bottom - half)
        : liveCenter
    const padTop = lockedCatCenterY != null ? Math.max(0, targetY - r.top - half) : 0
    Object.assign(host.style, {
      position: 'fixed',
      left: `${r.left}px`,
      top: `${r.top}px`,
      width: `${w}px`,
      height: `${h}px`,
      zIndex: Number.isFinite(zNum) ? String(zNum + 1) : '2010',
      display: 'flex',
      alignItems: 'flex-start',
      justifyContent: 'center',
      paddingTop: `${padTop}px`,
      visibility: lockedCatCenterY != null ? 'visible' : 'hidden',
      pointerEvents: 'none',
      boxSizing: 'border-box'
    })
  }
  syncPos()
  // 首帧后立刻再采一次，让 stableFrames 在定高后尽快达标
  requestAnimationFrame(() => {
    requestAnimationFrame(syncPos)
  })

  let ro = null
  if (typeof ResizeObserver !== 'undefined') {
    ro = new ResizeObserver(syncPos)
    ro.observe(mask)
  }
  const attrObserver = new MutationObserver(syncPos)
  attrObserver.observe(mask, { attributes: true, attributeFilter: ['style', 'class'] })
  const onTransition = () => syncPos()
  mask.addEventListener('transitionstart', onTransition)
  mask.addEventListener('transitionrun', onTransition)
  mask.addEventListener('transitionend', onTransition)
  window.addEventListener('scroll', syncPos, true)
  window.addEventListener('resize', syncPos)

  disposers.set(mask, () => {
    closedEarly = true
    window.removeEventListener('scroll', syncPos, true)
    window.removeEventListener('resize', syncPos)
    mask.removeEventListener('transitionstart', onTransition)
    mask.removeEventListener('transitionrun', onTransition)
    mask.removeEventListener('transitionend', onTransition)
    try {
      attrObserver.disconnect()
    } catch (_e) {
      /* ignore */
    }
    if (ro) {
      try {
        ro.disconnect()
      } catch (_e) {
        /* ignore */
      }
    }
    try {
      app.unmount()
    } catch (_e) {
      /* ignore */
    }
    if (host.parentNode) {
      host.parentNode.removeChild(host)
    }
    spinner.classList.remove(SPINNER_HIDE_CLASS)
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
