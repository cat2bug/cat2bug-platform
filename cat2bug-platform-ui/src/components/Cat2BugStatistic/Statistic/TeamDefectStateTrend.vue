<template>
  <cat2-bug-card
    class="defect-trend-card"
    :title="cardTitle"
    v-loading="loading"
    :tools="tools"
    @tools-click="toolsHandle"
  >
    <template slot="right-tools">
      <el-radio-group
        v-model="query.timeType"
        size="mini"
        class="defect-trend-time-type"
        @input="handleTimeTypeChange"
        @click.native.stop
      >
        <el-radio-button v-for="tt in timeTypeList" :key="tt.value" :label="tt.value">
          {{ $t(tt.label).toString() }}
        </el-radio-button>
      </el-radio-group>
      <el-tooltip v-if="!read" effect="dark" :content="$t('export').toString()" placement="top">
        <el-button type="text" icon="el-icon-download" class="defect-trend-export" @click.stop="handleExport" />
      </el-tooltip>
    </template>
    <template slot="content">
      <div ref="trendBody" class="defect-trend-body">
        <div ref="chart" class="defect-trend-canvas" />
        <div v-show="showEmptyHint" class="defect-trend-empty">{{ emptyHintText }}</div>
      </div>
    </template>
  </cat2-bug-card>
</template>

<script>
import * as echarts from '@/assets/js/echarts.min.js'
import resize from '@/views/dashboard/mixins/resize'
import Cat2BugCard from '../Components/Card'
import { statisticDefectStateLine } from '@/api/system/statistic/defect'
import { mapGetters } from 'vuex'
import { disconnectEchartsResize, observeEchartsResize, afterChartRefReady } from '../utils/echarts-resize'
import { resolveCurrentProjectId } from '../utils/project-id'
import {
  buildDefectStateLineSeries,
  buildTrendLineChartOption,
  DEFECT_STATE_NAME_TO_ID,
  formatUpdateTimeRange,
  refreshDefectStateSeriesI18n
} from '../utils/defect-trend-chart'

