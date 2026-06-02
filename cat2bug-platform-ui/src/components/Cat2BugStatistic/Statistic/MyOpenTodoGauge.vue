<template>
  <cat2-bug-card
    :title="$i18n.t('defect.my-open-todo').toString()"
    v-loading="loading"
    :tools="tools"
    @tools-click="toolsHandle"
  >
    <template slot="content">
      <div class="statistic-echarts-body" @click="clickHandle">
        <div v-if="isEmpty" class="statistic-echarts-zero">0</div>
        <template v-else>
          <div class="statistic-echarts-chart-wrap">
            <div ref="chart" class="statistic-echarts-canvas" />
          </div>
          <ul class="statistic-echarts-legend">
            <li v-for="item in legendItems" :key="item.key">
              <span class="statistic-echarts-legend-dot" :style="{ backgroundColor: item.color }" />
              <span class="statistic-echarts-legend-name">{{ item.name }}</span>
              <span class="statistic-echarts-legend-value">{{ item.value }}</span>
            </li>
          </ul>
        </template>
      </div>
    </template>
  </cat2-bug-card>
</template>

<script>
import * as echarts from '@/assets/js/echarts.min.js'
import resize from '@/views/dashboard/mixins/resize'
import Cat2BugCard from '../Components/Card'
import { statisticMyOpenWorkload } from '@/api/system/statistic/defect'

const OPEN_DEFECT_STATE_IDS = [0, 1, 3]

export default {
  name: 'MyOpenTodoGauge',
  components: { Cat2BugCard },
  mixins: [resize],
  data() {
    return {
      loading: false,
      chart: null,
      workload: {
        total: 0,
        processing: 0,
        audit: 0,
        rejected: 0
      }
    }
  },
  props: {
    params: { type: Object, default: () => ({}) },
    tools: { type: Array, default: () => [] },
    parent: { type: Object, default: () => ({}) },
    read: { type: Boolean, default: false }
  },
  computed: {
    currentProjectId() {
      return parseInt(this.$store.state.user.config.currentProjectId)
    },
    currentUserId() {
      return parseInt(this.$store.state.user.id)
    },
    isEmpty() {
      return (this.workload.total || 0) <= 0
    },
    chartTextColor() {
      const style = getComputedStyle(document.documentElement)
      return (style.getPropertyValue('--text-color-secondary') || '').trim() || '#a8abb2'
    },
    chartCenterColor() {
      const style = getComputedStyle(document.documentElement)
      return (style.getPropertyValue('--text-color-primary') || '').trim() || '#303133'
    },
    legendItems() {
      return [
        { key: 'processing', name: this.$t('PROCESSING').toString(), value: this.workload.processing || 0, color: '#F56C6C' },
        { key: 'audit', name: this.$t('AUDIT').toString(), value: this.workload.audit || 0, color: '#FBB13F' },
        { key: 'rejected', name: this.$t('REJECTED').toString(), value: this.workload.rejected || 0, color: '#909399' }
      ]
    }
  },
  watch: {
    '$i18n.locale'() {
      if (this.chart && !this.isEmpty) {
        this.initChart()
      }
    }
  },
  mounted() {
    this.getStatistic()
  },
  beforeDestroy() {
    if (this.chart) {
      this.chart.dispose()
      this.chart = null
    }
  },
  methods: {
    clickHandle() {
      if (this.read) return
      this.parent.searchQuery({
        common: {
          handleBy: [this.currentUserId],
          params: { defectStates: OPEN_DEFECT_STATE_IDS }
        }
      })
    },
    getStatistic() {
      this.loading = true
      statisticMyOpenWorkload(this.currentProjectId).then(res => {
        const data = res.data || {}
        this.workload = {
          total: data.total || 0,
          processing: data.processing || 0,
          audit: data.audit || 0,
          rejected: data.rejected || 0
        }
        this.$nextTick(() => {
          if (!this.isEmpty) {
            this.initChart()
          } else if (this.chart) {
            this.chart.dispose()
            this.chart = null
          }
        })
      }).finally(() => {
        this.loading = false
      })
    },
    pieSeriesData() {
      return [
        { value: this.workload.processing, name: this.$t('PROCESSING').toString(), itemStyle: { color: '#F56C6C' } },
        { value: this.workload.audit, name: this.$t('AUDIT').toString(), itemStyle: { color: '#FBB13F' } },
        { value: this.workload.rejected, name: this.$t('REJECTED').toString(), itemStyle: { color: '#909399' } }
      ].filter(item => item.value > 0)
    },
    initChart() {
      const el = this.$refs.chart
      if (!el) return
      if (!this.chart) {
        this.chart = echarts.init(el)
      }
      const seriesData = this.pieSeriesData()
      this.chart.setOption({
        color: ['#F56C6C', '#FBB13F', '#909399'],
        tooltip: {
          trigger: 'item',
          confine: true,
          formatter: '{b}: {c}'
        },
        series: [{
          type: 'pie',
          radius: ['54%', '84%'],
          center: ['50%', '50%'],
          silent: false,
          label: { show: false },
          labelLine: { show: false },
          data: seriesData
        }],
        graphic: [
          {
            type: 'group',
            left: 'center',
            top: 'center',
            children: [{
              type: 'text',
              left: 'center',
              top: 'middle',
              style: {
                text: String(this.workload.total),
                fill: this.chartCenterColor,
                fontSize: 24,
                fontWeight: 600,
                textAlign: 'center',
                textVerticalAlign: 'middle'
              }
            }]
          }
        ]
      }, true)
      this.$nextTick(() => this.chart && this.chart.resize())
    },
    resize() {
      this.chart && this.chart.resize()
    },
    refreshData() {
      this.getStatistic()
    },
    toolsHandle(e, tool) {
      this.$emit('tools-click', tool)
    }
  }
}
</script>

<style lang="scss" scoped>
.statistic-echarts-body {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
}

.statistic-echarts-chart-wrap {
  flex: 0 0 86px;
  width: 86px;
  height: 86px;
}

.statistic-echarts-canvas {
  width: 100%;
  height: 100%;
}

.statistic-echarts-legend {
  flex: 0 0 auto;
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 10px;
  line-height: 1.2;
  color: var(--text-color-secondary, #909399);

  li {
    display: grid;
    grid-template-columns: 6px auto auto;
    align-items: center;
    column-gap: 4px;
    white-space: nowrap;
  }
}

.statistic-echarts-legend-dot {
  flex-shrink: 0;
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.statistic-echarts-legend-name {
  min-width: 0;
}

.statistic-echarts-legend-value {
  font-weight: 600;
  color: var(--text-color-primary, #303133);
  text-align: right;
}

.statistic-echarts-zero {
  font-size: 22px;
  font-weight: 600;
  line-height: 1;
  color: var(--text-color-secondary, #909399);
}
</style>
