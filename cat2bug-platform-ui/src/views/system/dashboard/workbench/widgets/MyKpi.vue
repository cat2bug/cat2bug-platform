<template>
  <div class="my-kpi" v-loading="loading">
    <button
      v-for="item in cards"
      :key="item.key"
      type="button"
      class="my-kpi__tile"
      :class="['is-' + item.key, item.emphasis ? 'is-emphasis' : 'is-quiet']"
      @click="go(item)"
    >
      <div class="my-kpi__meta">
        <div class="my-kpi__title">
          <span class="my-kpi__icon" aria-hidden="true">
            <el-icon :size="16"><component :is="item.icon" /></el-icon>
          </span>
          <span class="my-kpi__label">{{ item.label }}</span>
        </div>
        <span class="my-kpi__sub">{{ item.sub }}</span>
      </div>
      <div class="my-kpi__aside">
        <span class="my-kpi__num">{{ item.value }}</span>
        <span v-if="item.progress != null" class="my-kpi__pct">{{ item.progress }}%</span>
      </div>
      <span v-if="item.progress != null" class="my-kpi__rail" aria-hidden="true">
        <i :style="{ width: item.progress + '%' }" />
      </span>
    </button>
  </div>
</template>

<script>
import { AlarmClock, CircleCheck, Finished, DataLine } from '@element-plus/icons-vue'
import { statisticMyOpenWorkload } from '@/api/system/statistic/defect'
import { listDefect } from '@/api/system/defect'
import request from '@/utils/request'
import { getCurrentProjectId, getCurrentUserId } from '@/utils/store-context'
import { buildProjectModulePath } from '@/utils/project-module-path'

