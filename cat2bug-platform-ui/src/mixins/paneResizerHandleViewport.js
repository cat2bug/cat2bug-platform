import { computeResizerHandleTopPx } from '@/utils/multipaneResizerViewport'

/**
 * 缺陷/用例等页：左右拖动条方块相对视窗与竖线重叠段垂直居中（需模板 multipane-resizer 设 ref="paneResizer"）。
 */
export default {
  data() {
    return {
      _paneHandleRaf: null,
      _onPaneHandleViewport: null,
    }
  },
  mounted() {
    this._onPaneHandleViewport = () => {
      this.scheduleSyncPaneResizerHandle()
    }
    window.addEventListener('resize', this._onPaneHandleViewport)
    window.addEventListener('scroll', this._onPaneHandleViewport, true)
    const vv = typeof window !== 'undefined' ? window.visualViewport : null
    if (vv && typeof vv.addEventListener === 'function') {
      vv.addEventListener('scroll', this._onPaneHandleViewport)
      vv.addEventListener('resize', this._onPaneHandleViewport)
    }
  },
  beforeDestroy() {
    if (this._onPaneHandleViewport) {
      window.removeEventListener('resize', this._onPaneHandleViewport)
      window.removeEventListener('scroll', this._onPaneHandleViewport, true)
      const vv = typeof window !== 'undefined' ? window.visualViewport : null
      if (vv && typeof vv.removeEventListener === 'function') {
        vv.removeEventListener('scroll', this._onPaneHandleViewport)
        vv.removeEventListener('resize', this._onPaneHandleViewport)
      }
    }
    if (this._paneHandleRaf != null) {
      cancelAnimationFrame(this._paneHandleRaf)
      this._paneHandleRaf = null
    }
  },
  methods: {
    scheduleSyncPaneResizerHandle() {
      if (this._paneHandleRaf != null) return
      this._paneHandleRaf = requestAnimationFrame(() => {
        this._paneHandleRaf = null
        this.syncPaneResizerHandleTop()
      })
    },
    syncPaneResizerHandleTop() {
      const comp = this.$refs.paneResizer
      if (!comp || !this.showModuleTree) return
      const el = comp.$el || comp
      if (!el || typeof el.getBoundingClientRect !== 'function') return
      const rectH = el.getBoundingClientRect().height
      const legacy = parseFloat(String(this.multipaneStyle['--marginTop'] || '0')) || 0
      const lineH = (rectH > 1 ? rectH : legacy) || 8
      const px = computeResizerHandleTopPx(el, lineH)
      if (px == null || Number.isNaN(px)) return
      this.$set(this.multipaneStyle, '--resizerHandleTop', `${Math.round(px * 100) / 100}px`)
    },
  },
}
