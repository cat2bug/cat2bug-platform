/**
 * 统计条内 ECharts 容器常在 flex/横向滚动布局后才获得宽度，需延迟 resize。
 */
export function scheduleEchartsResize(chart, el) {
  if (!chart || !el) {
    return
  }
  const resize = () => {
    if (!chart) return
    try {
      if (typeof chart.isDisposed === 'function' && chart.isDisposed()) return
      chart.resize()
    } catch (e) { /* ignore */ }
  }
  requestAnimationFrame(() => {
    requestAnimationFrame(resize)
  })
  setTimeout(resize, 120)
  setTimeout(resize, 400)
}

export function observeEchartsResize(chart, el) {
  if (!chart || !el) {
    return null
  }
  const doResize = () => {
    try {
      if (typeof chart.isDisposed === 'function' && chart.isDisposed()) return
      chart.resize()
    } catch (e) { /* ignore */ }
  }
  if (typeof ResizeObserver !== 'undefined') {
    const ro = new ResizeObserver(() => doResize())
    ro.observe(el)
    chart.__resizeRo = ro
  }
  requestAnimationFrame(() => {
    requestAnimationFrame(doResize)
  })
  setTimeout(doResize, 120)
  setTimeout(doResize, 400)
  return chart.__resizeRo || null
}

/** Vue2：v-if 切换后 ref 需再等一帧才能 init ECharts */
export function afterChartRefReady(vm, fn) {
  vm.$nextTick(() => {
    vm.$nextTick(() => {
      fn()
    })
  })
}

export function disconnectEchartsResize(chart) {
  if (chart && chart.__resizeRo) {
    chart.__resizeRo.disconnect()
    chart.__resizeRo = null
  }
}
