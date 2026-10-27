<template>
  <div ref="frame" class="catalog-thumb" :style="frameStyle" aria-hidden="true">
    <div ref="stage" class="catalog-thumb__stage" :style="stageStyle">
      <slot />
    </div>
  </div>
</template>

<script>
/** 按「设计宽度」渲染真实组件，再按侧栏宽度等比缩小；高度随内容变化 */
const DESIGN_W = 720

export default {
  name: 'CatalogLiveThumb',
  data() {
    return {
      frameW: DESIGN_W,
      contentH: 120,
      ro: null,
      mo: null
    }
  },
  computed: {
    scale() {
      if (!this.frameW) return 1
      return Math.min(1, this.frameW / DESIGN_W)
    },
    frameStyle() {
      return {
        height: `${Math.max(48, Math.round(this.contentH * this.scale))}px`
      }
    },
    stageStyle() {
      return {
        width: `${DESIGN_W}px`,
        transform: `scale(${this.scale})`,
        transformOrigin: 'top left'
      }
    }
  },
  mounted() {
    this.measureFrame()
    if (typeof ResizeObserver !== 'undefined') {
      this.ro = new ResizeObserver(() => {
        this.measureFrame()
        this.measureContent()
      })
      if (this.$refs.frame) this.ro.observe(this.$refs.frame)
      if (this.$refs.stage) this.ro.observe(this.$refs.stage)
    }
    this.$nextTick(() => {
      this.measureContent()
      // 图表/列表异步出数后再量一次
      this._t1 = setTimeout(() => this.measureContent(), 400)
      this._t2 = setTimeout(() => this.measureContent(), 1200)
    })
  },
  beforeUnmount() {
    if (this.ro) this.ro.disconnect()
    clearTimeout(this._t1)
    clearTimeout(this._t2)
  },
  updated() {
    this.$nextTick(() => this.measureContent())
  },
  methods: {
    measureFrame() {
      const el = this.$refs.frame
      if (!el) return
      const w = el.clientWidth
      if (w > 0) this.frameW = w
    },
    measureContent() {
      const stage = this.$refs.stage
      if (!stage) return
      const h = stage.scrollHeight || stage.offsetHeight
      if (h > 0) this.contentH = h
    }
  }
}
</script>

<style scoped>
.catalog-thumb {
  width: 100%;
  overflow: hidden;
  pointer-events: none;
  background: var(--el-fill-color-darker, #141414);
}
.catalog-thumb__stage {
  /* transform 不占布局，高度由外层 frameStyle 按等比结果设置 */
  will-change: transform;
}
</style>
