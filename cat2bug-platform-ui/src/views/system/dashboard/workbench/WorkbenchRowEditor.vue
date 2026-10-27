<template>
  <div class="wb-editor">
    <aside class="wb-editor__catalog">
      <div class="wb-editor__catalog-title">{{ $t('workbench.editor.catalog') }}</div>
      <div
        v-for="w in catalog"
        :key="w.id"
        class="wb-editor__chip"
        draggable="true"
        @dragstart="onCatalogDragStart($event, w.id)"
      >
        {{ $t(w.titleKey) }}
      </div>
    </aside>

    <div class="wb-editor__canvas">
      <div v-for="(row, ri) in localRows" :key="row.id" class="wb-editor__row">
        <div class="wb-editor__row-bar">
          <span class="wb-editor__row-label">{{ $t('workbench.editor.row-n', [ri + 1]) }}</span>
          <el-select
            size="small"
            :model-value="row.cols.length"
            style="width: 88px"
            @update:model-value="(n) => setColCount(ri, n)"
          >
            <el-option v-for="n in 4" :key="n" :label="$t('workbench.editor.cols-n', [n])" :value="n" />
          </el-select>
          <el-select
            size="small"
            :model-value="row.height?.mode || 'row'"
            style="width: 110px"
            @update:model-value="(m) => setRowHeightMode(ri, m)"
          >
            <el-option :label="$t('workbench.editor.height-content')" value="content" />
            <el-option :label="$t('workbench.editor.height-row')" value="row" />
            <el-option :label="$t('workbench.editor.height-fixed')" value="fixed" />
          </el-select>
          <el-input-number
            v-if="row.height?.mode === 'fixed'"
            size="small"
            :model-value="row.height.px || 240"
            :min="80"
            :max="800"
            :step="20"
            controls-position="right"
            style="width: 110px"
            @update:model-value="(px) => setRowHeightPx(ri, px)"
          />
          <el-select
            size="small"
            :model-value="ratioLabel(row)"
            :placeholder="$t('workbench.editor.ratio-ph')"
            style="width: 128px"
            @update:model-value="(v) => applyRatio(ri, v)"
          >
            <el-option
              v-for="opt in ratioOptions(row)"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-button size="small" link type="danger" @click="removeRow(ri)">{{ $t('delete') }}</el-button>
        </div>

        <div
          class="wb-editor__cols"
          :class="{
            'is-stack': stackMode,
            'is-resizing': resize?.ri === ri,
            'is-h-fixed': row.height?.mode === 'fixed',
            'is-h-content': row.height?.mode === 'content',
            'is-h-row': !row.height?.mode || row.height?.mode === 'row'
          }"
          :style="colsStyle(row)"
        >
          <template v-for="(col, ci) in row.cols" :key="col.id">
            <div
              class="wb-editor__slot"
              :style="slotStyle(row, col)"
              :class="{
                'is-over': dragOverColId === col.id && dragOverInstanceId == null,
                'is-empty': !colWidgetsOf(col).length
              }"
              @dragover.prevent="onSlotDragOver($event, col.id, null)"
              @dragleave="onSlotLeave(col.id)"
              @drop="onDrop($event, col.id, null)"
            >
              <template v-if="colWidgetsOf(col).length">
                <div class="wb-editor__stack">
                  <div
                    v-for="(w, wi) in colWidgetsOf(col)"
                    :key="w.instanceId || w.type"
                    class="wb-editor__item"
                    :class="{
                      'is-over': dragOverColId === col.id && dragOverInstanceId === w.instanceId
                    }"
                    draggable="true"
                    @dragstart.stop="onInstanceDragStart($event, col.id, w.instanceId)"
                    @dragover.prevent.stop="onSlotDragOver($event, col.id, w.instanceId, wi)"
                    @drop.stop="onDrop($event, col.id, wi)"
                  >
                    <div class="wb-editor__filled-hd">
                      <span class="wb-editor__drag-hint" :title="$t('workbench.editor.drag-reorder')">⋮⋮</span>
                      <span class="wb-editor__item-title">{{ titleOf(w) }}</span>
                      <div class="wb-editor__filled-actions">
                        <el-button
                          link
                          type="danger"
                          size="small"
                          @click.stop="removeInstance(w.instanceId)"
                        >{{ $t('remove') }}</el-button>
                      </div>
                    </div>
                    <div class="wb-editor__filled-bd">
                      <component
                        :is="componentOf(w.type)"
                        v-bind="propsOf(w, true)"
                        @update:doc-id="(id) => onDocId(col.id, id, w.instanceId)"
                      />
                    </div>
                  </div>
                  <div class="wb-editor__add">
                    <el-select
                      size="small"
                      filterable
                      clearable
                      :placeholder="$t('workbench.editor.add-widget')"
                      style="width: 100%; max-width: 220px"
                      @update:model-value="(type) => pickWidget(col.id, type)"
                    >
                      <el-option
                        v-for="opt in catalogForCol(col)"
                        :key="opt.id"
                        :label="$t(opt.titleKey)"
                        :value="opt.id"
                      />
                    </el-select>
                  </div>
                </div>
              </template>
              <template v-else>
                <div class="wb-editor__empty">
                  <div>{{ $t('workbench.editor.drop-here') }}</div>
                  <el-select
                    size="small"
                    filterable
                    clearable
                    :placeholder="$t('workbench.editor.pick-widget')"
                    style="width: 100%; max-width: 220px"
                    @update:model-value="(type) => pickWidget(col.id, type)"
                  >
                    <el-option
                      v-for="opt in catalogForCol(col)"
                      :key="opt.id"
                      :label="$t(opt.titleKey)"
                      :value="opt.id"
                    />
                  </el-select>
                </div>
              </template>
            </div>
            <div
              v-if="!stackMode && ci < row.cols.length - 1"
              class="wb-editor__resize"
              :title="$t('workbench.editor.resize-col')"
              @mousedown.prevent.stop="startColResize($event, ri, ci)"
            />
          </template>
        </div>
      </div>

      <el-button class="wb-editor__add-row" @click="addRow">{{ $t('workbench.editor.add-row') }}</el-button>
    </div>
  </div>