export default {
  name: 'TeamDefectStateTrend',
  components: { Cat2BugCard },
  mixins: [resize],
  data() {
    return {
      loading: false,
      chart: null,
      query: { timeType: 'day' },
      timeTypeList: [
        { value: 'day', label: 'dashboard.day' },
        { value: 'month', label: 'dashboard.month' }
      ],
      lastLegend: [],
      lastXData: [],
      lastSeries: [],
      hasLoaded: false
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
    cardTitle() {
      return this.query.timeType === 'month'
        ? this.$t('defect.team-defect-state-trend.month').toString()
        : this.$t('defect.team-defect-state-trend').toString()
    },
    showEmptyHint() {
      return !this.loading && this.hasLoaded && (!this.currentProjectId || !this.lastXData.length)
    },
    emptyHintText() {
      if (!this.currentProjectId) {
        return this.$t('defect.statistic.no-project').toString()
      }
      return this.$t('no-data').toString()
    },
    isDarkTheme() {
      return this.themeMode === 'dark'
    }
  },
  watch: {
    currentProjectId(val, oldVal) {
      if (val !== oldVal) {
        this.loadData()
      }
    },
    themeMode() {
      if (this.chart && this.lastXData.length) {
        this.applyChart()
      }
    },
    '$i18n.locale'() {
      this.refreshSeriesI18n()
    }
  },
  mounted() {
    this.loadData()
  },
  beforeDestroy() {
    this.clearChart()
  },
  methods: {
    loadData() {
      if (!this.currentProjectId) {
        this.hasLoaded = true
        this.clearChart()
        return
      }
      this.loading = true
      statisticDefectStateLine(this.currentProjectId, this.query).then(res => {
        const times = res.data.times || []
        const xData = Array.isArray(times) ? [...times] : [...times]
        const series = buildDefectStateLineSeries(res.data, k => this.$t(k).toString())
        this.lastLegend = series.map(s => s.name)
        this.lastXData = xData
        this.lastSeries = series
        this.hasLoaded = true
        afterChartRefReady(this, () => {
          if (!this.ensureChart()) return
          this.applyChart()
          this.bindChartClick()
        })
      }).catch(() => {
        this.lastXData = []
        this.lastSeries = []
        this.hasLoaded = true
        this.clearChart()
      }).finally(() => {
        this.loading = false
      })
    },
    ensureChart() {
      const el = this.$refs.chart
      if (!el) return false
      if (!this.chart) {
        this.chart = echarts.init(el, 'macarons')
      }
      return true
    },
    refreshSeriesI18n() {
      if (!this.lastSeries.length) return
      refreshDefectStateSeriesI18n(this.lastSeries, k => this.$t(k).toString())
      this.lastLegend = this.lastSeries.map(s => s.name)
      if (this.chart && this.lastXData.length) {
        this.applyChart()
      }
    },
    applyChart() {
      if (!this.chart) return
      const opt = buildTrendLineChartOption({
        legendData: this.lastLegend,
        xData: this.lastXData,
        series: this.lastSeries,
        isDark: this.isDarkTheme,
        tooltipAppendTo: this.$refs.trendBody || undefined
      })
      this.chart.setOption(opt, true)
      observeEchartsResize(this.chart, this.$refs.chart)
      this.$nextTick(() => this.chart && this.chart.resize())
    },
    bindChartClick() {
      if (!this.chart || this.read) return
      this.chart.off('click')
      this.chart.on('click', params => {
        if (!params || params.componentType !== 'series') return
        const matched = this.lastSeries[params.seriesIndex]
        const enumKey = matched && matched._stateKey
        const stateId = enumKey ? DEFECT_STATE_NAME_TO_ID[enumKey] : null
        if (stateId == null || !this.parent || typeof this.parent.searchQuery !== 'function') return
        const range = formatUpdateTimeRange(params.name, this.query.timeType)
        this.parent.searchQuery({
          stack: false,
          common: {
            params: {
              defectStates: stateId,
              beginUpdateTime: range.beginUpdateTime,
              endUpdateTime: range.endUpdateTime,
              delFlag: '0'
            }
          }
        })
      })
    },
    handleTimeTypeChange() {
      this.loadData()
    },
    handleExport() {
      if (!this.currentProjectId) return
      const title = this.cardTitle
      this.download(
        `/system/defect/statistic/defect-state-line/${this.currentProjectId}/export`,
        { ...this.query },
        `${title}_${new Date().getTime()}.xlsx`
      )
    },
    clearChart() {
      disconnectEchartsResize(this.chart)
      if (this.chart) {
        this.chart.off('click')
        this.chart.dispose()
        this.chart = null
      }
      this.lastLegend = []
      this.lastXData = []
      this.lastSeries = []
    },
    resize() {
      this.chart && this.chart.resize()
    },
    refreshData() {
      this.loadData()
    },
    toolsHandle(e, tool) {
      this.$emit('tools-click', tool)
    }
  }
}
</script>

<style lang="scss" scoped>
.defect-trend-card {
  overflow: visible;

  ::v-deep .statistic-box {
    overflow: visible;
  }

  ::v-deep .statistic-box-header {
    min-height: var(--statistic-card-header-min-height, 17px);
    align-items: center;
    margin-bottom: 2px;
    padding-bottom: 2px;
  }

  ::v-deep .statistic-box-header .cat2-bug-title {
    display: flex;
    align-items: center;
    line-height: var(--statistic-card-header-min-height, 17px);
  }

  ::v-deep .statistic-box-header .statistic-box-tools {
    gap: 4px;
    align-items: center;
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

.defect-trend-time-type {
  ::v-deep .el-radio-button__inner {
    padding: 2px 5px;
    font-size: 10px;
  }
}

.defect-trend-export {
  padding: 0 2px;
  font-size: 14px;
}

.defect-trend-body {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: stretch;
}

.defect-trend-canvas {
  flex: 1 1 auto;
  width: 100%;
  min-height: 0;
  height: 100%;
}

.defect-trend-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-color-secondary, #909399);
  background: var(--statistic-card-bg, var(--bg-color, #fff));
  z-index: 1;
}
</style>
