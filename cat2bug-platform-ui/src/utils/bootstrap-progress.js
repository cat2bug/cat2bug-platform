/** 首屏冷启动进度：仅用 index.html 内联 #page-load-bar，完成前禁止 NProgress */
const BAR_ID = 'page-load-bar'

let coldBootPending = typeof document !== 'undefined'
  && !!document.getElementById(BAR_ID)

let coldBootCompleteTimer = null

export function isColdBootPending() {
  return coldBootPending
}

export function isBootstrapProgressActive() {
  return coldBootPending && !!document.getElementById(BAR_ID)
}

export function cancelColdBootComplete() {
  if (coldBootCompleteTimer != null) {
    window.clearTimeout(coldBootCompleteTimer)
    coldBootCompleteTimer = null
  }
}

export function finishBootstrapProgress() {
  const bar = document.getElementById(BAR_ID)
  if (!bar || bar.classList.contains('is-done')) return
  bar.classList.add('is-done')
  window.setTimeout(() => {
    bar.remove()
  }, 220)
}

/** 首屏路由链（含 GetInfo / 动态路由 replace）全部完成后调用一次 */
export function completeColdBoot() {
  if (!coldBootPending) return
  coldBootPending = false
  cancelColdBootComplete()
  try {
    document.documentElement.removeAttribute('data-booting')
  } catch (e) {
    /* ignore */
  }
  finishBootstrapProgress()
}

/**
 * 连续 afterEach（动态路由 replace）会多次触发；防抖后再结束冷启动。
 */
export function scheduleColdBootComplete(delayMs = 180) {
  if (!coldBootPending) return
  cancelColdBootComplete()
  coldBootCompleteTimer = window.setTimeout(() => {
    coldBootCompleteTimer = null
    completeColdBoot()
  }, delayMs)
}

/** data-booting / coldBootPending 期间禁止 NProgress.start */
export function installNProgressColdBootGuard(NProgress) {
  if (!NProgress || NProgress.__coldBootGuardInstalled) return
  const nativeStart = NProgress.start.bind(NProgress)
  const nativeDone = NProgress.done.bind(NProgress)
  NProgress.start = function coldBootGuardedStart(...args) {
    if (coldBootPending) return NProgress
    return nativeStart(...args)
  }
  NProgress.done = function coldBootGuardedDone(...args) {
    if (coldBootPending) return NProgress
    return nativeDone(...args)
  }
  NProgress.__coldBootGuardInstalled = true
}
