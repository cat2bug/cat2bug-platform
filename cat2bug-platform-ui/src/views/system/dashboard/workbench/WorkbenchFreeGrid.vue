<template>
  <div
    ref="root"
    class="wb-free-grid"
    :class="{ 'is-editing': editing, 'is-stack': stackMode }"
    :style="rootStyle"
  >
    <section
      v-for="item in displayItems"
      :key="item.i"
      class="wb-free-grid__item wb-widget"
      :class="{ 'is-editing': editing, 'is-dragging': dragId === item.i }"
      :style="itemStyle(item)"
      :data-wid="item.i"
    >
      <header
        class="wb-widget__hd"
        :class="{ 'is-draggable': editing && !stackMode }"
        @pointerdown="onHeaderPointerDown($event, item)"
      >
        <div class="wb-widget__hd-left">
          <span
            v-if="editing && !stackMode"
            class="wb-widget__drag"
            :title="$t('workbench.drag')"
          >⋮⋮</span>
          <span class="name">{{ titleOf(item.i) }}</span>
        </div>
        <div class="wb-widget__hd-right" @pointerdown.stop>
          <el-button v-if="editing" link type="danger" @click="$emit('remove', item.i)">
            {{ $t('workbench.hide') }}
          </el-button>
          <span v-else-if="metaOf(item.i)" class="wb-range">{{ metaOf(item.i) }}</span>
        </div>
      </header>
      <div class="wb-widget__bd" :class="{ 'is-pad': padBody(item.i) }">
        <slot name="widget" :id="item.i" />
      </div>
      <div
        v-if="editing && !stackMode"
        class="wb-free-grid__resize"
        :title="$t('workbench.resize')"
        @pointerdown.stop.prevent="onResizeStart($event, item)"
      />
    </section>
  </div>
</template>

<script>
import {
  GRID_COLS,
  GRID_MIN_W,
  GRID_MIN_H,
  GRID_ROW_H,
  GRID_GAP,
  GRID_STACK_MQ,
  layoutMaxY,
  sortLayoutForStack
} from './grid-layout'

