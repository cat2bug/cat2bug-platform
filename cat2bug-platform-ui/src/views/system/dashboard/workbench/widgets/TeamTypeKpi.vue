<template>
  <div class="type-kpi" v-loading="loading">
    <button
      v-for="item in cards"
      :key="item.key"
      type="button"
      class="type-kpi__tile"
      :class="'is-' + item.key.toLowerCase()"
      @click="goList(item.key)"
    >
      <div class="type-kpi__top">
        <span class="type-kpi__name">{{ item.label }}</span>
        <span class="type-kpi__total">{{ item.total }}</span>
      </div>
      <div class="type-kpi__grid">
        <div v-for="m in item.metrics" :key="m.key" class="type-kpi__cell">
          <span class="type-kpi__cell-n">{{ m.value }}</span>
          <span class="type-kpi__cell-l">{{ m.label }}</span>
        </div>
      </div>
    </button>
  </div>
</template>

<script>
import { statisticDefectType } from '@/api/system/statistic/defect'
import { listDefect } from '@/api/system/defect'
import { getCurrentProjectId } from '@/utils/store-context'
import { buildProjectModulePath } from '@/utils/project-module-path'

const TYPE_ORDER = ['TASK', 'DEMAND', 'BUG']

function emptyBag() {
  return { TASK: 0, DEMAND: 0, BUG: 0 }
}

export default {
  name: 'TeamTypeKpi',
  data() {
    return {
      loading: false,
      totals: emptyBag(),
      processing: emptyBag(),
      audit: emptyBag(),
      open: emptyBag(),
      closed: emptyBag()
    }
  },
  computed: {
    cards() {
      return TYPE_ORDER.map(key => ({
        key,
        label: this.$t(key),
        total: this.totals[key] || 0,
        metrics: [
          {
            key: 'processing',
            label: this.$t('workbench.kpi.type-processing'),
            value: this.processing[key] || 0
          },
          {
            key: 'audit',
            label: this.$t('workbench.kpi.type-audit'),
            value: this.audit[key] || 0
          },
          {
            key: 'open',
            label: this.$t('workbench.kpi.type-open'),
            value: this.open[key] || 0
          },
          {
            key: 'closed',
            label: this.$t('workbench.kpi.type-closed'),
            value: this.closed[key] || 0
          }
        ]
      }))
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    countBy(projectId, defectType, defectStates) {
      return listDefect({
        projectId,
        pageNum: 1,
        pageSize: 1,
        defectType,
        params: { defectStates, delFlag: '0' }
      }).then(r => Number(r.total || 0)).catch(() => 0)
    },
    async load() {
      const projectId = getCurrentProjectId()
      if (!projectId) return
      this.loading = true
      try {
        const res = await statisticDefectType(projectId)
        const rows = res.data || res || []
        const list = Array.isArray(rows) ? rows : []
        const nextTotals = emptyBag()
        for (const row of list) {
          const k = String(row.k || row.key || '').toUpperCase()
          if (Object.prototype.hasOwnProperty.call(nextTotals, k)) {
            nextTotals[k] = Number(row.v ?? row.value ?? row.a ?? 0)
          }
        }
        this.totals = nextTotals

        const nextProcessing = emptyBag()
        const nextAudit = emptyBag()
        const nextOpen = emptyBag()
        const nextClosed = emptyBag()
        await Promise.all(TYPE_ORDER.map(async (type) => {
          const [processing, audit, open, closed] = await Promise.all([
            this.countBy(projectId, type, [0]),
            this.countBy(projectId, type, [1]),
            this.countBy(projectId, type, [0, 1, 3]),
            this.countBy(projectId, type, [2])
          ])
          nextProcessing[type] = processing
          nextAudit[type] = audit
          nextOpen[type] = open
          nextClosed[type] = closed
        }))
        this.processing = nextProcessing
        this.audit = nextAudit
        this.open = nextOpen
        this.closed = nextClosed
      } catch (e) {
        this.totals = emptyBag()
        this.processing = emptyBag()
        this.audit = emptyBag()
        this.open = emptyBag()
        this.closed = emptyBag()
      } finally {
        this.loading = false
      }
    },
    goList(defectType) {
      const path = buildProjectModulePath(
        this.$route.params.teamKey,
        this.$route.params.projectKey,
        'defect'
      )
      if (!path) return
      this.$router.push({ path, query: { defectType } })
    },
    refresh() {
      return this.load()
    }
  }
}
</script>

<style scoped lang="scss">
.type-kpi {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
}

.type-kpi__tile {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 14px;
  min-width: 0;
  min-height: 0;
  height: 100%;
  margin: 0;
  padding: 16px;
  text-align: left;
  cursor: pointer;
  color: #fff;
  border: 1px solid transparent;
  border-radius: 10px;
  box-sizing: border-box;
  overflow: hidden;
  transition: transform 0.15s ease, filter 0.15s ease;
}
.type-kpi__tile:hover {
  transform: translateY(-2px);
  filter: brightness(1.05);
}
.type-kpi__tile:focus-visible {
  outline: 2px solid #fff;
  outline-offset: 2px;
}

.type-kpi__tile.is-task {
  background: linear-gradient(145deg, #2f7fe0 0%, #1a5fb4 100%);
  border-color: rgba(120, 180, 255, 0.35);
  box-shadow: 0 10px 24px rgba(26, 95, 180, 0.28);
}
.type-kpi__tile.is-demand {
  background: linear-gradient(145deg, #8b6ad8 0%, #5b3fa6 100%);
  border-color: rgba(190, 160, 255, 0.35);
  box-shadow: 0 10px 24px rgba(91, 63, 166, 0.28);
}
.type-kpi__tile.is-bug {
  background: linear-gradient(145deg, #e06b5c 0%, #c44a3c 100%);
  border-color: rgba(255, 170, 160, 0.35);
  box-shadow: 0 10px 24px rgba(196, 74, 60, 0.28);
}

.type-kpi__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.type-kpi__name {
  padding-top: 6px;
  font-size: 14px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.92);
}
.type-kpi__total {
  font-size: 40px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.04em;
  font-variant-numeric: tabular-nums;
  color: #fff;
}

.type-kpi__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.type-kpi__cell {
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.16);
  box-sizing: border-box;
}
.type-kpi__cell-n {
  display: block;
  font-size: 18px;
  font-weight: 750;
  line-height: 1.15;
  font-variant-numeric: tabular-nums;
  color: #fff;
}
.type-kpi__cell-l {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.75);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 900px) {
  .type-kpi { grid-template-columns: 1fr; }
  .type-kpi__tile { min-height: 168px; }
  .type-kpi__total { font-size: 34px; }
}
</style>
