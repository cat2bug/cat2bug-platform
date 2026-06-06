/**
 * 报时提醒弹框表格快捷键：
 * - ⌘/Ctrl + 数字：聚焦对应行首控件
 * - ⌘/Ctrl + +：新增一行
 * - Del：删除当前行（可编辑文本框内除外）
 */
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'
import { closeDropdownsOnBlur } from '@/utils/dropdown-blur-close'
import {
  getRemindTimerRowStops,
  resolveRemindTimerColIndex,
  resolveRemindTimerRowIndex,
  shouldRemindTimerDeleteRow
} from '@/utils/remind-timer-table-kbd'

export default {
  data() {
    return {
      remindTimerKbdRow: -1,
      remindTimerKbdCol: 0
    }
  },
  watch: {
    dialogVisible(val) {
      if (val) {
        this.$nextTick(() => this.$_attachRemindTimerRowKeydown())
      } else {
        this.$_detachRemindTimerRowKeydown()
        this.remindTimerKbdRow = -1
        this.remindTimerKbdCol = 0
      }
    }
  },
  beforeDestroy() {
    this.$_detachRemindTimerRowKeydown()
  },
  methods: {
    $_getRemindTimerTableEl() {
      const table = this.$refs.remindTimerTable
      return (table && table.$el) || null
    },
    /** 强制收起报时弹框内已打开的时间/日期面板（Popper 挂 body，失焦时需主动关） */
    $_closeRemindTimerPickerPanels() {
      const tableEl = this.$_getRemindTimerTableEl()
      if (!tableEl) return
      const scope = tableEl.closest('.personal-remind-timer-dialog') || tableEl
      scope.querySelectorAll('.el-date-editor').forEach((editor) => {
        let vm = editor.__vue__
        while (vm) {
          const name = vm.$options && vm.$options.name
          if ((name === 'ElTimePicker' || name === 'ElDatePicker' || name === 'ElTimeSelect') &&
            vm.pickerVisible && typeof vm.handleClose === 'function') {
            vm.handleClose()
            break
          }
          vm = vm.$parent
        }
      })
    },
    $_buildFieldHints() {
      if (!this.dialogVisible) return null
      const tableEl = this.$_getRemindTimerTableEl()
      if (!tableEl) return null
      const map = {}
      const pending = []
      const rows = tableEl.querySelectorAll('.el-table__body tbody tr')
      rows.forEach((tr, rowIndex) => {
        if (rowIndex > 8) return
        const digit = String(rowIndex + 1)
        const stops = getRemindTimerRowStops(tr)
        if (!stops.length) return
        const anchor = tr.querySelector('td.remind-switch-col .cell') || tr
        map[digit] = {
          run: () => {
            this.remindTimerKbdRow = rowIndex
            this.remindTimerKbdCol = 0
            this.$_focusControl(stops[0])
          }
        }
        pending.push({ anchor, letter: digit })
      })
      const addBtn = this.$refs.remindAddBtn
      const addEl = (addBtn && addBtn.$el) || tableEl.querySelector('.remind-add-btn')
      if (addEl) {
        const addRun = () => this.addTimer()
        map['+'] = { run: addRun }
        map['='] = { run: addRun }
        pending.push({ anchor: addEl, letter: '+' })
      }
      return { map, pending }
    },
    $_attachRemindTimerRowKeydown() {
      if (this.$_remindTimerRowKeydownBound) return
      this.$_remindTimerRowKeydownBound = true
      this.$_onRemindTimerRowKeydown = (e) => this.$_handleRemindTimerRowKeydown(e)
      document.addEventListener('keydown', this.$_onRemindTimerRowKeydown, true)
    },
    $_detachRemindTimerRowKeydown() {
      if (!this.$_remindTimerRowKeydownBound) return
      this.$_remindTimerRowKeydownBound = false
      document.removeEventListener('keydown', this.$_onRemindTimerRowKeydown, true)
      this.$_onRemindTimerRowKeydown = null
    },
    $_shouldDeferRemindTimerRowKeys() {
      return hasBlockingUiLayer({
        excludeDefectFormDrawer: true,
        excludeHandleDefectDrawer: true,
        excludeDefectToolDialog: true
      })
    },
    $_syncRemindTimerKbdFromFocus() {
      const tableEl = this.$_getRemindTimerTableEl()
      const active = document.activeElement
      if (!tableEl || !active || !tableEl.contains(active)) return false
      const rowIndex = resolveRemindTimerRowIndex(tableEl, active)
      if (rowIndex < 0) return false
      const rows = tableEl.querySelectorAll('.el-table__body tbody tr')
      const tr = rows[rowIndex]
      const stops = getRemindTimerRowStops(tr)
      this.remindTimerKbdRow = rowIndex
      this.remindTimerKbdCol = Math.max(0, resolveRemindTimerColIndex(stops, active))
      return true
    },
    $_focusRemindTimerRowControl(rowIndex, colIndex) {
      const tableEl = this.$_getRemindTimerTableEl()
      if (!tableEl) return false
      const rows = tableEl.querySelectorAll('.el-table__body tbody tr')
      const tr = rows[rowIndex]
      if (!tr) return false
      const stops = getRemindTimerRowStops(tr)
      if (!stops.length) return false
      const idx = Math.max(0, Math.min(colIndex, stops.length - 1))
      this.remindTimerKbdRow = rowIndex
      this.remindTimerKbdCol = idx
      closeDropdownsOnBlur(stops[idx])
      this.$_focusControl(stops[idx])
      return true
    },
    $_afterRemindTimerRowRemoved(removedIndex) {
      const tableEl = this.$_getRemindTimerTableEl()
      if (!tableEl) return
      const count = tableEl.querySelectorAll('.el-table__body tbody tr').length
      if (!count) {
        this.remindTimerKbdRow = -1
        this.remindTimerKbdCol = 0
        return
      }
      const nextRow = Math.min(removedIndex, count - 1)
      this.$nextTick(() => {
        this.$_focusRemindTimerRowControl(nextRow, 0)
      })
    },
    $_handleRemindTimerRowKeydown(e) {
      if (!this.dialogVisible || e.isComposing) return
      if (e.metaKey || e.ctrlKey || e.altKey) return
      const tableEl = this.$_getRemindTimerTableEl()
      const active = document.activeElement
      if (!tableEl || !active || !tableEl.contains(active)) return

      if (this.$_shouldDeferRemindTimerRowKeys()) return

      if (e.key === 'Delete' || e.key === 'Backspace') {
        if (!shouldRemindTimerDeleteRow(active)) return
        if (!this.$_syncRemindTimerKbdFromFocus() && this.remindTimerKbdRow < 0) return
        const rowIndex = this.remindTimerKbdRow
        if (rowIndex < 0) return
        e.preventDefault()
        e.stopPropagation()
        this.removeTimer(rowIndex)
        this.$_afterRemindTimerRowRemoved(rowIndex)
      }
    }
  }
}
