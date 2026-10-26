<template>
  <div class="weekly-report is-team" v-loading="loading">
    <div v-if="showRange" class="weekly-report__range">{{ rangeLabel }}</div>
    <div class="weekly-report__donut">
      <div ref="chart" class="weekly-report__chart" />
      <div class="weekly-report__legend">
        <div v-for="item in donutItems" :key="item.key" class="leg">
          <span class="dot" :style="{ background: item.color }" />
          <span class="n">{{ item.display }}</span>
          <span class="l">{{ item.name }}</span>
        </div>
      </div>
    </div>
    <ul class="weekly-report__bullets">
      <li v-for="(line, i) in teamBullets" :key="i">{{ line }}</li>
    </ul>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import {
  statisticDefectState,
  statisticDefectType
} from '@/api/system/statistic/defect'
import request from '@/utils/request'
import { getCurrentProjectId } from '@/utils/store-context'

function startOfWeek(d) {
  const x = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const day = x.getDay()
  const diff = day === 0 ? -6 : 1 - day
  x.setDate(x.getDate() + diff)
  x.setHours(0, 0, 0, 0)
  return x
}

function endOfWeek(d) {
  const s = startOfWeek(d)
  const e = new Date(s)
  e.setDate(s.getDate() + 6)
  e.setHours(23, 59, 59, 999)
  return e
}

