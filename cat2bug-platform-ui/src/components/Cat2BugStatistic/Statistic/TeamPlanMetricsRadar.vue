<template>
  <cat2-bug-card
    class="plan-radar-card"
    :title="cardTitle"
    v-loading="loading"
    :tools="tools"
    @tools-click="toolsHandle"
  >
    <template slot="content">
      <div class="plan-radar-body">
        <div ref="chart" class="plan-radar-canvas" />
        <div v-show="showEmptyHint" class="plan-radar-empty">{{ emptyHintText }}</div>
      </div>
    </template>
  </cat2-bug-card>
</template>

<script>
import * as echarts from '@/assets/js/echarts.min.js'
import resize from '@/views/dashboard/mixins/resize'
import Cat2BugCard from '../Components/Card'
import { statisticPlanMetrics } from '@/api/system/statistic/defect'
import { mapGetters } from 'vuex'
import {
  disconnectEchartsResize,
  observeEchartsResize,
  afterChartRefReady,
  scheduleEchartsResize
} from '../utils/echarts-resize'
import { resolveCurrentProjectId } from '../utils/project-id'
import { statisticChartColors } from '../utils/chart-theme'

const RADAR_KEYS = ['discovery', 'repair', 'detection', 'severity', 'restart', 'escape']
const RADAR_I18N = [
  'plan.defect-discovery-rate',
  'plan.defect-repair-rate',
  'plan.defect-detection-rate',
  'plan.defect-severity-rate',
  'plan.defect-restart-rate',
  'plan.defect-escape-rate'
]
const RADAR_COLORS = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#6f7ad3']
const PLAN_METRICS_LIMIT = 4
const LEGEND_NAME_MAX = 9
const LEGEND_GAP_PX = 4
const RADAR_SIZE_RATIO = 0.46
const RADAR_EDGE_PAD_PX = 3
const RADAR_LEFT_PAD_PX = 4