export default {
  name: 'WorkbenchFreeGrid',
  props: {
    layout: { type: Array, default: () => [] },
    editing: { type: Boolean, default: false },
    titleOf: { type: Function, required: true },
    /** 右上角次要文案（如周报日期）；编辑态优先显示「隐藏」 */
    metaOf: { type: Function, default: () => '' },
    padBody: { type: Function, default: () => true }
  },
  emits: ['update:layout', 'remove'],
  data() {
    return {
      stackMode: false,
      dragId: null,
      cellW: 80,
      session: null
    }
  },
  computed: {
    displayItems() {
      if (this.stackMode) return sortLayoutForStack(this.layout)
      return this.layout || []
    },
    rowCount() {
      return Math.max(4, layoutMaxY(this.layout))
    },
    rootStyle() {
      if (this.stackMode) return {}
      return {
        '--wb-grid-rows': String(this.rowCount)
      }
    }
  },
  mounted() {
    this.measure()
    this.onResizeMq()
    window.addEventListener('resize', this.onWinResize)
    window.addEventListener('pointermove', this.onPointerMove)
    window.addEventListener('pointerup', this.onPointerUp)
    window.addEventListener('pointercancel', this.onPointerUp)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.onWinResize)
    window.removeEventListener('pointermove', this.onPointerMove)
    window.removeEventListener('pointerup', this.onPointerUp)
    window.removeEventListener('pointercancel', this.onPointerUp)
  },
  methods: {
    onWinResize() {
      this.measure()
      this.onResizeMq()
    },
    onResizeMq() {
      this.stackMode = window.innerWidth < GRID_STACK_MQ
    },
    measure() {
      const el = this.$refs.root
      if (!el) return
      const w = el.clientWidth
      this.cellW = Math.max(24, (w - GRID_GAP * (GRID_COLS - 1)) / GRID_COLS)
    },
    itemStyle(item) {
      if (this.stackMode) {
        return { gridColumn: '1 / -1' }
      }
      return {
        gridColumn: `${item.x + 1} / span ${item.w}`,
        gridRow: `${item.y + 1} / span ${item.h}`
      }
    },
    onHeaderPointerDown(e, item) {
      if (!this.editing || this.stackMode) return
      if (e.button != null && e.button !== 0) return
      // 避免点到标题内按钮/链接时开拖
      const t = e.target
      if (t && t.closest && t.closest('button, a, .el-button')) return
      e.preventDefault()
      this.onDragStart(e, item)
    },
    onDragStart(e, item) {
      this.measure()
      this.dragId = item.i
      this.session = {
        type: 'drag',
        id: item.i,
        startX: e.clientX,
        startY: e.clientY,
        origX: item.x,
        origY: item.y,
        w: item.w,
        h: item.h,
        pointerId: e.pointerId
      }
      try {
        e.currentTarget && e.currentTarget.setPointerCapture && e.currentTarget.setPointerCapture(e.pointerId)
      } catch (_) { /* ignore */ }
    },
    onResizeStart(e, item) {
      this.measure()
      this.dragId = item.i
      this.session = {
        type: 'resize',
        id: item.i,
        startX: e.clientX,
        startY: e.clientY,
        origW: item.w,
        origH: item.h,
        x: item.x,
        y: item.y,
        pointerId: e.pointerId
      }
      try {
        e.currentTarget && e.currentTarget.setPointerCapture && e.currentTarget.setPointerCapture(e.pointerId)
      } catch (_) { /* ignore */ }
    },
    onPointerMove(e) {
      if (!this.session || this.stackMode) return
      const s = this.session
      const dx = e.clientX - s.startX
      const dy = e.clientY - s.startY
      const stepX = this.cellW + GRID_GAP
      const stepY = GRID_ROW_H + GRID_GAP
      const next = (this.layout || []).map(it => ({ ...it }))
      const idx = next.findIndex(it => it.i === s.id)
      if (idx < 0) return
      if (s.type === 'drag') {
        let x = s.origX + Math.round(dx / stepX)
        let y = s.origY + Math.round(dy / stepY)
        x = Math.max(0, Math.min(GRID_COLS - s.w, x))
        y = Math.max(0, y)
        next[idx] = { ...next[idx], x, y }
      } else {
        let w = s.origW + Math.round(dx / stepX)
        let h = s.origH + Math.round(dy / stepY)
        w = Math.max(GRID_MIN_W, Math.min(GRID_COLS - s.x, w))
        h = Math.max(GRID_MIN_H, h)
        next[idx] = { ...next[idx], w, h }
      }
      this.$emit('update:layout', next)
    },
    onPointerUp() {
      this.session = null
      this.dragId = null
    }
  }
}
</script>

<style scoped lang="scss">
.wb-free-grid {
  --wb-grid-row-h: 48px;
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  grid-auto-rows: var(--wb-grid-row-h);
  gap: 12px;
  column-gap: 12px;
  row-gap: 12px;
  align-items: stretch;
}
.wb-free-grid.is-stack {
  grid-template-columns: 1fr;
  grid-auto-rows: auto;
}
.wb-free-grid__item {
  position: relative;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.wb-free-grid__item .wb-widget__bd {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: auto;
}
.wb-free-grid__item .wb-widget__bd > * {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
}
.wb-free-grid__item.is-dragging {
  opacity: 0.92;
  outline: 1px dashed var(--cat2bug-primary);
  outline-offset: -1px;
  z-index: 2;
}
.wb-widget__hd.is-draggable {
  cursor: grab;
  user-select: none;
  touch-action: none;
}
.wb-widget__hd.is-draggable:active {
  cursor: grabbing;
}
.wb-widget__drag {
  margin-right: 6px;
  color: var(--text-color-secondary);
  font-size: 14px;
  letter-spacing: -2px;
}
.wb-free-grid__resize {
  position: absolute;
  right: 4px;
  bottom: 4px;
  width: 14px;
  height: 14px;
  cursor: nwse-resize;
  border-right: 2px solid var(--border-color-base);
  border-bottom: 2px solid var(--border-color-base);
  opacity: 0.7;
}
.wb-free-grid__resize:hover {
  opacity: 1;
  border-color: var(--cat2bug-primary);
}
</style>
