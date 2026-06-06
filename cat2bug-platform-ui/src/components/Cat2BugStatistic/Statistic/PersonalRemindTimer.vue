<template>
  <div class="personal-remind-timer-root">
    <cat2-bug-card
      :title="cardTitle"
      v-loading="loading"
      :tools="tools"
      @tools-click="toolsHandle"
    >
      <template slot="content">
        <div class="remind-timer-body" @click="onBodyClick">
          <div v-if="!activeRow" class="remind-timer-empty">
            {{ $t('defect.personal-remind-timer.empty') }}
          </div>
          <template v-else>
            <div ref="countdownWrap" class="remind-timer-countdown-wrap">
              <span
                v-if="activeRow.countdownText && !ringingTimerId"
                class="statistic-countdown-nixie statistic-countdown-nixie--hero"
                :style="countdownFontStyle"
              >
                <span
                  v-for="(ch, ci) in splitCountdown(activeRow.countdownText)"
                  :key="`${activeRow.timer.id}-${ci}`"
                  class="statistic-countdown-nixie-digit"
                  :class="isCountdownDigit(ch) ? 'is-digit' : 'is-sep'"
                >{{ ch }}</span>
              </span>
              <div v-if="ringingTimerId" class="remind-timer-ringing">
                <button
                  type="button"
                  class="remind-timer-stop-sound"
                  :title="$t('defect.personal-remind-timer.stop-sound').toString()"
                  :aria-label="$t('defect.personal-remind-timer.stop-sound').toString()"
                  @click.stop="stopRinging"
                >
                  <svg class="remind-timer-stop-sound-icon" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z" />
                  </svg>
                </button>
              </div>
            </div>
          </template>
        </div>
      </template>
    </cat2-bug-card>

    <el-dialog
      :title="$t('defect.personal-remind-timer')"
      :visible.sync="dialogVisible"
      width="800px"
      append-to-body
      custom-class="personal-remind-timer-dialog"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="onToolDialogBeforeClose"
      @opened="onToolDialogOpened"
      @closed="onDialogClosed"
    >
      <el-table ref="remindTimerTable" :data="editTimers" size="mini" class="remind-timer-table">
        <el-table-column width="58" align="center" class-name="remind-switch-col">
          <template slot-scope="scope">
            <el-switch v-model="scope.row.enabled" data-remind-stop />
          </template>
        </el-table-column>
        <el-table-column :label="$t('defect.personal-remind-timer.label')" min-width="136">
          <template slot-scope="scope">
            <el-input v-model="scope.row.label" data-remind-stop="label" size="mini" maxlength="32" />
          </template>
        </el-table-column>
        <el-table-column :label="$t('defect.personal-remind-timer.type')" width="108">
          <template slot-scope="scope">
            <el-select v-model="scope.row.schedule.type" data-remind-stop size="mini" @change="onTypeChange(scope.row)">
              <el-option value="once" :label="$t('defect.personal-remind-timer.type-once')" />
              <el-option value="daily" :label="$t('defect.personal-remind-timer.type-daily')" />
              <el-option value="weekly" :label="$t('defect.personal-remind-timer.type-weekly')" />
              <el-option value="monthly" :label="$t('defect.personal-remind-timer.type-monthly')" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column :label="$t('defect.personal-remind-timer.when')" min-width="204">
          <template slot-scope="scope">
            <div class="remind-when-cell">
              <el-date-picker
                v-if="scope.row.schedule.type === 'once'"
                v-model="scope.row.schedule.date"
                data-remind-stop
                type="date"
                value-format="yyyy-MM-dd"
                size="mini"
                class="remind-when-date"
              />
              <el-select
                v-else-if="scope.row.schedule.type === 'weekly'"
                v-model="scope.row.schedule.weekday"
                data-remind-stop
                size="mini"
                class="remind-when-week"
              >
                <el-option v-for="w in weekdays" :key="w.v" :value="w.v" :label="w.l" />
              </el-select>
              <el-input-number
                v-else-if="scope.row.schedule.type === 'monthly'"
                v-model="scope.row.schedule.day"
                data-remind-stop
                :min="1"
                :max="31"
                size="mini"
                controls-position="right"
                class="remind-when-day"
              />
              <el-time-picker
                v-model="scope.row._timePicker"
                data-remind-stop
                format="HH:mm"
                value-format="HH:mm"
                size="mini"
                :clearable="false"
                class="remind-when-time"
                @focus="onRemindTimePickerFocus"
                @change="val => onTimeChange(scope.row, val)"
                @blur="onRemindTimePickerBlur"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('defect.personal-remind-timer.sound')" width="176" class-name="remind-sound-col">
          <template slot-scope="scope">
            <div class="remind-sound-cell">
              <el-select v-model="scope.row.sound" data-remind-stop size="mini" class="remind-sound-select">
                <el-option
                  v-for="s in soundOptions"
                  :key="s.id"
                  :value="s.id"
                  :label="$t('defect.personal-remind-timer.sound-' + s.id)"
                />
              </el-select>
              <el-button
                type="text"
                data-remind-stop
                size="mini"
                class="remind-sound-preview"
                @click="previewSound(scope.row.sound)"
              >{{ $t('defect.personal-remind-timer.preview') }}</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          width="44"
          align="center"
          class-name="remind-action-col"
        >
          <template slot="header">
            <el-button
              ref="remindAddBtn"
              type="text"
              icon="el-icon-plus"
              class="remind-add-btn cat2bug-field-hint-host"
              :title="$t('defect.personal-remind-timer.add').toString()"
              @click="addTimer"
            />
          </template>
          <template slot-scope="scope">
            <el-button
              type="text"
              icon="el-icon-delete"
              data-remind-stop
              class="remind-delete-btn"
              :title="$t('delete').toString()"
              @click="removeTimer(scope.$index)"
            />
          </template>
        </el-table-column>
      </el-table>
      <span slot="footer">
        <el-button @click="requestCloseToolDialog">{{ $t('cancel') }}</el-button>
        <el-button class="defect-kbd-hint-host" type="primary" @click="saveDialog">
          {{ $t('update') }}
          <span v-show="fieldHintsActive" class="cat2bug-field-hint defect-kbd-hint defect-kbd-hint--primary" aria-hidden="true">{{ dialogSaveShortcutLabel }}</span>
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import Cat2BugCard from '../Components/Card'
import statisticDialogKbd from '@/mixins/statistic-dialog-kbd'
import remindTimerTableKbd from '@/mixins/remind-timer-table-kbd'
import { serializeRemindTimerCloseState } from '@/utils/statistic-dialog-close-state'
import { closeDropdownsOnBlur } from '@/utils/dropdown-blur-close'
import {
  DEFAULT_RING_DURATION_MS,
  SOUND_OPTIONS,
  buildTimerRows,
  defaultTimer,
  detectTimerFire,
  newTimerId,
  soundUrl
} from '../utils/remind-timer'

