<template>
  <cat2-bug-card
    class="participation-heatmap-card"
    :title="$i18n.t('defect.my-participation').toString()"
    v-loading="loading"
    :tools="tools"
    @tools-click="toolsHandle"
  >
    <template slot="content">
      <div class="participation-heatmap-body">
        <div v-if="showEmptyHint" class="participation-heatmap-empty">{{ emptyHintText }}</div>
        <div v-else ref="gridWrap" class="participation-heatmap-grid-wrap" :style="gridWrapStyle">
          <div class="participation-heatmap-y-labels" :style="{ height: gridWrapStyle.height }">
            <span v-for="label in weekdayLabels" :key="label">{{ label }}</span>
          </div>
          <div class="participation-heatmap-grid-scroll">
            <div
              class="participation-heatmap-grid"
              :style="gridStyle"
              @mousemove="onGridMouseMove"
              @mouseleave="hideHoverTip"
            >
              <span
                v-for="(slot, index) in gridSlots"
                :key="index"
                :data-idx="index"
                class="participation-heatmap-cell"
                :class="{ 'is-pad': slot.pad, 'is-selected': !slot.pad && slot.date === selectedDate }"
                :style="cellStyle(slot)"
                @click.stop="onCellClick(slot)"
              />
            </div>
          </div>
        </div>
        <div
          v-show="hoverTip.visible"
          class="participation-heatmap-tooltip"
          :class="{ 'is-dark': isDarkTheme }"
          :style="hoverTipStyle"
        >{{ hoverTip.text }}</div>
        <div v-show="showZeroDataHint" class="participation-heatmap-zero-hint">{{ $t('defect.my-participation.empty') }}</div>
      </div>
    </template>
  </cat2-bug-card>
</template>

<script>
import Cat2BugCard from '../Components/Card'
import { statisticMyParticipation } from '@/api/system/statistic/defect'
import { mapGetters } from 'vuex'
import { resolveCurrentProjectId } from '../utils/project-id'
import { buildCalendarHeatmapData, WEEKDAY_LABELS, heatColorForCount } from '../utils/calendar-heatmap'
import { calendarHeatmapTheme } from '../utils/chart-theme'

const CALENDAR_DAYS = 84
const GRID_GAP = 3
const GRID_ROWS = 7

export default {
  name: 'MyParticipationHeatmap',
  components: { Cat2BugCard },
  data() {
    return {
      loading: false,
      dayList: [],
      gridSlots: [],
      weekCount: 0,
      heatMax: 1,
      allZero: true,
      cellSize: 7,
      resizeObserver: null,
      hoverTip: {
        visible: false,
        text: '',
        x: 0,
        y: 0
      },
      selectedDate: null
    }
  },
  props: {
    params: { type: Object, default: () => ({}) },
    tools: { type: Array, default: () => [] },
    parent: { type: Object, default: () => ({}) },
    read: { type: Boolean, default: false }
  },
  computed: {
    ...mapGetters(['themeMode']),
    weekdayLabels() {
      return WEEKDAY_LABELS
    },
    currentProjectId() {
      return resolveCurrentProjectId(this)
    },
    showEmptyHint() {
      return !this.loading && (!this.currentProjectId || !this.dayList.length)
    },
    showZeroDataHint() {
      return !this.loading && this.dayList.length > 0 && this.allZero
    },
    emptyHintText() {
      if (!this.currentProjectId) {
        return this.$t('defect.statistic.no-project').toString()
      }
      return this.$t('defect.my-participation.empty').toString()
    },
    currentUserId() {
      const id = this.$store.state.user.id
      const n = Number(id)
      return Number.isFinite(n) ? n : null
    },
    isDarkTheme() {
      return this.themeMode === 'dark'
    },
    heatTheme() {
      return calendarHeatmapTheme(this.isDarkTheme)
    },
    gridWrapStyle() {
      const gridH = this.cellSize * GRID_ROWS + GRID_GAP * (GRID_ROWS - 1)
      return {
        '--cell-size': `${this.cellSize}px`,
        '--grid-gap': `${GRID_GAP}px`,
        height: `${gridH}px`,
        minHeight: `${gridH}px`,
        maxHeight: `${gridH}px`
      }
    },
    gridStyle() {
      if (!this.weekCount) return {}
      const s = this.cellSize
      return {
        gridTemplateRows: `repeat(${GRID_ROWS}, ${s}px)`,
        gridAutoColumns: `${s}px`,
        gridAutoFlow: 'column',
        gap: `${GRID_GAP}px`,
        backgroundColor: this.heatTheme.gridGap
      }
    },
    hoverTipStyle() {
      return {
        left: `${this.hoverTip.x}px`,
        top: `${this.hoverTip.y}px`
      }
    }
  },
  watch: {
    currentProjectId(val, oldVal) {
      if (val !== oldVal) {
        this.getParticipation()
      }
    },
    themeMode() {
      this.applyGridData()
    }
  },
  mounted() {
    this.getParticipation()
    this.bindResizeObserver()
  },
  beforeDestroy() {
    if (this.resizeObserver) {
      this.resizeObserver.disconnect()
      this.resizeObserver = null
    }
  },
  methods: {
    bindResizeObserver() {
      this.$nextTick(() => {
        const body = this.getChartBodyEl()
        if (!body || typeof ResizeObserver === 'undefined') {
          this.measureCellSize()
          return
        }
        this.resizeObserver = new ResizeObserver(() => this.measureCellSize())
        this.resizeObserver.observe(body)
        this.measureCellSize()
      })
    },
    getChartBodyEl() {
      return this.$el && this.$el.querySelector('.statistic-box-body')
    },
    measureCellSize() {
      const body = this.getChartBodyEl()
      const h = body && body.clientHeight > 0 ? body.clientHeight : 0
      if (!h) return
      const size = Math.max(6, Math.min(10, Math.floor((h - GRID_GAP * (GRID_ROWS - 1)) / GRID_ROWS)))
      if (size !== this.cellSize) {
        this.cellSize = size
      }
    },
    getParticipation() {
      if (!this.currentProjectId) {
        this.dayList = []
        this.gridSlots = []
        this.weekCount = 0
        return
      }
      this.loading = true
      statisticMyParticipation(this.currentProjectId, CALENDAR_DAYS).then(res => {
        this.dayList = res.data || []
        this.applyGridData()
        this.$nextTick(this.measureCellSize)
      }).catch(() => {
        this.dayList = []
        this.gridSlots = []
        this.weekCount = 0
      }).finally(() => {
        this.loading = false
      })
    },
    applyGridData() {
      const { slots, weekCount, max, allZero } = buildCalendarHeatmapData(this.dayList)
      this.gridSlots = slots
      this.weekCount = weekCount
      this.heatMax = max
      this.allZero = allZero
      this.$nextTick(this.measureCellSize)
    },
    cellStyle(slot) {
      if (slot.pad) {
        return { backgroundColor: 'transparent' }
      }
      return {
        backgroundColor: heatColorForCount(slot.count, this.heatMax, this.heatTheme)
      }
    },
    formatCellTooltip(slot) {
      if (slot.pad || !slot.date) return ''
      return this.$t('defect.my-participation.tooltip', {
        date: slot.date,
        count: slot.count
      }).toString()
    },
    onGridMouseMove(e) {
      const cell = e.target.closest('.participation-heatmap-cell')
      if (!cell || cell.classList.contains('is-pad')) {
        this.hideHoverTip()
        return
      }
      const idx = parseInt(cell.dataset.idx, 10)
      const slot = this.gridSlots[idx]
      if (!slot || !slot.date) {
        this.hideHoverTip()
        return
      }
      this.hoverTip = {
        visible: true,
        text: this.formatCellTooltip(slot),
        x: e.clientX,
        y: e.clientY - 8
      }
    },
    hideHoverTip() {
      this.hoverTip.visible = false
    },
    onCellClick(slot) {
      if (slot.pad || !slot.date || this.read || !this.currentUserId) return
      if (!this.parent || typeof this.parent.searchQuery !== 'function') return
      this.selectedDate = slot.date
      this.hideHoverTip()
      this.parent.searchQuery({
        stack: false,
        extension: {
          participationLogDate: slot.date,
          participationUserId: this.currentUserId
        },
        common: {
          params: { defectStates: [], delFlag: '0' }
        }
      })
    },
    refreshData() {
      this.getParticipation()
    },
    toolsHandle(e, tool) {
      this.$emit('tools-click', tool)
    }
  }
}
</script>

