<template>
  <div class="mix-row" v-loading="loading">
    <div ref="chart" class="mix-chart" />
    <div class="mix-legend">
      <span v-for="item in items" :key="item.key">
        <b>{{ item.value }}</b>{{ item.name }}
      </span>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { statisticMyOpenWorkload, statisticDefectState } from '@/api/system/statistic/defect'
import { listDefect } from '@/api/system/defect'
import { getCurrentProjectId, getCurrentUserId } from '@/utils/store-context'

export default {
  name: 'MyWorkloadMix',
  props: {
    /** me = 个人名下；team = 项目全量 */
    scope: { type: String, default: 'me' }
  },
  data() {
    return {
      loading: false,
      processing: 0,
      audit: 0,
      closed: 0,
      rejected: 0,
      chart: null
    }
  },
  computed: {
    items() {
      return [
        { key: 'p', name: ` ${this.$t('PROCESSING')}`, value: this.processing, color: '#F56C6C' },
        { key: 'a', name: ` ${this.$t('AUDIT')}`, value: this.audit, color: '#FBB13F' },
        { key: 'c', name: ` ${this.$t('CLOSED')}`, value: this.closed, color: '#67C23A' },
        { key: 'r', name: ` ${this.$t('REJECTED')}`, value: this.rejected, color: '#909399' }
      ]
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
      const userId = getCurrentUserId()
      if (!projectId) return
      this.loading = true
      try {
        if (this.scope === 'team') {
          await this.loadTeam(projectId)
        } else {
          await this.loadMe(projectId, userId)
        }
        await this.$nextTick()
        this.renderChart()
        await this.$nextTick()
        this.chart && this.chart.resize()
      } finally {
        this.loading = false
      }
    },
    async loadMe(projectId, userId) {
      const res = await statisticMyOpenWorkload(projectId)
      const data = res.data || res || {}
      this.processing = Number(data.processing || 0)
      this.audit = Number(data.audit || 0)
      this.rejected = Number(data.rejected || 0)
      this.closed = 0
      if (userId) {
        try {
          const closedRes = await listDefect({
            projectId, pageNum: 1, pageSize: 1, handleBy: [userId],
            params: { defectStates: [2], delFlag: '0' }
          })
          this.closed = Number(closedRes.total || 0)
        } catch (e) {
          this.closed = 0
        }
      }
    },
    async loadTeam(projectId) {
      const res = await statisticDefectState(projectId)
      const rows = res.data || res || []
      const list = Array.isArray(rows) ? rows : []
      const pick = (name) => {
        const row = list.find(r => String(r.k || '').toUpperCase() === name)
        return Number(row?.a ?? row?.v ?? 0)
      }
      this.processing = pick('PROCESSING')
      this.audit = pick('AUDIT')
      this.closed = pick('CLOSED')
      this.rejected = pick('REJECTED')
    },
    renderChart() {
      const el = this.$refs.chart
      if (!el) return
      if (!this.chart) this.chart = echarts.init(el)
      const pieData = this.items
        .filter(i => Number(i.value) > 0)
        .map(i => ({ name: i.name.trim(), value: Number(i.value), itemStyle: { color: i.color } }))
      this.chart.setOption({
        tooltip: { trigger: 'item', appendToBody: true },
        series: [{
          type: 'pie',
          radius: ['52%', '78%'],
          center: ['50%', '50%'],
          label: { show: false },
          silent: !pieData.length,
          data: pieData.length
            ? pieData
            : [{ value: 1, name: '-', itemStyle: { color: 'var(--border-color-light, #ebeef5)' }, tooltip: { show: false } }]
        }]
      }, true)
    }
  }
}
</script>

<style scoped lang="scss">
.mix-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 14px 14px;
  flex: 1;
  min-height: 160px;
  box-sizing: border-box;
}
.mix-chart {
  width: 120px;
  height: 120px;
  flex-shrink: 0;
}
.mix-legend {
  flex: 1;
  font-size: 12px;
  color: var(--text-color-secondary);
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}
.mix-legend span b {
  color: var(--text-color-primary);
  font-weight: 600;
  margin-right: 4px;
}
</style>
