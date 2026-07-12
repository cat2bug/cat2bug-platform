import echarts from '@/utils/echarts.js'

export function isChartDisposed(chart) {
  return !chart || (typeof chart.isDisposed === 'function' && chart.isDisposed())
}

/** 初始化 ECharts（避免未注册的 macarons 主题；复用 DOM 上已有实例时先 dispose） */
export function initEchartsChart(el, theme) {
  if (!el) return null
  const existing = typeof echarts.getInstanceByDom === 'function' ? echarts.getInstanceByDom(el) : null
  if (existing) {
    try {
      existing.dispose()
    } catch (e) { /* ignore */ }
  }
  const resolvedTheme = theme && theme !== 'macarons' ? theme : undefined
  return echarts.init(el, resolvedTheme)
}

export function safeSetOption(chart, option, notMerge = false) {
  if (isChartDisposed(chart) || !option) return false
  try {
    let payload = option
    if (option.series != null) {
      const series = option.series
      if (Array.isArray(series)) {
        payload = { ...option, series: sanitizeChartSeries(series) }
      } else if (!series || typeof series.type !== 'string') {
        payload = { ...option, series: [] }
      }
    }
    if (notMerge) {
      chart.setOption(payload, { notMerge: true, replaceMerge: ['series'] })
    } else {
      chart.setOption(payload, notMerge)
    }
    return true
  } catch (e) {
    console.warn('[echarts] setOption failed', e)
    return false
  }
}

export function safeResizeChart(chart) {
  if (isChartDisposed(chart)) return
  try {
    chart.resize()
  } catch (e) { /* ignore */ }
}

export function scheduleEchartsResize(chart, el) {
  if (isChartDisposed(chart) || !el) {
    return
  }
  const resize = () => safeResizeChart(chart)
  requestAnimationFrame(() => {
    requestAnimationFrame(resize)
  })
  setTimeout(resize, 120)
  setTimeout(resize, 400)
}

export function observeEchartsResize(chart, el) {
  if (isChartDisposed(chart) || !el) {
    return null
  }
  disconnectEchartsResize(chart)
  const doResize = () => safeResizeChart(chart)
  if (typeof ResizeObserver !== 'undefined') {
    const ro = new ResizeObserver(() => doResize())
    ro.observe(el)
    chart.__resizeRo = ro
  }
  const timers = []
  const schedule = (fn, ms) => {
    const id = setTimeout(fn, ms)
    timers.push(id)
    return id
  }
  requestAnimationFrame(() => {
    requestAnimationFrame(doResize)
  })
  schedule(doResize, 120)
  schedule(doResize, 400)
  chart.__resizeTimers = timers
  return chart.__resizeRo || null
}

/** Vue2：v-if 切换后 ref 需再等一帧才能 init ECharts */
export function afterChartRefReady(vm, fn) {
  if (!vm || vm._isDestroyed || vm.isUnmounted) return
  vm.$nextTick(() => {
    if (vm._isDestroyed || vm.isUnmounted) return
    vm.$nextTick(() => {
      if (vm._isDestroyed || vm.isUnmounted) return
      fn()
    })
  })
}

export function disconnectEchartsResize(chart) {
  if (chart && chart.__resizeTimers) {
    chart.__resizeTimers.forEach((id) => clearTimeout(id))
    chart.__resizeTimers = null
  }
  if (chart && chart.__resizeRo) {
    chart.__resizeRo.disconnect()
    chart.__resizeRo = null
  }
}

export function disposeEchartsChart(chart) {
  disconnectEchartsResize(chart)
  if (!chart) return
  try {
    chart.dispose()
  } catch (e) { /* ignore */ }
}

/** 过滤无效 series，避免 ECharts reset 时读取 undefined.type */
export function sanitizeChartSeries(series) {
  if (!Array.isArray(series)) return []
  return series.filter((item) => item && typeof item.type === 'string' && item.type)
}
