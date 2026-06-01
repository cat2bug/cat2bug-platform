<template>
  <cat2-bug-card
    class="plan-burndown-card"
    :title="cardTitle"
    v-loading="loading"
    :tools="tools"
    @tools-click="toolsHandle"
  >
    <template slot="left-tools">
      <el-select
        v-if="planList.length"
        v-model="planId"
        class="plan-burndown-select"
        popper-class="plan-burndown-select-dropdown"
        size="mini"
        filterable
        @change="handlePlanChange"
        @click.native.stop
      >
        <el-option
          v-for="item in planList"
          :key="String(item.planId)"
          :label="item.planName || String(item.planId)"
          :value="String(item.planId)"
          :title="item.planName || String(item.planId)"
        >
          <span class="plan-burndown-option-text">{{ item.planName || item.planId }}</span>
        </el-option>
      </el-select>
    </template>
    <template slot="content">
      <div class="plan-burndown-body">
        <div ref="chart" class="plan-burndown-canvas" />
        <div v-show="showEmptyHint" class="plan-burndown-empty">{{ emptyHintText }}</div>
      </div>
    </template>
  </cat2-bug-card>
</template>

<script>
import * as echarts from '@/assets/js/echarts.min.js'
import resize from '@/views/dashboard/mixins/resize'
import Cat2BugCard from '../Components/Card'
import { statisticPlanBurndown, statisticPlanList } from '@/api/system/statistic/defect'
import { mapGetters } from 'vuex'
import { disconnectEchartsResize, observeEchartsResize, afterChartRefReady } from '../utils/echarts-resize'
import { resolveCurrentProjectId } from '../utils/project-id'
import { statisticChartColors } from '../utils/chart-theme'

export default {
  name: 'TeamPlanBurndown',
  components: { Cat2BugCard },
  mixins: [resize],
  data() {
    return {
      loading: false,
      chart: null,
      planList: [],
      planId: null,
      lastXData: [],
      lastSeries: []
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
      return !this.loading && (!this.currentProjectId || !this.planList.length)
    },
    emptyHintText() {
      if (!this.currentProjectId) {
        return this.$t('defect.statistic.no-project').toString()
      }
      return this.$t('defect.team-plan-burndown.empty').toString()
    },
    isDarkTheme() {
      return this.themeMode === 'dark'
    },
    cardTitle() {
      return this.$t('defect.team-plan-burndown').toString()
    }
  },
  watch: {
    currentProjectId(val, oldVal) {
      if (val !== oldVal) {
        this.loadPlans()
      }
    },
    themeMode() {
      if (this.chart && this.lastXData.length) {
        this.setOptions(this.lastXData, this.lastSeries)
      }
    }
  },
  mounted() {
    this.loadPlans()
  },
  beforeDestroy() {
    this.clearChart()
  },
  methods: {
    loadPlans() {
      if (!this.currentProjectId) {
        this.planList = []
        this.planId = null
        this.clearChart()
        return
      }
      this.loading = true
      statisticPlanList(this.currentProjectId).then(res => {
        this.planList = res.data || []
        if (this.planList.length) {
          this.planId = String(this.planList[0].planId)
          this.handlePlanChange()
        } else {
          this.clearChart()
        }
      }).catch(() => {
        this.planList = []
        this.clearChart()
      }).finally(() => {
        this.loading = false
      })
    },
    clearChart() {
      disconnectEchartsResize(this.chart)
      if (this.chart) {
        this.chart.dispose()
        this.chart = null
      }
      this.lastXData = []
      this.lastSeries = []
    },
    ensureChart() {
      const el = this.$refs.chart
      if (!el) return false
      if (!this.chart) {
        this.chart = echarts.init(el, 'macarons')
      }
      return true
    },
    handlePlanChange() {
      if (!this.planId) return
      this.loading = true
      statisticPlanBurndown(this.planId).then(res => {
        const xData = []
        const series = []
        ;(res.data || []).forEach(p => {
          xData.push(p.key)
          series.push(Number(p.value) || 0)
        })
        this.lastXData = xData
        this.lastSeries = series
        afterChartRefReady(this, () => {
          if (!this.ensureChart()) return
          this.setOptions(xData, series)
        })
      }).catch(() => {
        this.lastXData = []
        this.lastSeries = []
        afterChartRefReady(this, () => {
          if (this.ensureChart()) {
            this.setOptions([], [])
          }
        })
      }).finally(() => {
        this.loading = false
      })
    },
    setOptions(xData, series) {
      if (!this.chart) return
      const dark = this.isDarkTheme
      const colors = statisticChartColors(dark)
      this.chart.setOption({
        tooltip: {
          trigger: 'axis',
          confine: true,
          backgroundColor: colors.tooltipBg,
          borderColor: colors.tooltipBorder,
          textStyle: { color: colors.tooltipText, fontSize: 11 }
        },
        grid: {
          top: 0,
          left: 0,
          right: 4,
          bottom: 0,
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: xData,
          axisLabel: {
            color: colors.axisLabel,
            fontSize: 8,
            margin: 2,
            interval: 'auto'
          },
          axisLine: { lineStyle: { color: colors.axisLine } },
          axisTick: { lineStyle: { color: colors.axisLine } }
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            color: colors.axisLabel,
            fontSize: 8,
            margin: 2,
            width: 28,
            showMaxLabel: false
          },
          axisLine: { lineStyle: { color: colors.axisLine } },
          splitLine: { lineStyle: { color: colors.splitLine } }
        },
        series: [{
          data: series,
          type: 'bar',
          barMaxWidth: 14,
          itemStyle: { color: '#409eff' },
          showBackground: true,
          backgroundStyle: { color: colors.barBg }
        }]
      }, true)
      observeEchartsResize(this.chart, this.$refs.chart)
      this.$nextTick(() => this.chart && this.chart.resize())
    },
    resize() {
      this.chart && this.chart.resize()
    },
    refreshData() {
      if (this.planId) {
        this.handlePlanChange()
      } else {
        this.loadPlans()
      }
    },
    toolsHandle(e, tool) {
      this.$emit('tools-click', tool)
    }
  }
}
</script>

