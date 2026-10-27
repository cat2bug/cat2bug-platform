<template>
  <div class="wb-rows" :class="{ 'is-stack': stackMode }">
    <div
      v-for="row in visibleRows"
      :key="row.id"
      class="wb-rows__row"
      :class="'is-h-' + (row.height?.mode || 'row')"
      :style="rowStyle(row)"
    >
      <div
        v-for="(col, ci) in row.cols"
        :key="col.id"
        class="wb-rows__col"
        :class="[
          'is-h-' + effectiveHeight(row, col).mode,
          { 'is-stack-col': colWidgetsOf(col).length > 1 }
        ]"
        :style="colStyle(row, col, ci)"
      >
        <section
          v-for="w in colWidgetsOf(col)"
          :key="w.instanceId || w.type"
          class="wb-widget"
          :class="{ 'is-bare': isBare(w.type) }"
        >
          <header class="wb-widget__hd">
            <div class="wb-widget__hd-left">
              <span class="name">{{ titleOf(w) }}</span>
            </div>
            <div v-if="metaOf(w)" class="wb-widget__hd-right">
              <span class="wb-range">{{ metaOf(w) }}</span>
            </div>
          </header>
          <div class="wb-widget__bd" :class="{ 'is-pad': !isBare(w.type) }">
            <component
              :is="componentOf(w.type)"
              v-bind="propsOf(w)"
            />
          </div>
        </section>
      </div>
    </div>
    <div v-if="!visibleRows.length" class="workbench-empty">
      <slot name="empty" />
    </div>
  </div>
</template>

<script>
import { normalizeSpans, colWidgets } from './row-layout'
import { getWidgetDef } from './widget-registry'

const STACK_MQ = 720

export default {
  name: 'WorkbenchRows',
  props: {
    rows: { type: Array, default: () => [] },
    titleOf: { type: Function, required: true },
    metaOf: { type: Function, default: () => '' },
    propsOf: { type: Function, required: true },
    componentOf: { type: Function, required: true }
  },
  data() {
    return { stackMode: false }
  },
  computed: {
    visibleRows() {
      return (this.rows || [])
        .map(row => ({
          ...row,
          cols: (row.cols || [])
            .map(c => ({
              ...c,
              widgets: colWidgets(c).filter(w => getWidgetDef(w.type))
            }))
            .filter(c => c.widgets.length)
        }))
        .filter(row => row.cols.length)
    }
  },
  mounted() {
    this.onResize()
    window.addEventListener('resize', this.onResize)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.onResize)
  },
  methods: {
    onResize() {
      this.stackMode = window.innerWidth < STACK_MQ
    },
    colWidgetsOf(col) {
      return colWidgets(col).filter(w => getWidgetDef(w.type))
    },
    isBare(type) {
      return !!getWidgetDef(type)?.bare
    },
    effectiveHeight(row, col) {
      return col.height || row.height || { mode: 'row' }
    },
    rowStyle(row) {
      const h = row.height || { mode: 'row' }
      if (h.mode === 'fixed' && h.px) {
        const px = Math.max(80, Number(h.px) || 240)
        // 锁定行高：仅设 height 时，子项 min-height:auto 仍可能把行撑破
        return {
          height: px + 'px',
          minHeight: px + 'px',
          maxHeight: px + 'px',
          overflow: 'hidden'
        }
      }
      return {}
    },
    colStyle(row, col, ci) {
      const style = {}
      if (!this.stackMode) {
        // flex-grow 按 24 栅格 span 分配「扣除 gap 后」的剩余宽度，避免 % + gap 溢出/留白
        const spans = normalizeSpans((row.cols || []).map(c => c.ratio))
        const span = spans[ci] || 1
        style.flex = `${span} ${span} 0`
        style.minWidth = '0'
      }
      const h = this.effectiveHeight(row, col)
      if (h.mode === 'fixed' && h.px) {
        const px = Math.max(80, Number(h.px) || 240)
        style.height = px + 'px'
        style.minHeight = '0'
        style.maxHeight = px + 'px'
        style.overflow = 'auto'
      } else if (h.mode === 'content') {
        // 内容高度：不随行拉伸（行默认 align-items:stretch）
        style.alignSelf = 'flex-start'
        style.height = 'auto'
      } else if (h.mode === 'row' || (!col.height && row.height?.mode === 'row')) {
        style.alignSelf = 'stretch'
        style.minHeight = '0'
      }
      return style
    }
  }
}
</script>

