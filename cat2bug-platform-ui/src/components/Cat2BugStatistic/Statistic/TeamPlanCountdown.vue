<template>
  <cat2-bug-card
    class="team-plan-countdown-card"
    :title="cardTitle"
    v-loading="loading"
    :tools="tools"
    @tools-click="toolsHandle"
  >
    <template slot="left-tools">
      <el-select
        v-if="planList.length"
        v-model="planId"
        class="plan-countdown-select"
        popper-class="plan-countdown-select-dropdown"
        size="mini"
        filterable
        @change="onPlanChange"
        @click.native.stop
      >
        <el-option
          v-for="item in planList"
          :key="String(item.planId)"
          :label="item.planName || String(item.planId)"
          :value="String(item.planId)"
          :title="item.planName || String(item.planId)"
        >
          <span class="plan-countdown-option-text">{{ item.planName || item.planId }}</span>
        </el-option>
      </el-select>
    </template>
    <template slot="content">
      <div
        class="plan-countdown-body"
        :class="{ 'has-data': summary && !showEmptyHint }"
        @click.stop
      >
        <div v-if="showEmptyHint" class="plan-countdown-empty">{{ emptyHintText }}</div>
        <template v-else-if="summary">
          <div class="plan-countdown-main" :class="{ 'is-overdue': dayState === 'overdue' }">
            <template v-if="!summary.planEndTime">
              <span class="plan-countdown-no-end">{{ $t('defect.team-plan-countdown.no-end') }}</span>
            </template>
            <template v-else-if="dayState === 'today'">
              <span class="plan-countdown-label">{{ $t('defect.team-plan-countdown.remain') }}</span>
              <span class="statistic-countdown-nixie">0</span>
              <span class="plan-countdown-unit">{{ $t('defect.team-plan-countdown.days') }}</span>
            </template>
            <template v-else-if="dayState === 'overdue'">
              <span class="plan-countdown-label">{{ $t('defect.team-plan-countdown.overdue') }}</span>
              <span class="statistic-countdown-nixie statistic-countdown-nixie--overdue">{{ overdueDays }}</span>
              <span class="plan-countdown-unit">{{ $t('defect.team-plan-countdown.days') }}</span>
            </template>
            <template v-else>
              <span class="plan-countdown-label">{{ $t('defect.team-plan-countdown.remain') }}</span>
              <span class="statistic-countdown-nixie statistic-countdown-nixie--remain">{{ remainDays }}</span>
              <span class="plan-countdown-unit">{{ $t('defect.team-plan-countdown.days') }}</span>
            </template>
          </div>
          <div class="plan-countdown-stats">
            <el-tooltip effect="dark" :content="caseTooltip" placement="top">
              <div class="plan-stat-line plan-stat-line--case">
                <svg-icon class="plan-stat-icon" icon-class="case" />
                <span class="plan-stat-text">
                  <span class="plan-stat-kind">{{ $t('defect.team-plan-countdown.case-short') }}</span>
                  <span class="plan-stat-ratio">{{ caseRatioText }}</span>
                </span>
              </div>
            </el-tooltip>
            <el-tooltip effect="dark" :content="defectTooltip" placement="top">
              <div
                class="plan-stat-line plan-stat-line--defect"
                :class="{ 'is-all-closed': defectAllClosed }"
              >
                <svg-icon class="plan-stat-icon" icon-class="bug" />
                <span class="plan-stat-text">
                  <span class="plan-stat-kind">{{ $t('defect.team-plan-countdown.defect-short') }}</span>
                  <span class="plan-stat-ratio">{{ defectRatioText }}</span>
                </span>
              </div>
            </el-tooltip>
          </div>
        </template>
      </div>
    </template>
  </cat2-bug-card>
</template>

<script>
import Cat2BugCard from '../Components/Card'
import { statisticPlanCountdownSummary, statisticPlanList } from '@/api/system/statistic/defect'
import { resolveCurrentProjectId } from '../utils/project-id'
import { calendarDaysUntil } from '../utils/remind-timer'