<style lang="scss" scoped>
.participation-heatmap-card {
  width: max-content;
  max-width: 100%;

  ::v-deep .statistic-box-body {
    overflow: visible;
    align-items: flex-start;
    justify-content: flex-start;
  }
}

.participation-heatmap-body {
  position: relative;
  width: max-content;
  max-width: 100%;
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  align-items: flex-start;
}

.participation-heatmap-grid-wrap {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  width: max-content;
  max-width: 100%;
  gap: 4px;
  box-sizing: border-box;
}

.participation-heatmap-y-labels {
  flex: 0 0 14px;
  display: grid;
  grid-template-rows: repeat(7, var(--cell-size, 9px));
  gap: var(--grid-gap, 3px);
  align-items: center;
  justify-items: center;
  align-content: start;
  font-size: 8px;
  line-height: 1;
  color: var(--text-color-secondary, #909399);
}

.participation-heatmap-grid-scroll {
  flex: 0 0 auto;
  overflow-x: auto;
  overflow-y: visible;
  max-width: 100%;
  height: 100%;
}

.participation-heatmap-grid {
  display: grid;
  width: max-content;
  box-sizing: border-box;
}

.participation-heatmap-cell {
  display: block;
  width: var(--cell-size, 9px);
  height: var(--cell-size, 9px);
  box-sizing: border-box;
  border-radius: 2px;
  cursor: default;
  flex-shrink: 0;

  &:not(.is-pad) {
    cursor: pointer;
  }

  &:not(.is-pad):hover {
    outline: 1px solid rgba(64, 158, 255, 0.65);
    outline-offset: -1px;
  }

  &.is-selected:not(.is-pad) {
    outline: 2px solid #409eff;
    outline-offset: -1px;
  }
}

.participation-heatmap-tooltip {
  position: fixed;
  z-index: 9999;
  pointer-events: none;
  padding: 4px 8px;
  font-size: 11px;
  line-height: 1.3;
  border-radius: 4px;
  white-space: nowrap;
  transform: translate(-50%, -100%);
  background: #303133;
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);

  &.is-dark {
    background: #252529;
    border: 1px solid #363637;
    color: #e5eaf3;
  }
}

.participation-heatmap-zero-hint {
  position: absolute;
  right: 6px;
  bottom: 4px;
  z-index: 1;
  font-size: 10px;
  line-height: 1.2;
  color: var(--text-color-secondary, #909399);
  pointer-events: none;
  opacity: 0.85;
}

.participation-heatmap-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 48px;
  font-size: 12px;
  color: var(--text-color-secondary, #909399);
}
</style>