<style scoped lang="scss">
.wb-rows {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
}
.wb-rows__row {
  display: flex;
  flex-wrap: nowrap;
  gap: 12px;
  align-items: stretch;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
}
.wb-rows.is-stack .wb-rows__row {
  flex-direction: column;
}
.wb-rows__col {
  min-width: 0;
  box-sizing: border-box;
}
.wb-rows__row.is-h-fixed {
  box-sizing: border-box;
}
.wb-rows__col.is-h-fixed,
.wb-rows__col.is-h-row {
  box-sizing: border-box;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.wb-rows__col.is-stack-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  align-items: stretch;
}
/* 堆叠默认：均分列高；内容在组件内滚动，禁止画出盒子盖住兄弟/上一行 */
.wb-rows__col.is-stack-col > .wb-widget {
  flex: 1 1 0%;
  min-height: 0;
  height: auto;
  max-height: 100%;
  overflow: hidden;
  position: relative;
  z-index: 0;
}
/* content：各组件取自然高度，列高=堆叠之和；行高由各列 max 决定 */
.wb-rows__col.is-h-content.is-stack-col > .wb-widget {
  flex: 0 0 auto;
  height: auto;
  max-height: none;
  min-height: 0;
  overflow: visible;
}
.wb-rows.is-stack .wb-rows__col {
  flex: 1 1 auto !important;
  max-width: none !important;
  width: 100%;
}
.wb-widget {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 10px;
  background: var(--el-bg-color, #fff);
  overflow: hidden;
  box-sizing: border-box;
}
.wb-rows__col.is-h-content .wb-widget {
  height: auto;
}
/* 指定高度/行高度：仅非堆叠单组件列吃满列高 */
.wb-rows__col.is-h-fixed:not(.is-stack-col) > .wb-widget,
.wb-rows__col.is-h-row:not(.is-stack-col) > .wb-widget {
  flex: 1 1 auto;
  min-height: 0;
  height: auto;
  max-height: 100%;
  overflow: hidden;
}
/* 行高度 + 堆叠：均分列高 */
.wb-rows__col.is-h-row.is-stack-col > .wb-widget {
  flex: 1 1 0%;
  min-height: 0;
  height: auto;
  overflow: hidden;
}
/* 指定高度 + 堆叠：组件按内容高度自上而下排列，超出在列上滚动（勿均分压扁） */
.wb-rows__col.is-h-fixed.is-stack-col > .wb-widget {
  flex: 0 0 auto;
  height: auto;
  max-height: none;
  overflow: visible;
}
.wb-rows__col.is-h-fixed.is-stack-col > .wb-widget.is-bare {
  overflow: visible;
}
.wb-rows__col.is-h-fixed.is-stack-col .wb-widget__bd {
  overflow: visible;
}
.wb-widget.is-bare {
  border: none;
  background: transparent;
  overflow: visible;
}
/* bare 在固定/行高/堆叠列内必须裁剪，否则会盖住相邻组件标题 */
.wb-rows__col.is-h-fixed .wb-widget.is-bare,
.wb-rows__col.is-h-row .wb-widget.is-bare,
.wb-rows__col.is-stack-col .wb-widget.is-bare {
  overflow: hidden;
}
.wb-widget.is-bare .wb-widget__hd {
  padding-left: 0;
  padding-right: 0;
}
.wb-widget__hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px 0;
  flex: 0 0 auto;
}
.wb-widget__hd .name {
  font-size: 14px;
  font-weight: 650;
  color: var(--text-color-primary);
}
.wb-range {
  font-size: 12px;
  color: var(--text-color-secondary);
}
.wb-widget__bd {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}
.wb-widget__bd.is-pad {
  padding: 8px 12px 12px;
}
.wb-rows__col.is-h-content .wb-widget__bd {
  overflow: visible;
}
.workbench-empty {
  padding: 48px 0;
}
</style>