export default {
  name: 'TeamPlanCountdown',
  components: { Cat2BugCard },
  props: {
    params: { type: Object, default: () => ({}) },
    tools: { type: Array, default: () => [] },
    parent: { type: Object, default: () => ({}) },
    read: { type: Boolean, default: false }
  },
  data() {
    return {
      loading: false,
      planList: [],
      planId: null,
      summary: null
    }
  },
  computed: {
    currentProjectId() {
      return resolveCurrentProjectId(this)
    },
    cardTitle() {
      return this.$t('defect.team-plan-countdown').toString()
    },
    showEmptyHint() {
      return !this.loading && (!this.currentProjectId || !this.planList.length)
    },
    emptyHintText() {
      if (!this.currentProjectId) {
        return this.$t('defect.statistic.no-project').toString()
      }
      return this.$t('defect.team-plan-countdown.empty').toString()
    },
    diffDays() {
      if (!this.summary || !this.summary.planEndTime) {
        return null
      }
      return calendarDaysUntil(new Date(this.summary.planEndTime))
    },
    dayState() {
      const d = this.diffDays
      if (d == null) return 'none'
      if (d < 0) return 'overdue'
      if (d === 0) return 'today'
      return 'remain'
    },
    remainDays() {
      return this.diffDays > 0 ? this.diffDays : 0
    },
    overdueDays() {
      return this.diffDays < 0 ? Math.abs(this.diffDays) : 0
    },
    casePassCount() {
      if (!this.summary) return 0
      return Number(this.summary.passCount) || 0
    },
    caseTotalCount() {
      if (!this.summary) return 0
      const total = Number(this.summary.itemTotal)
      if (total > 0) {
        return total
      }
      const pass = Number(this.summary.passCount) || 0
      const unexec = Number(this.summary.unexecutedCount) || 0
      return pass + unexec
    },
    defectClosedCount() {
      if (!this.summary) return 0
      return Number(this.summary.defectCloseStateCount) || 0
    },
    defectTotalCount() {
      if (!this.summary) return 0
      return Number(this.summary.defectCount) || 0
    },
    caseRatioText() {
      return `${this.casePassCount}/${this.caseTotalCount}`
    },
    defectRatioText() {
      return `${this.defectClosedCount}/${this.defectTotalCount}`
    },
    defectAllClosed() {
      return this.defectTotalCount > 0 && this.defectClosedCount >= this.defectTotalCount
    },
    caseUnexecCount() {
      if (!this.summary) return 0
      const u = Number(this.summary.unexecutedCount)
      if (Number.isFinite(u) && u >= 0) {
        return u
      }
      return Math.max(0, this.caseTotalCount - this.casePassCount)
    },
    defectOpenCount() {
      return Math.max(0, this.defectTotalCount - this.defectClosedCount)
    },
    caseTooltip() {
      return this.$t('defect.team-plan-countdown.case-tooltip', {
        pass: this.casePassCount,
        unexec: this.caseUnexecCount,
        total: this.caseTotalCount
      }).toString()
    },
    defectTooltip() {
      return this.$t('defect.team-plan-countdown.defect-tooltip', {
        closed: this.defectClosedCount,
        open: this.defectOpenCount,
        total: this.defectTotalCount
      }).toString()
    }
  },
  watch: {
    currentProjectId(val, oldVal) {
      if (val !== oldVal) {
        this.loadPlans()
      }
    }
  },
  mounted() {
    this.loadPlans()
  },
  methods: {
    loadPlans() {
      if (!this.currentProjectId) {
        this.planList = []
        this.planId = null
        this.summary = null
        return
      }
      this.loading = true
      statisticPlanList(this.currentProjectId).then(res => {
        this.planList = res.data || []
        if (!this.planList.length) {
          this.planId = null
          this.summary = null
          return
        }
        const saved = this.params && this.params.planId
          ? String(this.params.planId)
          : null
        const exists = saved && this.planList.some(p => String(p.planId) === saved)
        this.planId = exists ? saved : String(this.planList[0].planId)
        this.loadSummary()
      }).catch(() => {
        this.planList = []
        this.summary = null
      }).finally(() => {
        this.loading = false
      })
    },
    loadSummary() {
      if (!this.planId) {
        this.summary = null
        return
      }
      this.loading = true
      statisticPlanCountdownSummary(this.planId).then(res => {
        this.summary = res.data || null
      }).catch(() => {
        this.summary = null
      }).finally(() => {
        this.loading = false
      })
    },
    onPlanChange() {
      if (!this.read && this.parent && typeof this.parent.saveStatisticItemParams === 'function') {
        this.parent.saveStatisticItemParams('TeamPlanCountdown', { planId: this.planId })
      }
      this.loadSummary()
    },
    refreshData() {
      if (this.planId) {
        this.loadSummary()
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
.team-plan-countdown-card {
  width: max-content;
  max-width: 100%;
  align-self: flex-start;

  ::v-deep .statistic-box-header {
    width: max-content;
    max-width: 100%;
    min-height: var(--statistic-card-header-min-height, 17px);
    align-items: center;
    flex-wrap: nowrap;
  }

  ::v-deep .statistic-box-header .cat2-bug-title {
    flex: 0 1 auto;
    width: auto;
    display: flex;
    align-items: center;
    line-height: var(--statistic-card-header-min-height, 17px);
  }

  ::v-deep .statistic-box-header .statistic-box-tools {
    flex-shrink: 0;
    gap: 5px;
    align-items: center;
  }

  ::v-deep .statistic-box-body {
    width: max-content;
    max-width: 100%;
    padding: 0;
    overflow: visible;
    align-items: flex-start;
    justify-content: flex-start;
  }
}

.plan-countdown-select {
  max-width: 108px;
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

.plan-countdown-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 0;
  width: max-content;
  max-width: 100%;
  height: 100%;
  font-size: 11px;
  line-height: 1.25;
  box-sizing: border-box;

  &.has-data {
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
    gap: 6px;
    padding: 0 4px 2px 4px;
  }
}

.plan-countdown-stats {
  flex: 0 0 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 5px;

  ::v-deep .el-tooltip {
    display: block;
    line-height: 1.25;
  }
}

.plan-stat-line {
  display: flex;
  align-items: center;
  gap: 0;
  min-width: 0;
  line-height: 1.25;
}

.plan-stat-icon {
  flex-shrink: 0;
  width: 14px !important;
  height: 14px !important;
  margin-right: 10px;
  color: inherit;
}

.plan-stat-text {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  min-width: 0;
}

.plan-stat-kind {
  flex-shrink: 0;
  font-size: 10px;
  white-space: nowrap;
}

.plan-stat-ratio {
  font-size: 11px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.plan-stat-line--case {
  color: #67c23a;
}

.plan-stat-line--defect {
  color: #f56c6c;

  &.is-all-closed {
    color: #67c23a;
  }
}

.plan-countdown-empty {
  padding: 2px 4px;
  color: var(--text-color-secondary, #909399);
  font-size: 11px;
}

.plan-countdown-main {
  flex: 0 0 auto;
  flex-shrink: 0;
  display: flex;
  align-items: baseline;
  flex-wrap: nowrap;
  justify-content: flex-start;
  gap: 4px;
  min-width: max-content;
  overflow: visible;

  .statistic-countdown-nixie {
    flex-shrink: 0;
    font-size: var(--plan-countdown-nixie-size, 44px);
    line-height: 1;
    letter-spacing: 0.06em;
  }

  &.is-overdue .statistic-countdown-nixie--overdue {
    font-size: var(--plan-countdown-nixie-size, 44px);
    letter-spacing: 0.06em;
  }
}

.plan-countdown-label,
.plan-countdown-unit,
.plan-countdown-no-end {
  color: var(--text-color-regular, #606266);
  font-size: 10px;
  white-space: nowrap;
}

</style>

<style lang="scss">
html.dark {
  .team-plan-countdown-card .plan-stat-line--case {
    color: #85ce61;
  }

  .team-plan-countdown-card .plan-stat-line--defect {
    color: #f89898;

    &.is-all-closed {
      color: #85ce61;
    }
  }
}

.el-select-dropdown.plan-countdown-select-dropdown {
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

  .plan-countdown-option-text {
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

  .el-select-dropdown__item.selected .plan-countdown-option-text {
    font-weight: 600;
  }
}
</style>