export default {
  name: 'PersonalRemindTimer',
  mixins: [statisticDialogKbd, remindTimerTableKbd],
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
      dialogVisible: false,
      timers: [],
      editTimers: [],
      activeRow: null,
      countdownFontSize: 48,
      countdownResizeObserver: null,
      _countdownFitBoundsKey: '',
      _countdownFitTimerId: null,
      _countdownFitRaf: null,
      _countdownResizeTimer: null,
      tickTimer: null,
      lastFiredKey: null,
      ringingTimerId: null,
      ringAudio: null,
      ringStopTimer: null,
      audioUnlocked: false,
      soundOptions: SOUND_OPTIONS
    }
  },
  computed: {
    cardTitle() {
      if (!this.activeRow || !this.activeRow.timer) {
        return this.$t('defect.personal-remind-timer').toString()
      }
      const timer = this.activeRow.timer
      const label = timer.label || this.$t('defect.personal-remind-timer.unnamed').toString()
      return `${label}[${this.formatScheduleBracket(timer)}]`
    },
    countdownFontStyle() {
      return { fontSize: `${this.countdownFontSize}px` }
    },
    weekdays() {
      return [1, 2, 3, 4, 5, 6, 7].map(v => ({
        v,
        l: this.$t(`defect.personal-remind-timer.weekday-${v}`).toString()
      }))
    }
  },
  watch: {
    'params.timers': {
      handler(val) {
        this.syncTimersFromParams(val)
      },
      deep: true,
      immediate: true
    }
  },
  mounted() {
    this.syncTimersFromParams(this.params && this.params.timers)
    if (!this.read) {
      this.startTick()
      document.addEventListener('visibilitychange', this.onVisibilityChange)
    } else {
      this.tickDisplay()
    }
    this.$nextTick(() => {
      this.setupCountdownResizeObserver()
      this.scheduleFitCountdownSize()
    })
  },
  beforeDestroy() {
    this.stopTick()
    this.stopRinging()
    this.teardownCountdownResizeObserver()
    if (this._countdownFitRaf) {
      cancelAnimationFrame(this._countdownFitRaf)
      this._countdownFitRaf = null
    }
    if (this._countdownResizeTimer) {
      clearTimeout(this._countdownResizeTimer)
      this._countdownResizeTimer = null
    }
    this.teardownCountdownMeasureEl()
    document.removeEventListener('visibilitychange', this.onVisibilityChange)
  },
  methods: {
    syncTimersFromParams(list) {
      const src = Array.isArray(list) ? list : []
      this.timers = src.map(t => this.normalizeTimer(t))
      this.tickDisplay()
    },
    normalizeTimer(t) {
      const schedule = { ...(t.schedule || {}) }
      if (!schedule.type) schedule.type = 'daily'
      if (!schedule.time) schedule.time = '09:00'
      return {
        id: t.id || newTimerId(),
        label: t.label || '',
        enabled: t.enabled !== false,
        sound: t.sound || 'bell-soft',
        firedOnce: !!t.firedOnce,
        schedule
      }
    },
    cloneForEdit() {
      return this.timers.map(t => {
        const row = JSON.parse(JSON.stringify(t))
        row._timePicker = row.schedule.time || '09:00'
        return row
      })
    },
    onBodyClick() {
      this.unlockAudio()
      this.openDialog()
    },
    openDialog() {
      if (this.read) {
        return
      }
      this.unlockAudio()
      this.editTimers = this.cloneForEdit()
      this.dialogVisible = true
    },
    onDialogClosed() {
      this.$_unbindRemindTimePickerPickRefocus()
      this._remindTimePickerVm = null
      this.editTimers = []
    },
    addTimer() {
      const t = defaultTimer()
      t._timePicker = t.schedule.time
      this.editTimers.push(t)
    },
    removeTimer(index) {
      this.editTimers.splice(index, 1)
    },
    onTypeChange(row) {
      const s = row.schedule
      if (s.type === 'once' && !s.date) {
        const now = new Date()
        s.date = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
      }
      if (s.type === 'weekly' && !s.weekday) {
        s.weekday = 1
      }
      if (s.type === 'monthly' && !s.day) {
        s.day = 1
      }
    },
    onRemindTimePickerFocus(pickerVm) {
      if (!pickerVm) return
      this._remindTimePickerVm = pickerVm
      this.$_wrapRemindTimePickerFocus(pickerVm)
      this.$_wrapRemindTimePickerKeydown(pickerVm)
      this.$_ensureRemindTimePickerPickRefocus(pickerVm)
    },
    $_wrapRemindTimePickerFocus(pickerVm) {
      if (!pickerVm || pickerVm._remindHandleFocusWrapped) return
      if (typeof pickerVm.handleFocus === 'function') {
        pickerVm._remindNativeHandleFocus = pickerVm.handleFocus.bind(pickerVm)
        pickerVm.handleFocus = function wrappedRemindTimePickerFocus() {
          if (pickerVm._remindSilentFocus) {
            pickerVm.$emit('focus', pickerVm)
            return
          }
          pickerVm._remindNativeHandleFocus()
        }
      }
      if (typeof pickerVm.blur === 'function') {
        pickerVm._remindNativeBlur = pickerVm.blur.bind(pickerVm)
        pickerVm.blur = function wrappedRemindTimePickerBlur() {
          if (pickerVm._remindSuppressBlur) return
          pickerVm._remindNativeBlur()
        }
      }
      pickerVm._remindHandleFocusWrapped = true
    },
    $_wrapRemindTimePickerKeydown(pickerVm) {
      if (!pickerVm || pickerVm._remindHandleKeydownWrapped) return
      if (typeof pickerVm.handleKeydown !== 'function') return
      const component = this
      pickerVm._remindNativeHandleKeydown = pickerVm.handleKeydown.bind(pickerVm)
      pickerVm.handleKeydown = function wrappedRemindTimePickerKeydown(event) {
        const keyCode = event.keyCode
        const panelWasOpen = pickerVm.pickerVisible

        if (!pickerVm.pickerVisible && (keyCode === 40 || keyCode === 38)) {
          if (pickerVm._remindNativeHandleFocus) {
            pickerVm._remindNativeHandleFocus()
          } else if (typeof pickerVm.handleFocus === 'function') {
            pickerVm.handleFocus()
          }
          event.preventDefault()
          pickerVm.$nextTick(() => {
            if (pickerVm.picker && typeof pickerVm.picker.handleKeydown === 'function') {
              pickerVm.picker.handleKeydown(event)
            }
          })
          return
        }

        pickerVm._remindNativeHandleKeydown(event)

        if (keyCode === 13 && panelWasOpen && !pickerVm.pickerVisible) {
          component._remindTimePickerRefocusing = true
          component.$_scheduleRemindTimePickerRefocusAfterPick(pickerVm)
        }
      }
      pickerVm._remindHandleKeydownWrapped = true
    },
    $_ensureRemindTimePickerPickRefocus(pickerVm) {
      if (!pickerVm || pickerVm._remindPickRefocusBound) return
      const bindPick = () => {
        if (pickerVm._remindPickRefocusBound) return
        if (!pickerVm.picker) {
          this.$nextTick(() => bindPick())
          return
        }
        pickerVm._remindPickRefocusBound = true
        pickerVm.picker.$on('pick', (date, visible, first) => {
          if (first) return
          // 滚轮/Spinner 变更时 visible=true，仅点「确定」关闭时 visible=false
          if (visible) return
          this.$_scheduleRemindTimePickerRefocusAfterPick(pickerVm)
        })
      }
      bindPick()
    },
    $_unbindRemindTimePickerPickRefocus() {
      const tableEl = typeof this.$_getRemindTimerTableEl === 'function' && this.$_getRemindTimerTableEl()
      if (!tableEl) return
      tableEl.querySelectorAll('.remind-when-time').forEach((editor) => {
        let vm = editor.__vue__
        while (vm) {
          if (vm.$options && vm.$options.name === 'ElTimePicker') {
            if (vm.picker && vm._remindPickRefocusBound) {
              vm.picker.$off('pick')
            }
            vm._remindPickRefocusBound = false
            break
          }
          vm = vm.$parent
        }
      })
    },
    $_scheduleRemindTimePickerRefocusAfterPick(pickerVm) {
      this._remindTimePickerRefocusing = true
      const attempt = () => {
        if (!this.dialogVisible) return
        this.$_focusRemindTimePickerInput(pickerVm)
      }
      window.setTimeout(attempt, 50)
      window.setTimeout(attempt, 120)
    },
    onTimeChange(row, val) {
      row.schedule.time = val || '09:00'
      row._timePicker = row.schedule.time
    },
    $_resolveRemindTimePickerInput(pickerVm) {
      if (!pickerVm) return null
      const ref = pickerVm.$refs && pickerVm.$refs.reference
      if (ref && ref.$el) {
        const inner = ref.$el.querySelector('.el-input__inner')
        if (inner) return inner
      }
      return pickerVm.$el ? pickerVm.$el.querySelector('.el-input__inner') : null
    },
    $_focusRemindTimePickerInput(pickerVm) {
      if (!this.dialogVisible || !pickerVm) return false
      this.$_wrapRemindTimePickerFocus(pickerVm)
      const input = this.$_resolveRemindTimePickerInput(pickerVm)
      if (!input || !input.isConnected) return false
      this._remindTimePickerRefocusing = true
      pickerVm._remindSilentFocus = true
      pickerVm._remindSuppressBlur = true
      try {
        input.focus({ preventScroll: false })
      } catch (e) {
        input.focus()
      }
      pickerVm._remindSilentFocus = false
      if (pickerVm.pickerVisible) {
        pickerVm.pickerVisible = false
        if (pickerVm.picker) pickerVm.picker.visible = false
        if (typeof pickerVm.destroyPopper === 'function') pickerVm.destroyPopper()
        pickerVm._remindSilentFocus = true
        try {
          input.focus({ preventScroll: false })
        } catch (e) {
          input.focus()
        }
        pickerVm._remindSilentFocus = false
      }
      window.setTimeout(() => {
        pickerVm._remindSuppressBlur = false
        this._remindTimePickerRefocusing = false
      }, 50)
      return true
    },
    onRemindTimePickerBlur() {
      this.$nextTick(() => {
        const active = document.activeElement
        if (active && active.closest && active.closest('.el-time-panel')) return
        if (this._remindTimePickerRefocusing) return
        if (typeof this.$_closeRemindTimerPickerPanels === 'function') {
          this.$_closeRemindTimerPickerPanels()
        }
        closeDropdownsOnBlur()
      })
    },
    shortcutSave() {
      this.saveDialog()
    },
    getStatisticDialogCloseSnapshot() {
      return this.editTimers
    },
    serializeStatisticDialogCloseSnapshot(snapshot) {
      return serializeRemindTimerCloseState(snapshot)
    },
    getFieldHintContainer() {
      const table = this.$refs.remindTimerTable
      return (table && table.$el) || this.$el
    },
    saveDialog() {
      const timers = this.editTimers.map(row => {
        const { _timePicker, ...rest } = row
        if (_timePicker) {
          rest.schedule.time = _timePicker
        }
        return this.normalizeTimer(rest)
      })
      this.timers = timers
      this.toolDialogCloseBaseline = null
      this.dialogVisible = false
      this.persistTimers()
      this.tickDisplay()
      if (!this.read) {
        this.startTick()
      }
    },
    persistTimers() {
      if (this.read || !this.parent || typeof this.parent.saveStatisticItemParams !== 'function') {
        return
      }
      this.parent.saveStatisticItemParams('PersonalRemindTimer', { timers: this.timers })
    },
    startTick() {
      this.stopTick()
      this.tickDisplay()
      this.tickTimer = window.setInterval(() => this.onTick(), 1000)
    },
    stopTick() {
      if (this.tickTimer) {
        clearInterval(this.tickTimer)
        this.tickTimer = null
      }
    },
    onVisibilityChange() {
      if (document.hidden) {
        this.stopTick()
        this.stopRinging()
      } else if (!this.read) {
        this.startTick()
      }
    },
    onTick() {
      if (this.read || document.hidden) {
        return
      }
      const now = new Date()
      this.timers.forEach(timer => {
        const fire = detectTimerFire(timer, now)
        if (fire && this.lastFiredKey !== fire.fireKey) {
          this.fireTimer(timer, fire.fireKey)
        }
      })
      this.tickDisplay()
    },
    fireTimer(timer, fireKey) {
      this.lastFiredKey = fireKey
      this.startRinging(timer)
      if (timer.schedule && timer.schedule.type === 'once') {
        timer.firedOnce = true
        timer.enabled = false
        this.persistTimers()
      }
    },
    unlockAudio() {
      if (this.audioUnlocked || this.read) {
        return
      }
      try {
        const audio = new Audio()
        audio.volume = 0
        const p = audio.play()
        if (p && typeof p.then === 'function') {
          p.then(() => {
            audio.pause()
            this.audioUnlocked = true
          }).catch(() => {})
        } else {
          this.audioUnlocked = true
        }
      } catch (e) {
        /* 浏览器限制时静默 */
      }
    },
    startRinging(timer) {
      this.stopRinging()
      if (this.read) {
        return
      }
      try {
        const audio = new Audio(soundUrl(timer.sound))
        audio.loop = true
        audio.volume = 0.6
        this.ringAudio = audio
        this.ringingTimerId = timer.id
        const p = audio.play()
        if (p && typeof p.catch === 'function') {
          p.catch(() => {})
        }
        this.ringStopTimer = window.setTimeout(() => {
          this.stopRinging()
        }, DEFAULT_RING_DURATION_MS)
      } catch (e) {
        this.ringingTimerId = null
        this.ringAudio = null
      }
    },
    stopRinging() {
      if (this.ringStopTimer) {
        clearTimeout(this.ringStopTimer)
        this.ringStopTimer = null
      }
      if (this.ringAudio) {
        this.ringAudio.pause()
        this.ringAudio.currentTime = 0
        this.ringAudio = null
      }
      this.ringingTimerId = null
    },
    playSoundOnce(soundId) {
      this.unlockAudio()
      try {
        const audio = new Audio(soundUrl(soundId))
        audio.volume = 0.6
        const p = audio.play()
        if (p && typeof p.catch === 'function') {
          p.catch(() => {})
        }
      } catch (e) {
        /* 自动播放策略拦截时静默 */
      }
    },
    previewSound(soundId) {
      this.playSoundOnce(soundId)
    },
    splitCountdown(text) {
      return (text || '').split('')
    },
    isCountdownDigit(ch) {
      return /[0-9]/.test(ch)
    },
    formatScheduleBracket(timer) {
      const schedule = (timer && timer.schedule) || {}
      const time = schedule.time || '09:00'
      const type = schedule.type || 'daily'
      const typeLabel = this.$t(`defect.personal-remind-timer.type-${type}`).toString()
      if (type === 'once') {
        return [typeLabel, schedule.date, time].filter(Boolean).join(' ')
      }
      if (type === 'weekly') {
        const weekday = this.$t(`defect.personal-remind-timer.weekday-${schedule.weekday || 1}`).toString()
        return `${typeLabel} ${weekday} ${time}`
      }
      if (type === 'monthly') {
        return `${typeLabel} ${schedule.day || 1} ${time}`
      }
      return `${typeLabel} ${time}`
    },
    formatScheduleSummary(timer) {
      return this.formatScheduleBracket(timer)
    },
    setupCountdownResizeObserver() {
      this.teardownCountdownResizeObserver()
      if (typeof ResizeObserver === 'undefined' || !this.$refs.countdownWrap) {
        return
      }
      this.countdownResizeObserver = new ResizeObserver(() => {
        if (this._countdownResizeTimer) {
          clearTimeout(this._countdownResizeTimer)
        }
        this._countdownResizeTimer = setTimeout(() => {
          this._countdownResizeTimer = null
          this.scheduleFitCountdownSize(true)
        }, 120)
      })
      this.countdownResizeObserver.observe(this.$refs.countdownWrap)
    },
    scheduleFitCountdownSize() {
      if (this._countdownFitRaf) {
        cancelAnimationFrame(this._countdownFitRaf)
      }
      this._countdownFitRaf = requestAnimationFrame(() => {
        this._countdownFitRaf = requestAnimationFrame(() => {
          this._countdownFitRaf = null
          this.fitCountdownSize(false)
        })
      })
    },
    teardownCountdownResizeObserver() {
      if (this.countdownResizeObserver) {
        this.countdownResizeObserver.disconnect()
        this.countdownResizeObserver = null
      }
    },
    getCountdownProbeText(text) {
      if (!text) {
        return '88:88:88'
      }
      const dayMatch = String(text).match(/^(\d+)\s+/)
      if (dayMatch) {
        return `${'9'.repeat(dayMatch[1].length)} 88:88:88`
      }
      return '88:88:88'
    },
    teardownCountdownMeasureEl() {
      if (this._countdownMeasureEl && this._countdownMeasureEl.parentNode) {
        this._countdownMeasureEl.parentNode.removeChild(this._countdownMeasureEl)
      }
      this._countdownMeasureEl = null
    },
    getCountdownMeasureEl(wrap, probeText) {
      if (!this._countdownMeasureEl) {
        const el = document.createElement('span')
        el.className = 'statistic-countdown-nixie statistic-countdown-nixie--hero'
        el.setAttribute('aria-hidden', 'true')
        el.style.cssText = 'position:absolute;left:-9999px;top:0;visibility:hidden;pointer-events:none;white-space:nowrap;'
        wrap.appendChild(el)
        this._countdownMeasureEl = el
      }
      const html = probeText.split('').map(ch => {
        const cls = /[0-9]/.test(ch) ? 'is-digit' : 'is-sep'
        return `<span class="statistic-countdown-nixie-digit ${cls}">${ch}</span>`
      }).join('')
      this._countdownMeasureEl.innerHTML = html
      return this._countdownMeasureEl
    },
    getCountdownFitBounds(wrap) {
      let maxWidth = wrap.clientWidth
      let maxHeight = wrap.clientHeight
      if (maxHeight <= 0) {
        const body = wrap.closest('.remind-timer-body')
        if (body) {
          maxHeight = body.clientHeight
        }
      }
      if (maxWidth <= 0) {
        const body = wrap.closest('.statistic-box-body')
        maxWidth = body ? body.clientWidth : 0
      }
      return { maxWidth, maxHeight }
    },
    fitCountdownSize(force = false) {
      const wrap = this.$refs.countdownWrap
      const text = this.activeRow && this.activeRow.countdownText
      if (!wrap || !text) {
        return
      }
      const timerId = this.activeRow.timer && this.activeRow.timer.id
      const { maxWidth, maxHeight } = this.getCountdownFitBounds(wrap)
      if (maxWidth <= 0 || maxHeight <= 0) {
        return
      }
      const boundsKey = `${Math.round(maxWidth)}x${Math.round(maxHeight)}`
      const probeText = this.getCountdownProbeText(text)
      const fitKey = `${boundsKey}|${probeText}|${timerId || ''}`
      if (!force && fitKey === this._countdownFitBoundsKey && this.countdownFontSize >= 12) {
        return
      }
      const el = this.getCountdownMeasureEl(wrap, probeText)
      const fits = (size) => {
        el.style.fontSize = `${size}px`
        return el.scrollWidth <= maxWidth && el.offsetHeight <= maxHeight
      }
      let lo = 12
      let hi = Math.max(12, Math.floor(maxHeight))
      let chosen = lo
      while (lo <= hi) {
        const mid = Math.floor((lo + hi) / 2)
        if (fits(mid)) {
          chosen = mid
          lo = mid + 1
        } else {
          hi = mid - 1
        }
      }
      el.style.fontSize = ''
      if (Math.abs(chosen - this.countdownFontSize) >= 1 || force) {
        this.countdownFontSize = chosen
      }
      this._countdownFitBoundsKey = fitKey
      this._countdownFitTimerId = timerId
    },
    tickDisplay() {
      const rows = buildTimerRows(this.timers)
      const prevTimerId = this.activeRow && this.activeRow.timer && this.activeRow.timer.id
      const prevProbe = this.activeRow && this.activeRow.countdownText
        ? this.getCountdownProbeText(this.activeRow.countdownText)
        : ''
      this.activeRow = rows[0] || null
      const nextTimerId = this.activeRow && this.activeRow.timer && this.activeRow.timer.id
      const nextProbe = this.activeRow && this.activeRow.countdownText
        ? this.getCountdownProbeText(this.activeRow.countdownText)
        : ''
      const needRefit = prevTimerId !== nextTimerId || prevProbe !== nextProbe
      this.$nextTick(() => {
        if (this.$refs.countdownWrap && !this.countdownResizeObserver) {
          this.setupCountdownResizeObserver()
        }
        if (needRefit || !this._countdownFitBoundsKey) {
          this.scheduleFitCountdownSize(true)
        }
      })
    },
    refreshData() {
      this.tickDisplay()
    },
    toolsHandle(e, tool) {
      this.$emit('tools-click', tool)
    }
  }
}
</script>

