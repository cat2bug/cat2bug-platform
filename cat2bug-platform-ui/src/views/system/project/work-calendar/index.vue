<template>
  <el-dialog
    v-model="visible"
    :title="$t('project-work-calendar.title')"
    width="720px"
    append-to-body
    destroy-on-close
    class="work-calendar-dialog"
    @closed="onClosed"
  >
    <div v-loading="loading" class="work-calendar-dialog__body">
      <el-alert
        v-if="missingYearHint"
        class="work-calendar-dialog__alert"
        type="warning"
        :closable="false"
        :title="missingYearHint"
        show-icon
      />

      <el-form label-width="96px" @submit.prevent>
        <el-form-item :label="$t('project-work-calendar.work-week')">
          <el-checkbox-group v-model="form.workWeek" :disabled="!editable">
            <el-checkbox v-for="d in weekOptions" :key="d.value" :value="d.value">
              {{ d.label }}
            </el-checkbox>
          </el-checkbox-group>
          <div class="work-calendar-dialog__hint">{{ $t('project-work-calendar.work-week-hint') }}</div>
        </el-form-item>

        <el-form-item :label="$t('project-work-calendar.month')">
          <div class="work-calendar-dialog__month">
            <div class="work-calendar-dialog__nav">
              <el-button :icon="ArrowLeft" circle @click="shiftMonth(-1)" />
              <span class="work-calendar-dialog__ym">{{ viewYear }}-{{ String(viewMonth + 1).padStart(2, '0') }}</span>
              <el-button :icon="ArrowRight" circle @click="shiftMonth(1)" />
            </div>
            <div class="work-calendar-dialog__grid">
              <div v-for="h in weekdayHeaders" :key="h" class="work-calendar-dialog__head">{{ h }}</div>
              <button
                v-for="cell in monthCells"
                :key="cell.key"
                type="button"
                class="work-calendar-dialog__cell"
                :class="{
                  'is-other': cell.other,
                  'is-off': cell.kind === 'off',
                  'is-holiday': cell.kind === 'holiday',
                  'is-makeup': cell.kind === 'workday',
                  'is-readonly': !editable
                }"
                :disabled="!editable"
                :title="cellTitle(cell)"
                @click="cycleDay(cell)"
              >
                <span class="work-calendar-dialog__day">{{ cell.day }}</span>
                <span v-if="cell.kind === 'holiday'" class="work-calendar-dialog__tag">休</span>
                <span v-else-if="cell.kind === 'workday'" class="work-calendar-dialog__tag">班</span>
              </button>
            </div>
            <div class="work-calendar-dialog__legend">
              <span><i class="lg lg-off" />{{ $t('project-work-calendar.legend-off') }}</span>
              <span><i class="lg lg-holiday" />{{ $t('project-work-calendar.legend-holiday') }}</span>
              <span><i class="lg lg-makeup" />{{ $t('project-work-calendar.legend-makeup') }}</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="work-calendar-dialog__footer">
        <span v-if="settings.updateTime" class="work-calendar-dialog__updated">
          {{ $t('project-work-calendar.updated', { by: settings.updateBy || '-', time: settings.updateTime }) }}
        </span>
        <el-button @click="visible = false">{{ $t('cancel') }}</el-button>
        <el-button v-if="editable" type="primary" :loading="saving" @click="save">{{ $t('save') }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import {
  getProjectWorkCalendar,
  updateProjectWorkCalendar
} from '@/api/system/project-work-calendar'
import { normalizeWorkCalendar, formatDateKey, isoWeekday } from '@/utils/work-calendar'
import { useUserStore } from '@/stores/user'

const WEEK_KEYS = [
  'project-work-calendar.mon',
  'project-work-calendar.tue',
  'project-work-calendar.wed',
  'project-work-calendar.thu',
  'project-work-calendar.fri',
  'project-work-calendar.sat',
  'project-work-calendar.sun'
]

export default {
  name: 'ProjectWorkCalendarDialog',
  emits: ['saved'],
  data() {
    const now = new Date()
    return {
      ArrowLeft,
      ArrowRight,
      visible: false,
      loading: false,
      saving: false,
      settings: normalizeWorkCalendar(null),
      form: {
        workWeek: [1, 2, 3, 4, 5],
        exceptions: {}
      },
      viewYear: now.getFullYear(),
      viewMonth: now.getMonth()
    }
  },
  computed: {
    projectId() {
      return parseInt(useUserStore().config.currentProjectId, 10)
    },
    editable() {
      return !!this.settings.editable
    },
    weekOptions() {
      return WEEK_KEYS.map((key, i) => ({ value: i + 1, label: this.$t(key) }))
    },
    weekdayHeaders() {
      return WEEK_KEYS.map((k) => this.$t(k))
    },
    missingYearHint() {
      const y = this.viewYear
      const prefix = `${y}-`
      const has = Object.keys(this.form.exceptions || {}).some((k) => k.startsWith(prefix))
      if (has) return ''
      return this.$t('project-work-calendar.missing-year', { year: y })
    },
    monthCells() {
      const year = this.viewYear
      const month = this.viewMonth
      const first = new Date(year, month, 1)
      const startPad = (isoWeekday(first) + 6) % 7
      const daysInMonth = new Date(year, month + 1, 0).getDate()
      const cells = []
      for (let i = 0; i < startPad; i++) {
        const d = new Date(year, month, 1 - (startPad - i))
        cells.push(this.cellFor(d, true))
      }
      for (let day = 1; day <= daysInMonth; day++) {
        cells.push(this.cellFor(new Date(year, month, day), false))
      }
      while (cells.length % 7 !== 0) {
        const last = cells[cells.length - 1]
        const d = new Date(last.date)
        d.setDate(d.getDate() + 1)
        cells.push(this.cellFor(d, true))
      }
      return cells
    }
  },
  methods: {
    /** 供父组件调用：打开并加载当前项目日历 */
    open() {
      const now = new Date()
      this.viewYear = now.getFullYear()
      this.viewMonth = now.getMonth()
      this.visible = true
      this.load()
    },
    onClosed() {
      this.settings = normalizeWorkCalendar(null)
      this.form = { workWeek: [1, 2, 3, 4, 5], exceptions: {} }
    },
    cellFor(date, other) {
      const key = formatDateKey(date)
      const ex = (this.form.exceptions || {})[key]
      let kind = 'work'
      if (ex === 'holiday') kind = 'holiday'
      else if (ex === 'workday') kind = 'workday'
      else if (!(this.form.workWeek || []).includes(isoWeekday(date))) kind = 'off'
      return {
        key: key + (other ? '-o' : ''),
        date,
        day: date.getDate(),
        other,
        kind,
        dateKey: key
      }
    },
    cellTitle(cell) {
      if (cell.kind === 'holiday') return this.$t('project-work-calendar.type-holiday')
      if (cell.kind === 'workday') return this.$t('project-work-calendar.type-workday')
      if (cell.kind === 'off') return this.$t('project-work-calendar.legend-off')
      return this.$t('project-work-calendar.legend-work')
    },
    shiftMonth(delta) {
      const d = new Date(this.viewYear, this.viewMonth + delta, 1)
      this.viewYear = d.getFullYear()
      this.viewMonth = d.getMonth()
    },
    cycleDay(cell) {
      if (!this.editable || cell.other) return
      const key = cell.dateKey
      const cur = this.form.exceptions[key]
      const next = { ...this.form.exceptions }
      // 工作周外（默认六日）已是休息，只需切「调休上班」；工作周内只需切「假日」
      const inWorkWeek = (this.form.workWeek || []).includes(isoWeekday(cell.date))
      if (inWorkWeek) {
        if (!cur) next[key] = 'holiday'
        else delete next[key]
      } else if (!cur) {
        next[key] = 'workday'
      } else {
        delete next[key]
      }
      this.form.exceptions = next
    },
    async load() {
      if (!this.projectId) return
      this.loading = true
      try {
        const res = await getProjectWorkCalendar(this.projectId)
        this.settings = normalizeWorkCalendar(res.data)
        this.form.workWeek = [...(this.settings.workWeek || [1, 2, 3, 4, 5])]
        this.form.exceptions = { ...(this.settings.exceptions || {}) }
      } catch (e) {
        this.settings = normalizeWorkCalendar(null)
        this.$modal.msgError(this.$t('project-work-calendar.load-fail'))
      } finally {
        this.loading = false
      }
    },
    async save() {
      this.saving = true
      try {
        const res = await updateProjectWorkCalendar(this.projectId, {
          workWeek: this.form.workWeek,
          exceptions: this.form.exceptions
        })
        this.settings = normalizeWorkCalendar(res.data)
        this.form.workWeek = [...(this.settings.workWeek || [])]
        this.form.exceptions = { ...(this.settings.exceptions || {}) }
        this.$modal.msgSuccess(this.$t('save-success'))
        this.$emit('saved', this.settings)
        this.visible = false
      } catch (e) {
        /* request 已提示 */
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped lang="scss">
.work-calendar-dialog {
  &__body {
    min-height: 120px;
  }
  &__alert {
    margin-bottom: 12px;
  }
  &__hint {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.5;
    margin-top: 4px;
  }
  &__month {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
    width: 100%;
    max-width: 520px;
  }
  &__nav {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
  }
  &__ym {
    font-weight: 600;
    min-width: 88px;
    text-align: center;
  }
  &__grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 4px;
    width: 100%;
  }
  &__head {
    text-align: center;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    padding: 4px 0;
  }
  &__cell {
    position: relative;
    min-height: 40px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 4px;
    background: var(--el-fill-color-blank, var(--el-bg-color));
    color: var(--el-text-color-primary);
    cursor: pointer;
    padding: 4px;
    text-align: left;
    &.is-other {
      opacity: 0.35;
    }
    &.is-off {
      background: var(--el-fill-color);
      color: var(--el-text-color-secondary);
    }
    &.is-holiday {
      background: var(--el-color-danger-light-9);
      color: var(--el-color-danger);
    }
    &.is-makeup {
      background: var(--el-color-primary-light-9);
      color: var(--el-color-primary);
    }
    &.is-readonly {
      cursor: default;
    }
  }
  &__day {
    font-size: 13px;
  }
  &__tag {
    position: absolute;
    right: 4px;
    bottom: 2px;
    font-size: 11px;
    color: inherit;
    opacity: 0.85;
  }
  &__legend {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    .lg {
      display: inline-block;
      width: 12px;
      height: 12px;
      margin-right: 4px;
      border-radius: 2px;
      vertical-align: -1px;
      border: 1px solid var(--el-border-color-lighter);
    }
    .lg-off { background: var(--el-fill-color); }
    .lg-holiday { background: var(--el-color-danger-light-9); }
    .lg-makeup { background: var(--el-color-primary-light-9); }
  }
  &__footer {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 12px;
    width: 100%;
  }
  &__updated {
    margin-right: auto;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