</template>

<script>
import {
  normalizeRow,
  setRowColCount,
  applyRatioString,
  placeWidgetInCol,
  clearColWidget,
  removeWidgetInstance,
  moveWidgetInstance,
  columnHasWidgetType,
  findWidgetInstance,
  findCol,
  ratioSelectOptions,
  normalizeSpans,
  resizeAdjacentSpans,
  filterRatioOptionsByMins,
  colWidgets,
  GRID_SPAN,
  spansLabel
} from './row-layout'
import { listCatalogWidgets, colMinSpans } from './widget-registry'

const STACK_MQ = 720

export default {
  name: 'WorkbenchRowEditor',
  props: {
    rows: { type: Array, default: () => [] },
    titleOf: { type: Function, required: true },
    propsOf: { type: Function, required: true },
    componentOf: { type: Function, required: true }
  },
  emits: ['update:rows'],
  data() {
    return {
      dragType: null,
      dragFromColId: null,
      dragInstanceId: null,
      dragOverColId: null,
      dragOverInstanceId: null,
      stackMode: false,
      /** { ri, ci, startX, rowWidth, spans, mins } */
      resize: null
    }
  },
  computed: {
    catalog() {
      return listCatalogWidgets()
    },
    localRows() {
      return this.rows || []
    }
  },
  mounted() {
    this.onResize()
    window.addEventListener('resize', this.onResize)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.onResize)
    this.teardownColResize()
  },
  methods: {
    onResize() {
      this.stackMode = window.innerWidth < STACK_MQ
    },
    startColResize(e, ri, ci) {
      if (this.stackMode) return
      const colsEl = e.currentTarget.parentElement
      if (!colsEl) return
      const row = this.localRows[ri]
      if (!row || row.cols.length < 2) return
      const spans = normalizeSpans(row.cols.map(c => c.ratio))
      this.resize = {
        ri,
        ci,
        startX: e.clientX,
        rowWidth: Math.max(1, colsEl.getBoundingClientRect().width),
        spans,
        mins: colMinSpans(row.cols)
      }
      document.body.classList.add('wb-col-resizing')
      window.addEventListener('mousemove', this.onColResizeMove)
      window.addEventListener('mouseup', this.onColResizeEnd)
    },
    onColResizeMove(e) {
      const s = this.resize
      if (!s) return
      const deltaUnits = ((e.clientX - s.startX) / s.rowWidth) * GRID_SPAN
      const nextSpans = resizeAdjacentSpans(s.spans, s.ci, deltaUnits, s.mins)
      const next = JSON.parse(JSON.stringify(this.localRows))
      next[s.ri].cols.forEach((c, i) => {
        c.ratio = nextSpans[i]
      })
      this.emitRows(next)
    },
    onColResizeEnd() {
      this.teardownColResize()
    },
    teardownColResize() {
      this.resize = null
      document.body.classList.remove('wb-col-resizing')
      window.removeEventListener('mousemove', this.onColResizeMove)
      window.removeEventListener('mouseup', this.onColResizeEnd)
    },
    emitRows(next) {
      this.$emit('update:rows', next)
    },
    warnDuplicateType() {
      this.$message.warning(this.$t('workbench.editor.duplicate-type'))
    },
    applyPlaceOrWarn(prev, next) {
      if (next === prev) {
        this.warnDuplicateType()
        return
      }
      this.emitRows(next)
    },
    ratioLabel(row) {
      return spansLabel(normalizeSpans((row.cols || []).map(c => c.ratio)))
    },
    ratioOptions(row) {
      const label = this.ratioLabel(row)
      const opts = ratioSelectOptions(row.cols?.length || 1, label)
      return filterRatioOptionsByMins(opts, colMinSpans(row.cols), label)
    },
    colsStyle(row) {
      const h = row?.height
      if (h?.mode === 'fixed' && h.px) {
        const px = Math.max(80, Number(h.px) || 240)
        return {
          height: px + 'px',
          minHeight: px + 'px',
          maxHeight: px + 'px'
        }
      }
      return {}
    },
    slotStyle(row, col) {
      if (this.stackMode) return {}
      const idx = row.cols.findIndex(c => c.id === col.id)
      const spans = normalizeSpans((row.cols || []).map(c => c.ratio))
      const span = spans[idx] || 1
      return {
        flex: `${span} ${span} 0`,
        minWidth: '0',
        minHeight: '0'
      }
    },
    setColCount(ri, n) {
      const next = this.localRows.slice()
      next[ri] = setRowColCount(next[ri], n)
      this.emitRows(next)
    },
    setRowHeightMode(ri, mode) {
      const next = this.localRows.slice()
      const row = { ...normalizeRow(next[ri]) }
      row.height = mode === 'fixed'
        ? { mode: 'fixed', px: row.height?.px || 240 }
        : { mode }
      next[ri] = row
      this.emitRows(next)
    },
    setRowHeightPx(ri, px) {
      const next = this.localRows.slice()
      const row = { ...normalizeRow(next[ri]) }
      row.height = { mode: 'fixed', px: Number(px) || 240 }
      next[ri] = row
      this.emitRows(next)
    },
    applyRatio(ri, value) {
      if (!value) return
      const next = this.localRows.slice()
      next[ri] = applyRatioString(next[ri], value)
      this.emitRows(next)
    },
    addRow() {
      this.emitRows([
        ...this.localRows,
        normalizeRow({
          cols: [{ ratio: GRID_SPAN, widget: null }]
        })
      ])
    },
    rowHasContent(row) {
      return (row?.cols || []).some(c => colWidgets(c).length > 0)
    },
    doRemoveRow(ri) {
      const next = this.localRows.slice()
      next.splice(ri, 1)
      this.emitRows(next.length ? next : [normalizeRow({ cols: [{ ratio: GRID_SPAN }] })])
    },
    removeRow(ri) {
      const row = this.localRows[ri]
      if (!row) return
      if (!this.rowHasContent(row)) {
        this.doRemoveRow(ri)
        return
      }
      this.$confirm(
        this.$t('workbench.editor.remove-row-confirm'),
        this.$t('workbench.editor.remove-row-title'),
        { type: 'warning', confirmButtonText: this.$t('ok'), cancelButtonText: this.$t('cancel') }
      )
        .then(() => this.doRemoveRow(ri))
        .catch(() => {})
    },
    colWidgetsOf(col) {
      return colWidgets(col)
    },
    catalogForCol(col) {
      const used = new Set(colWidgets(col).map(w => w.type))
      return this.catalog.filter(w => !used.has(w.id))
    },
    clearSlot(colId) {
      this.emitRows(clearColWidget(this.localRows, colId))
    },
    removeInstance(instanceId) {
      if (!instanceId) return
      this.emitRows(removeWidgetInstance(this.localRows, instanceId))
    },
    pickWidget(colId, type) {
      if (!type) return
      const prev = this.localRows
      const next = placeWidgetInCol(prev, colId, type, {})
      this.applyPlaceOrWarn(prev, next)
    },
    onDocId(colId, docId, instanceId) {
      const next = JSON.parse(JSON.stringify(this.localRows))
      for (const row of next) {
        for (const col of row.cols) {
          if (col.id !== colId) continue
          const list = colWidgets(col)
          for (const w of list) {
            if (instanceId && w.instanceId !== instanceId) continue
            if (!instanceId && w.type !== 'document-pin') continue
            w.props = { ...(w.props || {}), docId }
          }
          col.widgets = list
          col.widget = list[0] || null
        }
      }
      this.emitRows(next)
    },
    onCatalogDragStart(e, type) {
      this.dragType = type
      this.dragFromColId = null
      this.dragInstanceId = null
      e.dataTransfer.setData('text/plain', 'catalog:' + type)
      e.dataTransfer.effectAllowed = 'copy'
    },
    onInstanceDragStart(e, colId, instanceId) {
      this.dragFromColId = colId
      this.dragInstanceId = instanceId
      this.dragType = null
      e.dataTransfer.setData('text/plain', 'instance:' + instanceId)
      e.dataTransfer.effectAllowed = 'move'
    },
    onSlotDragOver(e, colId, instanceId) {
      this.dragOverColId = colId
      this.dragOverInstanceId = instanceId || null
    },
    onSlotLeave(colId) {
      if (this.dragOverColId === colId) {
        this.dragOverColId = null
        this.dragOverInstanceId = null
      }
    },
    /**
     * @param {string} colId
     * @param {number|null} insertIndex 落在某组件上时为该组件下标（移动后的最终位置）；列空白为 null → 追加末尾
     */
    onDrop(e, colId, insertIndex) {
      e.preventDefault()
      this.dragOverColId = null
      this.dragOverInstanceId = null
      const raw = e.dataTransfer.getData('text/plain') || ''
      const prev = this.localRows

      if (raw.startsWith('catalog:') || this.dragType) {
        const type = raw.startsWith('catalog:') ? raw.slice(8) : this.dragType
        const hit = findCol(prev, colId)
        if (hit && columnHasWidgetType(hit.col, type)) {
          this.warnDuplicateType()
        } else {
          const next = placeWidgetInCol(prev, colId, type, {}, insertIndex)
          this.applyPlaceOrWarn(prev, next)
        }
      } else if (raw.startsWith('instance:') || this.dragInstanceId) {
        const instanceId = raw.startsWith('instance:') ? raw.slice(9) : this.dragInstanceId
        const from = findWidgetInstance(prev, instanceId)
        if (!from) {
          /* ignore */
        } else if (from.col.id === colId && (insertIndex == null || insertIndex === from.widgetIndex)) {
          /* no-op same spot */
        } else {
          const next = moveWidgetInstance(prev, instanceId, colId, insertIndex)
          this.applyPlaceOrWarn(prev, next)
        }
      }

      this.dragType = null
      this.dragFromColId = null
      this.dragInstanceId = null
    }
  }
}
</script>