<style lang="scss" scoped>
.personal-remind-timer-root {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  width: 100%;
  box-sizing: border-box;

  ::v-deep .statistic-box {
    flex: 1 1 auto;
    height: 100%;
    max-height: 100%;
    min-height: 0;
    width: 100%;
  }

  ::v-deep .statistic-box-header {
    min-height: 0;
    margin-bottom: 2px;
    padding-bottom: 2px;
  }

  ::v-deep .statistic-box-body {
    flex: 1 1 auto;
    min-height: 0;
    overflow: hidden;
  }
}

.remind-timer-body {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  align-items: stretch;
  justify-content: center;
}

.remind-timer-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--text-color-secondary, #909399);
  font-size: 11px;
}

.remind-timer-countdown-wrap {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
  height: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  box-sizing: border-box;
  overflow: hidden;

  ::v-deep .statistic-countdown-nixie--hero {
    width: auto;
    max-width: none;
    height: auto;
    max-height: none;
    align-items: center;
    flex-shrink: 0;
  }
}

.remind-timer-ringing {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.remind-timer-stop-sound {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  padding: 0;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.12);
  transition: background-color 0.2s, transform 0.15s;

  &:hover {
    background: rgba(245, 108, 108, 0.22);
  }

  &:active {
    transform: scale(0.96);
  }
}