<style lang="scss" scoped>
.plan-burndown-card {
  /* 与其它统计块标题行统一：--statistic-card-header-min-height */
  ::v-deep .statistic-box-header {
    min-height: var(--statistic-card-header-min-height, 17px);
    align-items: center;
  }

  ::v-deep .statistic-box-header .cat2-bug-title {
    display: flex;
    align-items: center;
    line-height: var(--statistic-card-header-min-height, 17px);
  }

  ::v-deep .statistic-box-header .statistic-box-tools {
    gap: 5px;
    align-items: center;
  }

  /* 与「我的参与」一致：内容区贴齐标题分割线下方 */
  ::v-deep .statistic-box-body {
    padding: 0;
    overflow: hidden;
    align-items: flex-start;
    justify-content: flex-start;
  }
}

.plan-burndown-select {
  max-width: 140px;
  vertical-align: middle;

  ::v-deep .el-input {
    height: var(--statistic-card-header-min-height, 17px);
  }

  ::v-deep .el-input__inner {
    height: calc(var(--statistic-card-header-min-height, 17px) - 2px);
    line-height: calc(var(--statistic-card-header-min-height, 17px) - 2px);
    font-size: 11px;
    padding: 0 22px 0 6px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  ::v-deep .el-input__suffix {
    display: flex;
    align-items: center;
    height: 100%;
    right: 2px;
  }

  ::v-deep .el-input__icon {
    line-height: 1;
    font-size: 12px;
  }
}

.plan-burndown-body {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  align-content: flex-start;
}

.plan-burndown-canvas {
  flex: 1 1 auto;
  width: 100%;
  min-height: 0;
}

.plan-burndown-empty {
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

<!-- 下拉挂载在 body，需非 scoped -->
<style lang="scss">
.el-select-dropdown.plan-burndown-select-dropdown {
  max-width: 200px;

  .el-select-dropdown__item {
    box-sizing: border-box;
    height: auto !important;
    min-height: 0 !important;
    max-height: 60px;
    padding: 6px 12px;
    overflow: hidden;
    line-height: normal;
    white-space: normal;
  }

  .plan-burndown-option-text {
    display: -webkit-box;
    max-height: 48px;
    overflow: hidden;
    font-size: 12px;
    line-height: 16px;
    word-break: break-all;
    white-space: normal;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
    text-overflow: ellipsis;
  }

  .el-select-dropdown__item.selected .plan-burndown-option-text {
    font-weight: 600;
  }
}
</style>