<style scoped lang="scss">
.wb-editor {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 12px;
  align-items: start;
}
@media (max-width: 900px) {
  .wb-editor {
    grid-template-columns: 1fr;
  }
}
.wb-editor__catalog {
  position: sticky;
  top: 8px;
  padding: 10px;
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 10px;
  background: var(--el-bg-color, var(--bg-color-base, #fff));
  color: var(--text-color-primary, var(--el-text-color-primary, #303133));
  max-height: calc(100vh - 160px);
  overflow: auto;
}
.wb-editor__catalog-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 650;
  color: var(--text-color-primary, var(--el-text-color-primary, #303133));
}
.wb-editor__chip {
  padding: 8px 10px;
  margin-bottom: 6px;
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter, transparent);
  background: var(--el-fill-color, var(--bg-color-block, #f5f7fa));
  color: var(--text-color-primary, var(--el-text-color-primary, #303133));
  font-size: 12px;
  cursor: grab;
  user-select: none;
}
.wb-editor__chip:hover {
  background: var(--el-fill-color-dark, var(--el-fill-color-light, #ebeef5));
  color: var(--text-color-primary, var(--el-text-color-primary, #303133));
}
.wb-editor__chip:active {
  cursor: grabbing;
}
.wb-editor__canvas {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}
.wb-editor__row {
  padding: 10px;
  border: 1px dashed var(--el-border-color, #dcdfe6);
  border-radius: 10px;
  background: var(--el-fill-color-blank, #fafafa);
}
.wb-editor__row-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
}
.wb-editor__row-label {
  font-size: 13px;
  font-weight: 650;
  margin-right: 4px;
}
.wb-editor__cols {
  display: flex;
  gap: 0;
  align-items: stretch;
  box-sizing: border-box;
}
.wb-editor__cols.is-stack {
  flex-direction: column;
  gap: 10px;
}
.wb-editor__cols.is-h-fixed {
  overflow: hidden;
}
.wb-editor__slot {
  min-width: 0;
  min-height: 120px;
  box-sizing: border-box;
  padding-right: 5px;
  padding-left: 5px;
  display: flex;
  flex-direction: column;
}
.wb-editor__cols.is-h-fixed .wb-editor__slot {
  min-height: 0;
  height: 100%;
  overflow: hidden;
}
.wb-editor__slot:first-child {
  padding-left: 0;
}
.wb-editor__cols > .wb-editor__slot:last-child {
  padding-right: 0;
}
.wb-editor__cols.is-stack .wb-editor__slot {
  flex: 1 1 auto !important;
  max-width: none !important;
  width: 100%;
  padding-left: 0;
  padding-right: 0;
}
.wb-editor__resize {
  flex: 0 0 6px;
  align-self: stretch;
  margin: 0 -3px;
  position: relative;
  z-index: 2;
  cursor: col-resize;
  border-radius: 3px;
  background: transparent;
}
.wb-editor__resize::after {
  content: '';
  position: absolute;
  top: 12%;
  bottom: 12%;
  left: 2px;
  width: 2px;
  border-radius: 1px;
  background: var(--el-border-color, #dcdfe6);
  opacity: 0.7;
}
.wb-editor__resize:hover::after,
.wb-editor__cols.is-resizing .wb-editor__resize::after {
  background: var(--el-color-primary, #409eff);
  opacity: 1;
}
.wb-editor__cols.is-stack .wb-editor__resize {
  display: none;
}
.wb-editor__slot.is-over,
.wb-editor__item.is-over {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}
.wb-editor__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 100%;
  min-height: 120px;
  padding: 12px;
  border: 1px dashed var(--el-border-color, #dcdfe6);
  border-radius: 8px;
  background: var(--el-bg-color, #fff);
  color: var(--text-color-secondary);
  font-size: 12px;
  box-sizing: border-box;
}
.wb-editor__stack {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 120px;
  flex: 1 1 auto;
  min-width: 0;
}
.wb-editor__cols.is-h-fixed .wb-editor__stack {
  min-height: 0;
  height: 100%;
  overflow: auto;
}
.wb-editor__item {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-lighter, #ebeef5);
  border-radius: 8px;
  background: var(--el-bg-color, #fff);
  overflow: hidden;
  cursor: grab;
  min-height: 0;
}
.wb-editor__cols.is-h-fixed .wb-editor__item {
  flex: 1 1 0;
}
.wb-editor__item:active {
  cursor: grabbing;
}
.wb-editor__filled-hd {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 650;
  border-bottom: 1px solid var(--el-border-color-lighter, #ebeef5);
  background: var(--el-fill-color, var(--bg-color-block, #f5f7fa));
  color: var(--text-color-primary, var(--el-text-color-primary, #303133));
}
.wb-editor__drag-hint {
  color: var(--text-color-secondary, #909399);
  letter-spacing: -2px;
  font-weight: 700;
  cursor: grab;
  user-select: none;
}
.wb-editor__item-title {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.wb-editor__filled-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.wb-editor__filled-bd {
  padding: 8px;
  max-height: 280px;
  overflow: auto;
  flex: 1 1 auto;
  min-height: 0;
}
.wb-editor__cols.is-h-fixed .wb-editor__filled-bd {
  max-height: none;
}
.wb-editor__add {
  display: flex;
  justify-content: center;
  padding: 4px 0 2px;
}
.wb-editor__add-row {
  align-self: flex-start;
}
</style>

<style lang="scss">
/* 拖列宽时全局光标（非 scoped） */
body.wb-col-resizing,
body.wb-col-resizing * {
  cursor: col-resize !important;
  user-select: none !important;
}
</style>
