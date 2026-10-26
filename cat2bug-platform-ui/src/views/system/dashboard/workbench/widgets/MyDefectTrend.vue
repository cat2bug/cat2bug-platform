<template>
  <div class="my-trend" v-loading="loading">
    <div class="my-trend__toolbar">
      <el-radio-group v-model="days" size="small" @change="load">
        <el-radio-button :value="14">14</el-radio-button>
        <el-radio-button :value="30">30</el-radio-button>
      </el-radio-group>
    </div>
    <div ref="chart" class="my-trend__chart" />
    <div class="my-trend__hint">{{ isTeam ? $t('workbench.trend-hint-team') : $t('workbench.trend-hint') }}</div>
    <div v-if="empty" class="my-trend__empty">{{ $t('workbench.empty-chart') }}</div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { storeToRefs } from 'pinia'
import { useSettingsStore } from '@/stores/settings'
import { statisticMyParticipation, statisticMemberDefectLine } from '@/api/system/statistic/defect'
import { getCurrentProjectId } from '@/utils/store-context'
import { buildDashboardAxisStyle, getDashboardChartTheme } from '@/utils/dashboard-chart-theme'

export default {
  name: 'MyDefectTrend',
  props: {
    /** me = 本人参与；team = 全员处理量汇总 */
    scope: { type: String, default: 'me' }
  },
  setup() {
    const { themeMode } = storeToRefs(useSettingsStore())
    return { themeMode }
  },
  data() {
    return { loading: false, days: 14, chart: null, empty: false, lastSeries: null }
  },
  computed: {
    isTeam() {
      return this.scope === 'team'
    },
    isDarkTheme() {
      return this.themeMode === 'dark'
    }
  },
  watch: {
    themeMode() {
      if (this.lastSeries) this.applyChart(this.lastSeries.dates, this.lastSeries.values)
    }
  },
  mounted() {
    this.load()
    window.addEventListener('resize', this.onResize)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.onResize)
    if (this.chart) {
      this.chart.dispose()
      this.chart = null
    }
  },
  methods: {
    onResize() {
      this.chart && this.chart.resize()
    },
    async load() {
      const projectId = getCurrentProjectId()
      if (!projectId) return
      this.loading = true
      try {
        const { dates, values } = this.isTeam
          ? await this.loadTeamSeries(projectId)
          : await this.loadMeSeries(projectId)
        this.empty = !dates.length || values.every(v => !v)
        await this.$nextTick()
        if (!this.chart) this.chart = echarts.init(this.$refs.chart)
        this.lastSeries = { dates, values }
        this.applyChart(dates, values)
        await this.$nextTick()
        this.chart && this.chart.resize()
      } catch (e) {
        this.empty = true
      } finally {
        this.loading = false
      }
    },
    async loadMeSeries(projectId) {
      const res = await statisticMyParticipation(projectId, this.days)
      const rows = res.data || res || []
      const list = Array.isArray(rows) ? rows : (rows.rows || [])
      return {
        dates: list.map(r => r.date || r.day || r.statDate || ''),
        values: list.map(r => Number(r.count || r.value || r.total || 0))
      }
    },
    async loadTeamSeries(projectId) {
      const res = await statisticMemberDefectLine(projectId, { timeType: 'day' })
      const payload = res.data || res || {}
      const times = Array.isArray(payload.time) ? payload.time : (payload.times || [])
      const rows = Array.isArray(payload.data) ? payload.data : []
      const byDay = new Map()
      for (const t of times) byDay.set(t, 0)
      for (const row of rows) {
        const t = row.createTime || row.defectTime || row.date
        if (!t) continue
        const n = Number(row.defectTodayCount ?? row.defectCount ?? row.count ?? 0)
        byDay.set(t, (byDay.get(t) || 0) + n)
      }
      let dates = times.length ? times.slice() : [...byDay.keys()].sort()
      if (dates.length > this.days) dates = dates.slice(dates.length - this.days)
      return {
        dates,
        values: dates.map(d => Number(byDay.get(d) || 0))
      }
    },
    applyChart(dates, values) {
      if (!this.chart) return
      const theme = getDashboardChartTheme(this.isDarkTheme)
      const axisStyle = buildDashboardAxisStyle(theme)
      const barColor = this.isDarkTheme ? '#69b1ff' : '#1890ff'
      this.chart.setOption({
        grid: { left: 28, right: 8, top: 8, bottom: 22 },
        xAxis: {
          type: 'category',
          data: dates,
          axisTick: { show: false },
          ...axisStyle,
          axisLabel: {
            ...axisStyle.axisLabel,
            fontSize: 10,
            interval: Math.max(1, Math.floor(dates.length / 7))
          }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          splitNumber: 3,
          ...axisStyle,
          axisLabel: { ...axisStyle.axisLabel, fontSize: 10 },
          splitLine: { lineStyle: { color: theme.splitColor, type: 'dashed' } }
        },
        series: [{
          type: 'bar',
          data: values,
          itemStyle: { color: barColor, opacity: this.isDarkTheme ? 0.75 : 0.55, borderRadius: [2, 2, 0, 0] },
          barMaxWidth: 10
        }]
      }, true)
    }
  }
}
</script>

<style scoped lang="scss">
.my-trend {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 160px;
}
.my-trend__toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 4px;
  flex-shrink: 0;
}
.my-trend__chart {
  flex: 1;
  min-height: 140px;
  width: 100%;
}
.my-trend__hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-color-secondary);
  flex-shrink: 0;
}
.my-trend__empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-color-secondary);
  font-size: 12px;
  padding: 12px 0;
}
</style>