function fmt(d) {
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${m}-${day}`
}

/** 饼图扇区实色（ECharts 不认 CSS var） */
const DONUT_COLORS = {
  handled: '#409EFF',
  pending: '#F56C6C',
  audit: '#FBB13F',
  plan: '#67C23A'
}

/** 中心文字等从主题 token 取实色 */
function themeColor(name, fallback) {
  if (typeof window === 'undefined' || !window.getComputedStyle) return fallback
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return v || fallback
}

export default {
  name: 'MyWeeklyReport',
  /** 仅项目周报；保留 scope 以免旧调用方报错 */
  props: {
    scope: { type: String, default: 'team' },
    /** 为 false 时日期由卡片标题栏右上角展示 */
    showRange: { type: Boolean, default: true }
  },
  data() {
    return {
      loading: false,
      handled: 0,
      pending: 0,
      audit: 0,
      planDone: 0,
      planTotal: 0,
      typeTotals: { TASK: 0, DEMAND: 0, BUG: 0 },
      weekStart: startOfWeek(new Date()),
      weekEnd: endOfWeek(new Date()),
      chart: null
    }
  },
  computed: {
    rangeLabel() {
      return this.$t('workbench.weekly.range', [fmt(this.weekStart), fmt(this.weekEnd)])
    },
    planLabel() {
      if (!this.planTotal) return String(this.planDone)
      return `${this.planDone}/${this.planTotal}`
    },
    donutItems() {
      return [
        {
          key: 'handled',
          name: this.$t('workbench.weekly.handled'),
          value: this.handled,
          display: String(this.handled),
          color: DONUT_COLORS.handled
        },
        {
          key: 'pending',
          name: this.$t('workbench.weekly.pending'),
          value: this.pending,
          display: String(this.pending),
          color: DONUT_COLORS.pending
        },
        {
          key: 'audit',
          name: this.$t('workbench.weekly.audit'),
          value: this.audit,
          display: String(this.audit),
          color: DONUT_COLORS.audit
        },
        {
          key: 'plan',
          name: this.$t('workbench.weekly.plan'),
          value: this.planTotal > 0 ? this.planDone : 0,
          display: this.planLabel,
          color: DONUT_COLORS.plan
        }
      ]
    },
    teamBullets() {
      return [
        this.$t('workbench.weekly.team-b2', [this.pending, this.audit]),
        this.$t('workbench.weekly.team-b3', [this.handled, this.planDone, this.planTotal])
      ]
    }
  },
  mounted() {
    this.load()
    window.addEventListener('resize', this.onResize)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.onResize)
    this.disposeChart()
  },
  methods: {
    onResize() {
      this.chart && this.chart.resize()
    },
    disposeChart() {
      if (this.chart) {
        this.chart.dispose()
        this.chart = null
      }
    },
    async load() {
      const projectId = getCurrentProjectId()
      if (!projectId) return
      this.weekStart = startOfWeek(new Date())
      this.weekEnd = endOfWeek(new Date())
      this.loading = true
      try {
        await Promise.all([
          this.loadTeam(projectId),
          this.loadPlan(projectId)
        ])
        await this.$nextTick()
        this.renderDonut()
        await this.$nextTick()
        this.chart && this.chart.resize()
      } catch (e) {
        this.handled = 0
        this.pending = 0
        this.audit = 0
      } finally {
        this.loading = false
      }
    },
    async loadTeam(projectId) {
      const [stateRes, typeRes] = await Promise.all([
        statisticDefectState(projectId),
        statisticDefectType(projectId)
      ])
      const states = Array.isArray(stateRes.data || stateRes) ? (stateRes.data || stateRes) : []
      const pick = (name) => {
        const row = states.find(r => String(r.k || '').toUpperCase() === name)
        return {
          total: Number(row?.a ?? 0),
          week: Number(row?.w ?? 0)
        }
      }
      const processing = pick('PROCESSING')
      const audit = pick('AUDIT')
      const closed = pick('CLOSED')
      const rejected = pick('REJECTED')
      this.pending = processing.total
      this.audit = audit.total
      this.handled = processing.week + audit.week + closed.week + rejected.week

      const types = Array.isArray(typeRes.data || typeRes) ? (typeRes.data || typeRes) : []
      const next = { TASK: 0, DEMAND: 0, BUG: 0 }
      for (const row of types) {
        const k = String(row.k || '').toUpperCase()
        if (Object.prototype.hasOwnProperty.call(next, k)) {
          next[k] = Number(row.v ?? row.a ?? 0)
        }
      }
      this.typeTotals = next
    },
    async loadPlan(projectId) {
      try {
        const countPlan = (planItemState) => request({
          url: '/system/PlanItem/list',
          method: 'get',
          params: { projectId, pageNum: 1, pageSize: 1, planItemState },
          silentError: true
        })
        const [unexec, passed, fail] = await Promise.all([
          countPlan('unexecuted'),
          countPlan('pass'),
          countPlan('not_pass')
        ])
        const u = Number(unexec.total || 0)
        const p = Number(passed.total || 0)
        const f = Number(fail.total || 0)
        this.planDone = p
        this.planTotal = u + p + f
      } catch (e) {
        this.planDone = 0
        this.planTotal = 0
      }
    },
    renderDonut() {
      const el = this.$refs.chart
      if (!el) return
      if (!this.chart) this.chart = echarts.init(el)
      const pieData = this.donutItems
        .filter(i => Number(i.value) > 0)
        .map(i => ({
          name: i.name,
          value: Number(i.value),
          itemStyle: { color: i.color }
        }))
      const centerLabel = this.planTotal
        ? `${this.planDone}/${this.planTotal}`
        : String(this.handled)
      const centerSub = this.planTotal
        ? this.$t('workbench.weekly.plan')
        : this.$t('workbench.weekly.handled')
      this.chart.setOption({
        tooltip: { trigger: 'item', appendToBody: true },
        series: [{
          type: 'pie',
          radius: ['48%', '72%'],
          center: ['50%', '50%'],
          label: { show: false },
          silent: !pieData.length,
          data: pieData.length
            ? pieData
            : [{
                value: 1,
                name: '-',
                itemStyle: { color: themeColor('--border-color-light', '#ebeef5') },
                tooltip: { show: false }
              }]
        }],
        graphic: [
          {
            type: 'text',
            left: 'center',
            top: '38%',
            style: {
              text: centerLabel,
              textAlign: 'center',
              fill: themeColor('--text-color-primary', '#303133'),
              fontSize: 16,
              fontWeight: 600
            }
          },
          {
            type: 'text',
            left: 'center',
            top: '54%',
            style: {
              text: centerSub,
              textAlign: 'center',
              fill: themeColor('--text-color-secondary', '#909399'),
              fontSize: 11
            }
          }
        ]
      }, true)
    },
    refresh() {
      return this.load()
    }
  }
}
</script>

<style scoped lang="scss">
.weekly-report {
  padding: 12px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  min-height: 0;
  height: 100%;
  box-sizing: border-box;
}

.weekly-report__range {
  font-size: 12px;
  color: var(--text-color-secondary);
}
.weekly-report__donut {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 120px;
}
.weekly-report__chart {
  width: 118px;
  height: 118px;
  flex-shrink: 0;
}
.weekly-report__legend {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.weekly-report__legend .leg {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-size: 12px;
  color: var(--text-color-secondary);
  min-width: 0;
}
.weekly-report__legend .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  align-self: center;
}
.weekly-report__legend .n {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color-primary);
  flex-shrink: 0;
}
.weekly-report__legend .l {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.weekly-report__bullets {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: var(--text-color-regular);
  line-height: 1.55;
}

@media (max-width: 640px) {
  .weekly-report__donut { flex-direction: column; align-items: stretch; }
  .weekly-report__chart { margin: 0 auto; }
}
</style>