export default {
  name: 'MyKpi',
  components: { AlarmClock, CircleCheck, Finished, DataLine },
  data() {
    return {
      loading: false,
      pending: 0,
      audit: 0,
      todayHandled: 0,
      planDone: 0,
      planTotal: 0
    }
  },
  computed: {
    cards() {
      const planVal = this.planTotal > 0 ? `${this.planDone}/${this.planTotal}` : String(this.planDone)
      const progress = this.planTotal > 0
        ? Math.min(100, Math.round((this.planDone / this.planTotal) * 100))
        : null
      return [
        {
          key: 'pending',
          value: this.pending,
          label: this.$t('workbench.kpi.pending'),
          sub: this.$t('workbench.kpi.pending-sub'),
          icon: 'AlarmClock',
          emphasis: true,
          query: { defectState: 0 }
        },
        {
          key: 'audit',
          value: this.audit,
          label: this.$t('workbench.kpi.audit'),
          sub: this.$t('workbench.kpi.audit-sub'),
          icon: 'CircleCheck',
          emphasis: true,
          query: { defectState: 1 }
        },
        {
          key: 'today',
          value: this.todayHandled,
          label: this.$t('workbench.kpi.today-handled'),
          sub: this.$t('workbench.kpi.today-handled-sub'),
          icon: 'Finished',
          emphasis: true,
          query: { defectState: 2 }
        },
        {
          key: 'plan',
          value: planVal,
          label: this.$t('workbench.kpi.plan-progress'),
          sub: this.$t('workbench.kpi.plan-progress-sub'),
          icon: 'DataLine',
          emphasis: true,
          progress,
          module: 'plan'
        }
      ]
    }
  },
  mounted() {
    this.load()
  },
  methods: {
    go(item) {
      const module = item.module || 'defect'
      const path = buildProjectModulePath(
        this.$route.params.teamKey,
        this.$route.params.projectKey,
        module
      )
      if (!path) return
      this.$router.push(item.query ? { path, query: item.query } : path)
    },
    async load() {
      const projectId = getCurrentProjectId()
      const userId = getCurrentUserId()
      if (!projectId || !userId) return
      this.loading = true
      try {
        const res = await statisticMyOpenWorkload(projectId)
        const data = res.data || res || {}
        this.pending = Number(data.processing || 0)
        this.audit = Number(data.audit || 0)
        const closed = await listDefect({
          projectId, pageNum: 1, pageSize: 1, handleBy: [userId],
          params: { defectStates: [2], delFlag: '0' }
        })
        const n = Number(closed.total || 0)
        this.todayHandled = n > 99 ? '99+' : n
        try {
          const countPlan = (planItemState) => request({
            url: '/system/PlanItem/list', method: 'get',
            params: { projectId, pageNum: 1, pageSize: 1, planItemState },
            silentError: true
          })
          const [unexec, passed, fail] = await Promise.all([
            countPlan('unexecuted'), countPlan('pass'), countPlan('not_pass')
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
      } catch (e) {
        this.pending = 0
        this.audit = 0
        this.todayHandled = 0
        this.planDone = 0
        this.planTotal = 0
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
.my-kpi {
  display: grid;
  grid-template-columns: 1.15fr 1.15fr 1fr 1fr;
  gap: 12px;
  width: 100%;
  min-height: 0;
}

.my-kpi__tile {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  min-height: 112px;
  padding: 16px 18px;
  margin: 0;
  border-radius: 10px;
  border: 1px solid transparent;
  text-align: left;
  cursor: pointer;
  overflow: hidden;
  box-sizing: border-box;
  transition: transform 0.15s ease, border-color 0.15s ease, filter 0.15s ease;
}
.my-kpi__tile:hover {
  transform: translateY(-2px);
  filter: brightness(1.05);
}
.my-kpi__tile:focus-visible {
  outline: 2px solid var(--cat2bug-primary, #1890ff);
  outline-offset: 2px;
}

.my-kpi__tile.is-emphasis {
  color: #fff;
}
.my-kpi__tile.is-pending {
  background: linear-gradient(145deg, #e06b5c 0%, #c44a3c 100%);
  border-color: rgba(255, 170, 160, 0.35);
  box-shadow: 0 10px 24px rgba(196, 74, 60, 0.28);
}
.my-kpi__tile.is-audit {
  background: linear-gradient(145deg, #2f7fe0 0%, #1a5fb4 100%);
  border-color: rgba(120, 180, 255, 0.35);
  box-shadow: 0 10px 24px rgba(26, 95, 180, 0.28);
}
.my-kpi__tile.is-today {
  background: linear-gradient(145deg, #5daf45 0%, #3f8f2e 100%);
  border-color: rgba(160, 220, 140, 0.35);
  box-shadow: 0 10px 24px rgba(63, 143, 46, 0.28);
}
.my-kpi__tile.is-plan {
  background: linear-gradient(145deg, #2bb8b0 0%, #1a8f89 100%);
  border-color: rgba(140, 230, 220, 0.35);
  box-shadow: 0 10px 24px rgba(26, 143, 137, 0.28);
}
.my-kpi__tile.is-emphasis .my-kpi__label {
  color: rgba(255, 255, 255, 0.95);
}
.my-kpi__tile.is-emphasis .my-kpi__sub {
  color: rgba(255, 255, 255, 0.72);
}
.my-kpi__tile.is-emphasis .my-kpi__num {
  color: #fff;
}
.my-kpi__tile.is-emphasis .my-kpi__pct {
  color: rgba(255, 255, 255, 0.78);
}

.my-kpi__meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-bottom: 2px;
}
.my-kpi__title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.my-kpi__icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
}
.my-kpi__label {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.25;
  color: var(--text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.my-kpi__sub {
  font-size: 12px;
  line-height: 1.35;
  color: var(--text-color-secondary);
}

.my-kpi__aside {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}
.my-kpi__num {
  font-size: 40px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.04em;
  font-variant-numeric: tabular-nums;
}
.my-kpi__pct {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-color-secondary);
}

.my-kpi__rail {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 4px;
  background: rgba(255, 255, 255, 0.18);
}
.my-kpi__rail > i {
  display: block;
  height: 100%;
  background: rgba(255, 255, 255, 0.92);
}

@media (max-width: 1100px) {
  .my-kpi {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 640px) {
  .my-kpi {
    grid-template-columns: 1fr;
  }
  .my-kpi__num {
    font-size: 34px;
  }
}
</style>