.remind-timer-stop-sound-icon {
  width: 24px;
  height: 24px;
  fill: currentColor;
}
</style>

<style lang="scss">
.personal-remind-timer-dialog {
  &.el-dialog > .el-dialog__header {
    padding: 20px 10px 10px !important;
  }

  &.el-dialog > .el-dialog__body {
    padding: 0 10px 20px !important;
  }

  &.el-dialog > .el-dialog__footer {
    padding: 20px 10px 20px !important;
  }

  .remind-timer-table {
    width: 100%;

    .el-table__body-wrapper {
      overflow: visible !important;
    }

    .el-table__header-wrapper {
      overflow: visible !important;
    }

    tbody td {
      padding-top: 4px;
      padding-bottom: 4px;
    }

    th.remind-action-col,
    td.remind-action-col {
      padding: 0;
      text-align: center;
      vertical-align: middle;
    }

    th.remind-action-col .cell,
    td.remind-action-col .cell {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 100%;
      min-height: 32px;
      padding: 0 !important;
      overflow: visible;
    }

    th.remind-switch-col,
    td.remind-switch-col {
      padding: 0;
      text-align: center;
      vertical-align: middle;
      overflow: visible;
    }

    th.remind-switch-col .cell,
    td.remind-switch-col .cell {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: flex-start;
      width: 100%;
      height: 100%;
      min-height: 32px;
      padding: 4px 4px 4px 10px !important;
      overflow: visible;
      box-sizing: border-box;
    }

    th:not(.remind-action-col):not(.remind-switch-col) .cell,
    td:not(.remind-action-col):not(.remind-switch-col) .cell {
      padding-left: 12px !important;
      padding-right: 12px !important;
    }

    td:not(.remind-action-col):not(.remind-switch-col) .cell {
      overflow: visible;
      padding-top: 4px !important;
      padding-bottom: 4px !important;
    }

    td.remind-sound-col .cell,
    th.remind-sound-col .cell {
      overflow: visible;
    }
  }

  .remind-when-time {
    width: 92px;
    flex-shrink: 0;
  }

  .remind-when-cell {
    display: flex;
    align-items: center;
    flex-wrap: nowrap;
    gap: 8px;
    max-width: 100%;
    overflow: visible;
  }

  .remind-when-date {
    width: 118px;
    flex-shrink: 0;
  }

  .remind-when-week {
    width: 72px;
    flex-shrink: 0;
  }

  .remind-when-day {
    width: 100px;
    flex-shrink: 0;

    &.el-input-number.is-controls-right {
      .el-input {
        width: 100%;
      }

      .el-input__inner {
        padding-left: 8px;
        padding-right: 34px;
        text-align: left;
      }
    }
  }

  .remind-sound-cell {
    display: flex;
    align-items: center;
    flex-wrap: nowrap;
    gap: 8px;
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
  }

  .remind-sound-select {
    width: 78px;
    flex: 0 0 78px;
  }

  .remind-sound-preview {
    flex: 0 0 auto;
    min-height: 24px;
    padding: 2px 6px;
    white-space: nowrap;
    border-radius: 4px;

    &:focus,
    &:focus-visible {
      outline: none !important;
      box-shadow: 0 0 0 1px var(--cat2bug-field-focus-color, #1890ff) !important;
    }
  }

  .remind-add-btn,
  .remind-delete-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    margin: 0;
    min-width: 28px;
    min-height: 28px;
    padding: 4px;
    line-height: 1;
    border-radius: 4px;

    &:focus,
    &:focus-visible {
      outline: none !important;
      box-shadow: 0 0 0 1px var(--cat2bug-field-focus-color, #1890ff) !important;
    }
  }

  .remind-add-btn {
    position: relative;
    overflow: visible !important;
    font-size: 16px;
    color: #409eff;
  }

  .remind-delete-btn {
    font-size: 16px;
    color: #f56c6c !important;

    &:hover {
      color: #f78989 !important;
    }
  }
}
</style>
