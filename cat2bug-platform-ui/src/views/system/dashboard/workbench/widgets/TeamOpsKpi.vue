<template>
  <div class="ops-kpi" v-loading="loading">
    <div
      v-for="item in cards"
      :key="item.key"
      class="ops-kpi__tile"
      :class="'is-' + item.key"
    >
      <div class="ops-kpi__top">
        <span class="ops-kpi__name">{{ item.label }}</span>
        <span class="ops-kpi__total">{{ item.total }}</span>
      </div>
      <div class="ops-kpi__grid" :class="{ 'is-triple': item.metrics.length >= 3 }">
        <div v-for="m in item.metrics" :key="m.key" class="ops-kpi__cell">
          <span class="ops-kpi__cell-n">{{ m.value }}</span>
          <span class="ops-kpi__cell-l">{{ m.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { weekOps } from '@/api/system/dashboard'
import { getCurrentProjectId } from '@/utils/store-context'

function empty() {
  return {
    testerCreated: 0,
    testerClosed: 0,
    developerRepaired: 0,
    outsiderCreated: 0,
    mcpCallCount: 0,
    apiCallCount: 0,
    llmCallCount: 0
  }
}

export default {
  name: 'TeamOpsKpi',
  data() {
    return {
      loading: false,
      stats: empty()
    }
  },
  computed: {
    cards() {
      const s = this.stats
      const callTotal = (s.mcpCallCount || 0) + (s.apiCallCount || 0) + (s.llmCallCount || 0)
      return [
        {
          key: 'tester',
          label: this.$t('workbench.ops.tester'),
          total: s.testerCreated || 0,
          metrics: [
            {
              key: 'created',
              label: this.$t('workbench.ops.created'),
              value: s.testerCreated || 0
            },
            {
              key: 'closed',
              label: this.$t('workbench.ops.closed'),
              value: s.testerClosed || 0
            }
          ]
        },
        {
          key: 'developer',
          label: this.$t('workbench.ops.developer'),
          total: s.developerRepaired || 0,
          metrics: [
            {
              key: 'repaired',
              label: this.$t('workbench.ops.repaired'),
              value: s.developerRepaired || 0
            }
          ]
        },
        {
          key: 'outsider',
          label: this.$t('workbench.ops.outsider'),
          total: s.outsiderCreated || 0,
          metrics: [
            {
              key: 'created',
              label: this.$t('workbench.ops.created'),
              value: s.outsiderCreated || 0
            }
          ]
        },
        {
          key: 'calls',
          label: this.$t('workbench.ops.calls'),
          total: callTotal,
          metrics: [
            {
              key: 'mcp',
              label: this.$t('workbench.ops.mcp'),
              value: s.mcpCallCount || 0
            },
            {
              key: 'api',
              label: this.$t('workbench.ops.api'),
              value: s.apiCallCount || 0
            },
            {
              key: 'llm',
              label: this.$t('workbench.ops.llm'),
              value: s.llmCallCount || 0
            }
          ]
        }
      ]
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    async load() {
      const projectId = getCurrentProjectId()
      if (!projectId) return
      this.loading = true
      try {
        const res = await weekOps(projectId)
        const data = res.data || res || {}
        this.stats = {
          testerCreated: Number(data.testerCreated || 0),
          testerClosed: Number(data.testerClosed || 0),
          developerRepaired: Number(data.developerRepaired || 0),
          outsiderCreated: Number(data.outsiderCreated || 0),
          mcpCallCount: Number(data.mcpCallCount || 0),
          apiCallCount: Number(data.apiCallCount || 0),
          llmCallCount: Number(data.llmCallCount || 0)
        }
      } catch (e) {
        this.stats = empty()
      } finally {
        this.loading = false
      }
    },
    refresh() {
      return this.load()
    }
  }
}
</script>

<style scoped lang="scss">
.ops-kpi {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
}

.ops-kpi__tile {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  min-height: 0;
  height: 100%;
  padding: 14px 16px;
  color: #fff;
  border: 1px solid transparent;
  border-radius: 10px;
  box-sizing: border-box;
  overflow: hidden;
}

.ops-kpi__tile.is-tester {
  background: linear-gradient(145deg, #2bb8a6 0%, #1a8f82 100%);
  border-color: rgba(140, 230, 210, 0.35);
  box-shadow: 0 10px 24px rgba(26, 143, 130, 0.28);
}
.ops-kpi__tile.is-developer {
  background: linear-gradient(145deg, #4a8fe0 0%, #2d6cb5 100%);
  border-color: rgba(150, 200, 255, 0.35);
  box-shadow: 0 10px 24px rgba(45, 108, 181, 0.28);
}
.ops-kpi__tile.is-outsider {
  background: linear-gradient(145deg, #d4a04a 0%, #b07a28 100%);
  border-color: rgba(255, 210, 140, 0.35);
  box-shadow: 0 10px 24px rgba(176, 122, 40, 0.28);
}
.ops-kpi__tile.is-calls {
  background: linear-gradient(145deg, #6b7280 0%, #4b5563 100%);
  border-color: rgba(200, 210, 220, 0.3);
  box-shadow: 0 10px 24px rgba(75, 85, 99, 0.28);
}

.ops-kpi__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.ops-kpi__name {
  padding-top: 4px;
  font-size: 13px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.92);
}
.ops-kpi__total {
  font-size: 32px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.04em;
  font-variant-numeric: tabular-nums;
  color: #fff;
}

.ops-kpi__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.ops-kpi__grid.is-triple {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.ops-kpi__cell {
  padding: 7px 8px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.16);
  box-sizing: border-box;
  min-width: 0;
}
.ops-kpi__cell-n {
  display: block;
  font-size: 16px;
  font-weight: 750;
  line-height: 1.15;
  font-variant-numeric: tabular-nums;
  color: #fff;
}
.ops-kpi__cell-l {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.75);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 1100px) {
  .ops-kpi { grid-template-columns: 1fr 1fr; }
  .ops-kpi__tile { min-height: 120px; }
}
@media (max-width: 640px) {
  .ops-kpi { grid-template-columns: 1fr; }
}
</style>