export default {
  name: 'TeamPlanMetricsRadar',
  components: { Cat2BugCard },
  mixins: [resize],
  data() {
    return {
      loading: false,
      chart: null,
      metricsList: [],
      renderRetryTimer: null
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
    currentProjectId() {
      return resolveCurrentProjectId(this)
    },
    showEmptyHint() {
      return !this.loading && (!this.currentProjectId || !this.metricsList.length)
    },
    emptyHintText() {
      if (!this.currentProjectId) {
        return this.$t('defect.statistic.no-project').toString()
      }
      return this.$t('defect.team-plan-radar.empty').toString()
    },
    isDarkTheme() {
      return this.themeMode === 'dark'
    },
    cardTitle() {
      return this.$t('defect.team-plan-radar').toString()
    },
    displayPlans() {
      const sorted = [...this.metricsList].sort((a, b) => {
        const ta = a.updateTime ? new Date(a.updateTime).getTime() : 0
        const tb = b.updateTime ? new Date(b.updateTime).getTime() : 0
        return tb - ta
      })
      return sorted.slice(0, PLAN_METRICS_LIMIT)
    }
  },
  watch: {
    currentProjectId(val, oldVal) {
      if (val !== oldVal) {
        this.getPlanMetrics()
      }
    },
    themeMode() {
      if (this.metricsList.length) {
        this.renderChart()
      }
    },
    '$i18n.locale'() {
      if (this.metricsList.length) {
        this.renderChart()
      }
    }
  },
  mounted() {
    this.getPlanMetrics()
  },
  activated() {
    this.$nextTick(() => {
      if (this.metricsList.length) {
        this.renderChart()
      } else {
        this.getPlanMetrics()
      }
    })
  },
  beforeDestroy() {
    this.clearRenderRetry()
    this.clearChart()
  },
  methods: {
    getPlanMetrics() {
      if (!this.currentProjectId) {
        this.metricsList = []
        this.clearChart()
        return
      }
      this.loading = true
      statisticPlanMetrics(this.currentProjectId).then(res => {
        this.metricsList = Array.isArray(res.data) ? res.data : []
      }).catch(() => {
        this.metricsList = []
        this.clearChart()
      }).finally(() => {
        this.loading = false
        this.scheduleChartPaint()
      })
    },
    clearRenderRetry() {
      if (this.renderRetryTimer) {
        clearTimeout(this.renderRetryTimer)
        this.renderRetryTimer = null
      }
    },
    scheduleChartPaint() {
      this.clearRenderRetry()
      if (!this.metricsList.length) {
        this.clearChart()
        return
      }
      afterChartRefReady(this, () => this.renderChart())
      this.renderRetryTimer = setTimeout(() => {
        if (this.chart) {
          scheduleEchartsResize(this.chart, this.$refs.chart)
        } else {
          this.renderChart()
        }
      }, 200)
    },
    clearChart() {
      disconnectEchartsResize(this.chart)
      if (this.chart) {
        try {
          this.chart.dispose()
        } catch (e) { /* ignore */ }
        this.chart = null
      }
    },
    ensureChart() {
      const el = this.$refs.chart
      if (!el) return false
      const disposed = this.chart && typeof this.chart.isDisposed === 'function' && this.chart.isDisposed()
      if (this.chart && !disposed) return true
      const existing = typeof echarts.getInstanceByDom === 'function' ? echarts.getInstanceByDom(el) : null
      if (existing) {
        try {
          existing.dispose()
        } catch (e) { /* ignore */ }
      }
      this.chart = echarts.init(el, 'macarons')
      return true
    },
    planMetricValue(plan, key) {
      const v = plan[key]
      if (v == null || v === '') return 0
      const n = Number(v)
      return Number.isFinite(n) ? n : 0
    },
    truncateLegendName(name) {
      const text = (name || '').toString()
      if (text.length <= LEGEND_NAME_MAX) return text
      return `${text.slice(0, LEGEND_NAME_MAX - 1)}…`
    },
    formatPercentValue(value) {
      const n = Number(value)
      const text = Number.isFinite(n) ? n : 0
      return `${text}%`
    },
    formatRadarTooltip(params) {
      const p = Array.isArray(params) ? params[0] : params
      if (!p || !p.value) return ''
      const labels = RADAR_I18N.map(key => this.$t(key).toString())
      const marker = p.marker || ''
      const lines = p.value.map((v, i) => `${marker}${labels[i] || ''} ${this.formatPercentValue(v)}`)
      const title = p.name ? `${p.name}<br/>` : ''
      return title + lines.join('<br/>')
    },
    radarLayout() {
      const el = this.$refs.chart
      if (!el) {
        return { center: ['32%', '50%'], radius: '78%', legendLeft: 88 }
      }
      const w = el.clientWidth || 0
      const h = el.clientHeight || 0
      if (h < 24) {
        return { center: ['32%', '50%'], radius: '78%', legendLeft: 88 }
      }
      const radius = Math.max(32, Math.floor(h * RADAR_SIZE_RATIO) - RADAR_EDGE_PAD_PX)
      const cxPx = RADAR_LEFT_PAD_PX + radius
      const legendLeft = cxPx + radius + LEGEND_GAP_PX
      const cx = w > 0 ? (cxPx / w) * 100 : 32
      return {
        center: [`${Math.round(cx)}%`, '50%'],
        radius,
        legendLeft
      }
    },
    buildChartOption() {
      const dark = this.isDarkTheme
      const colors = statisticChartColors(dark)
      const layout = this.radarLayout()
      const indicator = RADAR_I18N.map(key => ({
        name: this.$t(key).toString(),
        max: 100
      }))
      const seriesData = this.displayPlans.map(plan => ({
        name: plan.planName || String(plan.planId),
        value: RADAR_KEYS.map(k => this.planMetricValue(plan, k))
      }))
      const splitAreaColors = dark
        ? ['rgba(255, 255, 255, 0.1)', 'rgba(255, 255, 255, 0.04)']
        : ['rgba(64, 158, 255, 0.08)', 'rgba(64, 158, 255, 0.03)']
      return {
        color: RADAR_COLORS,
        tooltip: {
          trigger: 'item',
          confine: false,
          appendTo: document.body,
          extraCssText: 'z-index: 5000;',
          backgroundColor: colors.tooltipBg,
          borderColor: colors.tooltipBorder,
          textStyle: { color: colors.tooltipText, fontSize: 11 },
          formatter: params => this.formatRadarTooltip(params)
        },
        legend: {
          show: seriesData.length > 0,
          type: 'plain',
          orient: 'vertical',
          left: layout.legendLeft,
          top: 'middle',
          itemWidth: 8,
          itemHeight: 8,
          itemGap: 3,
          icon: 'circle',
          textStyle: {
            fontSize: 10,
            lineHeight: 12,
            color: colors.axisLabel
          },
          formatter: name => this.truncateLegendName(name),
          data: seriesData.map(item => item.name)
        },
        radar: {
          radius: layout.radius,
          center: layout.center,
          indicator,
          axisName: { show: false },
          splitNumber: 4,
          splitLine: { lineStyle: { color: colors.radarLine, width: 1 } },
          splitArea: {
            show: true,
            areaStyle: { color: splitAreaColors }
          },
          axisLine: { lineStyle: { color: colors.radarLine, width: 1 } }
        },
        series: [{
          type: 'radar',
          symbolSize: 5,
          lineStyle: { width: 1.5 },
          areaStyle: { opacity: dark ? 0.22 : 0.16 },
          data: seriesData
        }]
      }
    },
    renderChart() {
      if (!this.metricsList.length) {
        this.clearChart()
        return
      }
      if (!this.ensureChart()) return
      const el = this.$refs.chart
      try {
        this.chart.setOption(this.buildChartOption(), true)
      } catch (e) {
        console.error('[TeamPlanMetricsRadar] setOption failed', e)
        return
      }
      observeEchartsResize(this.chart, el)
      scheduleEchartsResize(this.chart, el)
    },
    resize() {
      if (!this.chart || !this.metricsList.length) return
      try {
        const layout = this.radarLayout()
        this.chart.setOption({
          radar: { radius: layout.radius, center: layout.center },
          legend: { left: layout.legendLeft }
        })
        this.chart.resize()
      } catch (e) { /* ignore */ }
    },
    refreshData() {
      this.getPlanMetrics()
    },
    toolsHandle(e, tool) {
      this.$emit('tools-click', tool)
    }
  }
}
</script>

<style lang="scss" scoped>
.plan-radar-card {
  overflow: visible;

  ::v-deep .statistic-box {
    overflow: visible;
  }

  ::v-deep .statistic-box-header {
    margin-bottom: 4px;
    padding-bottom: 4px;
  }

  ::v-deep .statistic-box-body {
    padding: 0;
    overflow: visible;
    flex: 1 1 auto;
    min-height: 0;
    align-items: stretch;
    justify-content: stretch;
  }
}

.plan-radar-body {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.plan-radar-canvas {
  flex: 1 1 auto;
  width: 100%;
  min-height: 0;
  display: block;
  box-sizing: border-box;
}

.plan-radar-empty {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-color-secondary, #909399);
  background: var(--statistic-card-bg, var(--bg-color, #fff));
}
</style>
