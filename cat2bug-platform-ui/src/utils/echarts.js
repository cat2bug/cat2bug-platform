/**
 * Vite/ESM shim for vendored UMD echarts.min.js (side-effect assigns globalThis.echarts).
 */
import '@/assets/js/echarts.min.js'

const echarts = globalThis.echarts

if (!echarts || typeof echarts.init !== 'function') {
  throw new Error('[echarts] failed to load from echarts.min.js')
}

export default echarts
