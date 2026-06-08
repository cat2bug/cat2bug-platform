/**
 * 日期选择器键盘：获焦不自动展开面板；↓/↑/Enter/Space 或鼠标点击后再展开。
 * 范围选择器另有 handleRangeClick / focus() 等入口，须一并拦截。
 */

export const DATE_PICKER_OPEN_ON_FOCUS_CLASS = 'cat2bug-date-open-on-focus'

import {
  isDatePanelNavKeyEvent,
  runDatePanelNavByKeyCode,
  resetDatePickerPanelHintModifier
} from '@/utils/date-picker-panel-hints'

const OPEN_KEY_CODES = new Set([13, 32, 38, 40]) // Enter, Space, Up, Down
const ARROW_KEY_CODES = new Set([37, 38, 39, 40])
const DAY_ARROW_STEP = { 38: -7, 40: 7, 37: -1, 39: 1 }
const RANGE_KBD_FOCUS_CLASS = 'cat2bug-date-kbd-current'

function stripDay(date) {
  const d = new Date(date.getTime())
  d.setHours(0, 0, 0, 0)
  return d
}

function isVisibleEl(el) {
  if (!el || !el.getBoundingClientRect) return false
  if (el.getAttribute('aria-hidden') === 'true') return false
  const style = window.getComputedStyle(el)
  if (style.display === 'none' || style.visibility === 'hidden') return false
  const rect = el.getBoundingClientRect()
  return rect.width > 0 && rect.height > 0
}

export function isRangeDatePicker(pickerVm) {
  if (!pickerVm) return false
  if (typeof pickerVm.ranged === 'boolean') return pickerVm.ranged
  const type = pickerVm.type
  return typeof type === 'string' && type.indexOf('range') > -1
}

/** 日期面板已展开（供查询区 ←/→ 让路） */
export function isDatePickerPanelOpen() {
  const editors = document.querySelectorAll('.el-range-editor.is-active, .el-date-editor.is-active')
  for (let i = 0; i < editors.length; i++) {
    const vm = resolveDatePickerVm(editors[i])
    if (vm && vm.pickerVisible) return true
  }
  return Array.from(document.querySelectorAll('.el-picker-panel')).some(isVisibleEl)
}

function findDateTableVms(rootVm) {
  const out = []
  const stack = [rootVm]
  while (stack.length) {
    const vm = stack.pop()
    if (!vm) continue
    if (typeof vm.getDateOfCell === 'function' && Array.isArray(vm.rows)) out.push(vm)
    if (vm.$children) {
      for (let i = 0; i < vm.$children.length; i++) stack.push(vm.$children[i])
    }
  }
  return out
}

export function clearRangePanelFocusVisual(panel) {
  if (!panel || !panel.$el) return
  panel.$el.querySelectorAll(`.${RANGE_KBD_FOCUS_CLASS}`).forEach((el) => {
    el.classList.remove(RANGE_KBD_FOCUS_CLASS)
  })
}

/** Element UI date-table tbody 首行可能无 td（占位行），须与 table.rows 对齐 */
export function getDateTableBodyRows(tableEl) {
  if (!tableEl) return []
  return Array.from(tableEl.querySelectorAll('tbody tr')).filter((tr) => tr.querySelectorAll('td').length > 0)
}

export function syncRangePanelFocusVisual(panel, focusDate) {
  clearRangePanelFocusVisual(panel)
  if (!panel || !focusDate) return false
  const targetTs = stripDay(focusDate).getTime()
  let matched = false
  findDateTableVms(panel).forEach((table) => {
    if (!table.$el || !table.rows) return
    const trs = getDateTableBodyRows(table.$el)
    for (let r = 0; r < table.rows.length; r++) {
      const tr = trs[r]
      if (!tr) continue
      const tds = tr.querySelectorAll('td')
      const row = table.rows[r]
      for (let c = 0; c < row.length; c++) {
        const cell = row[c]
        if (!cell || cell.type === 'week' || cell.disabled) continue
        let cellDate
        try {
          cellDate = table.getDateOfCell(r, c)
        } catch (e) {
          continue
        }
        if (!cellDate || stripDay(cellDate).getTime() !== targetTs) continue
        const td = tds[c]
        if (td) {
          td.classList.add(RANGE_KBD_FOCUS_CLASS)
          matched = true
        }
      }
    }
  })
  return matched
}

function nextMonth(date) {
  const d = new Date(date.getTime())
  d.setDate(1)
  d.setMonth(d.getMonth() + 1)
  return d
}

function getInitialRangeFocusDate(panel) {
  if (panel.minDate) return new Date(panel.minDate.getTime())
  if (panel.rangeState && panel.rangeState.selecting && panel.rangeState.endDate) {
    return new Date(panel.rangeState.endDate.getTime())
  }
  if (Array.isArray(panel.value) && panel.value[0]) return new Date(panel.value[0])
  if (panel.leftDate) return new Date(panel.leftDate.getTime())
  return new Date()
}

/** Element UI daterange 面板无 handleKeydown，补全方向键与 Enter/Space 选日 */
export function ensureDateVisibleOnRangePanel(panel, date) {
  if (!panel || !date || !panel.leftDate || !panel.rightDate) return
  const focusYm = date.getFullYear() * 12 + date.getMonth()
  const leftYm = panel.leftDate.getFullYear() * 12 + panel.leftDate.getMonth()
  const rightYm = panel.rightDate.getFullYear() * 12 + panel.rightDate.getMonth()
  if (focusYm >= leftYm && focusYm <= rightYm) return
  if (!panel.unlinkPanels) {
    panel.leftDate = new Date(date.getFullYear(), date.getMonth(), 1)
    panel.rightDate = nextMonth(panel.leftDate)
    return
  }
  if (focusYm < leftYm) {
    panel.leftDate = new Date(date.getFullYear(), date.getMonth(), 1)
  } else if (focusYm > rightYm) {
    panel.rightDate = new Date(date.getFullYear(), date.getMonth(), 1)
  }
}

export function moveRangeFocusDateByKey(focusDate, keyCode, disabledDate) {
  const step = DAY_ARROW_STEP[keyCode]
  if (!step || !focusDate) return focusDate
  const year = 3.1536e10
  const anchor = focusDate.getTime()
  const next = new Date(focusDate.getTime())
  while (Math.abs(anchor - next.getTime()) <= year) {
    next.setDate(next.getDate() + step)
    if (typeof disabledDate === 'function' && disabledDate(next)) continue
    return new Date(next.getTime())
  }
  return focusDate
}

function previewRangeEndDate(panel, endDate) {
  const payload = {
    minDate: panel.minDate,
    maxDate: panel.maxDate,
    rangeState: {
      selecting: true,
      endDate,
      row: null,
      column: null
    }
  }
  if (typeof panel.handleChangeRange === 'function') {
    panel.handleChangeRange(payload)
  } else {
    panel.rangeState = payload.rangeState
  }
}

function markRangeSelectingStart(panel) {
  if (!panel.rangeState) {
    panel.rangeState = { selecting: false, endDate: null, row: null, column: null }
  }
  panel.rangeState.selecting = true
  panel.rangeState.endDate = null
}

/** 是否处于选结束日阶段（键盘直调 handleRangePick 不会走 date-table 的 selecting 赋值） */
function isPickingRangeEnd(panel) {
  return !!(panel && panel.minDate && !panel.maxDate)
}

function pickRangeFocusDate(panel, date, pickerVm) {
  if (!panel || !date || typeof panel.handleRangePick !== 'function') return false
  if (!isPickingRangeEnd(panel)) {
    panel.handleRangePick({ minDate: date, maxDate: null }, false)
    markRangeSelectingStart(panel)
    panel._cat2bugKbdFocusDate = new Date(date.getTime())
    return false
  }
  const minDate = panel.minDate
  if (!minDate) return false
  if (pickerVm) pickerVm._cat2bugKbdPickHandled = true
  if (date >= minDate) {
    panel.handleRangePick({ minDate, maxDate: date }, true)
  } else {
    panel.handleRangePick({ minDate: date, maxDate: minDate }, true)
  }
  if (panel.rangeState) panel.rangeState.selecting = false
  return true
}

function finalizeRangeKeyboardSelection(pickerVm) {
  if (!pickerVm || pickerVm._cat2bugFinalizePending) return
  pickerVm._cat2bugFinalizePending = true
  pickerVm.userInput = null
  const inputs = pickerVm.refInput || []
  const refocusEl = inputs[inputs.length - 1] || inputs[0]
  const finish = () => {
    pickerVm._cat2bugFinalizePending = false
    if (pickerVm.pickerVisible) {
      closeDatePickerPanelKeepFocus(pickerVm, refocusEl)
      return
    }
    if (refocusEl && document.activeElement !== refocusEl && typeof refocusEl.focus === 'function') {
      try {
        refocusEl.focus({ preventScroll: false })
      } catch (e) {
        refocusEl.focus()
      }
    }
  }
  // Element handleRangePick 在预览后可能 early-return，且内部有 setTimeout(10) 写回 min/max
  pickerVm.$nextTick(() => setTimeout(finish, 20))
}

export function handleDateRangePanelKeydown(pickerVm, event) {
  if (event && event.defaultPrevented) return false
  const panel = pickerVm && pickerVm.picker
  if (!panel || !pickerVm.pickerVisible || !isRangeDatePicker(pickerVm)) return false
  if (typeof panel.handleKeydown === 'function') return false

  const keyCode = event && event.keyCode
  if (keyCode == null) return false

  if (isDatePanelNavKeyEvent(event)) {
    if (runDatePanelNavByKeyCode(keyCode, panel, event.key)) {
      pickerVm.$nextTick(() => {
        pickerVm.$nextTick(() => {
          if (panel._cat2bugKbdFocusDate) {
            syncRangePanelFocusVisual(panel, panel._cat2bugKbdFocusDate)
          }
        })
      })
      return true
    }
    return false
  }

  if (ARROW_KEY_CODES.has(keyCode)) {
    if (!panel._cat2bugKbdFocusDate) {
      panel._cat2bugKbdFocusDate = getInitialRangeFocusDate(panel)
    }
    const next = moveRangeFocusDateByKey(panel._cat2bugKbdFocusDate, keyCode, panel.disabledDate)
    panel._cat2bugKbdFocusDate = next
    ensureDateVisibleOnRangePanel(panel, next)
    if (panel.rangeState && panel.rangeState.selecting) {
      previewRangeEndDate(panel, next)
    }
    pickerVm.$nextTick(() => {
      pickerVm.$nextTick(() => syncRangePanelFocusVisual(panel, next))
    })
    return true
  }

  if (keyCode === 13 || keyCode === 32) {
    const target = event.target
    if (target && target.closest && target.closest('.el-picker-panel__shortcut')) return false
    if (!panel._cat2bugKbdFocusDate) {
      panel._cat2bugKbdFocusDate = getInitialRangeFocusDate(panel)
    }
    const completed = pickRangeFocusDate(panel, panel._cat2bugKbdFocusDate, pickerVm)
    if (completed) {
      clearRangePanelFocusVisual(panel)
      finalizeRangeKeyboardSelection(pickerVm)
    } else {
      pickerVm.$nextTick(() => {
        syncRangePanelFocusVisual(panel, panel._cat2bugKbdFocusDate)
      })
    }
    return true
  }

  return false
}

function resolveActiveRangePickerVm() {
  const activeEditor = document.querySelector('.el-range-editor.is-active')
  if (activeEditor) {
    const vm = resolveDatePickerVm(activeEditor)
    if (vm && vm.pickerVisible && isRangeDatePicker(vm)) return vm
  }
  const editors = document.querySelectorAll('.el-range-editor')
  for (let i = 0; i < editors.length; i++) {
    const vm = resolveDatePickerVm(editors[i])
    if (vm && vm.pickerVisible && isRangeDatePicker(vm)) return vm
  }
  return null
}

export function handleActiveDateRangeKeydown(event) {
  if (event && (event.metaKey || event.ctrlKey)) return false
  if (event && event.altKey) return false
  const pickerVm = resolveActiveRangePickerVm()
  if (!pickerVm) return false
  wrapDatePickerKeyboard(pickerVm)
  if (!handleDateRangePanelKeydown(pickerVm, event)) return false
  event.preventDefault()
  event.stopPropagation()
  return true
}

function initRangePanelKeyboardFocus(pickerVm) {
  const panel = pickerVm && pickerVm.picker
  if (!panel || !isRangeDatePicker(pickerVm)) return
  panel._cat2bugKbdFocusDate = getInitialRangeFocusDate(panel)
  ensureDateVisibleOnRangePanel(panel, panel._cat2bugKbdFocusDate)
  pickerVm.$nextTick(() => syncRangePanelFocusVisual(panel, panel._cat2bugKbdFocusDate))
}

function closeDatePickerPanel(pickerVm) {
  if (!pickerVm || !pickerVm.pickerVisible) return
  if (pickerVm.picker) clearRangePanelFocusVisual(pickerVm.picker)
  if (typeof pickerVm.handleClose === 'function') pickerVm.handleClose()
  else pickerVm.pickerVisible = false
  resetDatePickerPanelHintModifier()
}

export function resolveDatePickerVm(fromEl) {
  if (!fromEl || !fromEl.closest) return null
  const editor = fromEl.closest('.el-date-editor')
  if (!editor) return null

  const candidates = []
  if (editor.__vue__) candidates.push(editor.__vue__)
  let vm = fromEl.__vue__
  while (vm) {
    if (candidates.indexOf(vm) === -1) candidates.push(vm)
    vm = vm.$parent
  }

  for (let i = 0; i < candidates.length; i++) {
    const node = candidates[i]
    if (typeof node.handleFocus === 'function' && 'pickerVisible' in node) return node
  }
  return null
}

export function isDatePickerFocusEl(el) {
  return !!(el && resolveDatePickerVm(el))
}

function shouldOpenPanelOnFocus(pickerVm) {
  if (!pickerVm || pickerVm._cat2bugSuppressPickerOpen) return false
  if (pickerVm._cat2bugSilentFocusDepth > 0) return false
  if (pickerVm._cat2bugDateOpenOnFocus) return true
  const el = pickerVm.$el
  return !!(el && el.classList && el.classList.contains(DATE_PICKER_OPEN_ON_FOCUS_CLASS))
}

function beginSilentFocus(pickerVm, options) {
  if (!pickerVm) return
  pickerVm._cat2bugSilentFocusDepth = (pickerVm._cat2bugSilentFocusDepth || 0) + 1
  pickerVm._cat2bugSuppressPickerOpen = true
  if (options && options.closePanelOnEnd) {
    pickerVm._cat2bugSilentFocusClosePanel = true
  }
}

function endSilentFocus(pickerVm, inputEl) {
  if (!pickerVm) return
  pickerVm._cat2bugSilentFocusDepth = Math.max(0, (pickerVm._cat2bugSilentFocusDepth || 1) - 1)
  if (pickerVm._cat2bugSilentFocusDepth > 0) return
  const shouldClose = !!pickerVm._cat2bugSilentFocusClosePanel
  pickerVm._cat2bugSilentFocusClosePanel = false
  pickerVm._cat2bugSuppressPickerOpen = false
  if (shouldClose && pickerVm.pickerVisible) {
    closeDatePickerPanel(pickerVm)
    resetDatePickerPanelHintModifier()
  }
  if (inputEl && document.activeElement !== inputEl && typeof inputEl.focus === 'function') {
    try {
      inputEl.focus({ preventScroll: false })
    } catch (e) {
      inputEl.focus()
    }
  }
}

/** 关闭面板并保持/恢复输入框焦点（Esc 关闭下拉时使用） */
export function closeDatePickerPanelKeepFocus(pickerVm, preferredInput) {
  if (!pickerVm || !pickerVm.pickerVisible) return false
  const inputs = pickerVm.refInput || []
  let refocusEl = preferredInput
  if (!refocusEl || (inputs.length && inputs.indexOf(refocusEl) < 0)) {
    refocusEl = inputs.find((el) => el === document.activeElement) || inputs[0]
  }
  beginSilentFocus(pickerVm)
  closeDatePickerPanel(pickerVm)
  pickerVm.$nextTick(() => {
    endSilentFocus(pickerVm, refocusEl)
  })
  return true
}

/** 程序化 focus 前调用，使下一次 handleFocus 仍展开面板（如 Excel 计划时间列） */
export function markDatePickerOpenOnFocus(pickerVm) {
  if (!pickerVm) return
  pickerVm._cat2bugDateOpenOnFocus = true
}

/** 查询区 ←/→ 等导航：聚焦日期输入但不展开面板 */
export function focusDatePickerInputSilently(inputEl) {
  if (!inputEl || typeof inputEl.focus !== 'function') return
  const pickerVm = resolveDatePickerVm(inputEl)
  if (pickerVm) wrapDatePickerKeyboard(pickerVm)
  beginSilentFocus(pickerVm)
  try {
    inputEl.focus({ preventScroll: false })
  } catch (e) {
    inputEl.focus()
  }
  if (!pickerVm) return
  pickerVm.$nextTick(() => {
    endSilentFocus(pickerVm, inputEl)
  })
}

function ensureRangePickListener(pickerVm) {
  if (!pickerVm || !isRangeDatePicker(pickerVm) || !pickerVm.picker || pickerVm._cat2bugPickListenerBound) return
  pickerVm._cat2bugPickListenerBound = true
  pickerVm.picker.$on('pick', (date, visible) => {
    if (visible !== false || !date || !Array.isArray(date) || date.length !== 2 || !date[0] || !date[1]) return
    if (pickerVm._cat2bugFinalizePending || pickerVm._cat2bugKbdPickHandled) {
      pickerVm._cat2bugKbdPickHandled = false
      return
    }
    pickerVm.userInput = null
    finalizeRangeKeyboardSelection(pickerVm)
  })
}

export function wrapDatePickerKeyboard(pickerVm) {
  if (!pickerVm || pickerVm._cat2bugDateKbdWrapped) return
  if (typeof pickerVm.handleFocus !== 'function' || !('pickerVisible' in pickerVm)) return
  pickerVm._cat2bugDateKbdWrapped = true
  pickerVm._cat2bugSilentFocusDepth = pickerVm._cat2bugSilentFocusDepth || 0

  const editor = pickerVm.$el
  if (editor && editor.addEventListener) {
    editor.addEventListener('mousedown', () => {
      pickerVm._cat2bugDateOpenOnFocus = true
    }, true)

    editor.addEventListener('focusin', (e) => {
      if (shouldOpenPanelOnFocus(pickerVm)) return
      if (pickerVm.pickerVisible) {
        if (isRangeDatePicker(pickerVm) && e.target &&
          e.target.classList && e.target.classList.contains('el-range-input')) {
          return
        }
        const input = e.target
        beginSilentFocus(pickerVm)
        closeDatePickerPanel(pickerVm)
        pickerVm.$nextTick(() => endSilentFocus(pickerVm, input))
      }
    }, true)
  }

  if (typeof pickerVm.blur === 'function') {
    pickerVm._cat2bugNativeBlur = pickerVm.blur.bind(pickerVm)
    pickerVm.blur = function wrappedDatePickerBlur() {
      if (pickerVm._cat2bugSuppressPickerOpen || pickerVm._cat2bugSilentFocusDepth > 0) return
      pickerVm._cat2bugNativeBlur()
    }
  }

  pickerVm._cat2bugNativeHandleFocus = pickerVm.handleFocus.bind(pickerVm)
  pickerVm.handleFocus = function wrappedDatePickerHandleFocus() {
    if (!shouldOpenPanelOnFocus(pickerVm)) {
      pickerVm.$emit('focus', pickerVm)
      return
    }
    pickerVm._cat2bugDateOpenOnFocus = false
    pickerVm._cat2bugNativeHandleFocus()
    if (pickerVm.pickerVisible && isRangeDatePicker(pickerVm)) {
      pickerVm.$nextTick(() => {
        initRangePanelKeyboardFocus(pickerVm)
        ensureRangePickListener(pickerVm)
      })
    }
  }

  if (typeof pickerVm.handleRangeClick === 'function') {
    pickerVm._cat2bugNativeHandleRangeClick = pickerVm.handleRangeClick.bind(pickerVm)
    pickerVm.handleRangeClick = function wrappedDatePickerHandleRangeClick() {
      if (!shouldOpenPanelOnFocus(pickerVm)) {
        pickerVm.$emit('focus', pickerVm)
        return
      }
      pickerVm._cat2bugDateOpenOnFocus = false
      pickerVm._cat2bugNativeHandleRangeClick()
      if (pickerVm.pickerVisible && isRangeDatePicker(pickerVm)) {
        pickerVm.$nextTick(() => {
          initRangePanelKeyboardFocus(pickerVm)
          ensureRangePickListener(pickerVm)
        })
      }
    }
  }

  if (typeof pickerVm.focus === 'function') {
    pickerVm._cat2bugNativeFocus = pickerVm.focus.bind(pickerVm)
    pickerVm.focus = function wrappedDatePickerFocus() {
      if (pickerVm.ranged) {
        const input = pickerVm.refInput && pickerVm.refInput[0]
        if (input) {
          focusDatePickerInputSilently(input)
          return
        }
      }
      const ref = pickerVm.$refs.reference
      const inner = ref && ref.$refs && ref.$refs.input
      if (inner) {
        focusDatePickerInputSilently(inner)
        return
      }
      pickerVm._cat2bugNativeFocus()
    }
  }

  if (typeof pickerVm.showPicker === 'function') {
    pickerVm._cat2bugNativeShowPicker = pickerVm.showPicker.bind(pickerVm)
    pickerVm.showPicker = function wrappedDatePickerShowPicker() {
      pickerVm._cat2bugNativeShowPicker()
      initRangePanelKeyboardFocus(pickerVm)
      ensureRangePickListener(pickerVm)
    }
  }

  if (typeof pickerVm.mountPicker === 'function' && !pickerVm._cat2bugMountPickerWrapped) {
    pickerVm._cat2bugMountPickerWrapped = true
    pickerVm._cat2bugNativeMountPicker = pickerVm.mountPicker.bind(pickerVm)
    pickerVm.mountPicker = function wrappedDatePickerMountPicker() {
      pickerVm._cat2bugNativeMountPicker()
      ensureRangePickListener(pickerVm)
    }
  }

  if (typeof pickerVm.handleKeydown !== 'function') return
  pickerVm._cat2bugNativeHandleKeydown = pickerVm.handleKeydown.bind(pickerVm)
  pickerVm.handleKeydown = function wrappedDatePickerHandleKeydown(event) {
    const keyCode = event && event.keyCode
    if (keyCode === 27) {
      if (pickerVm.pickerVisible) {
        closeDatePickerPanelKeepFocus(pickerVm, event.target)
        event.preventDefault()
        event.stopPropagation()
        return
      }
      // 面板已关闭：不交给 Element（会 stopPropagation），让查询区 Esc 等上层逻辑处理
      return
    }
    if (pickerVm.pickerVisible && handleDateRangePanelKeydown(pickerVm, event)) {
      event.preventDefault()
      event.stopPropagation()
      return
    }
    if (!pickerVm.pickerVisible && keyCode != null && OPEN_KEY_CODES.has(keyCode)) {
      pickerVm._cat2bugSuppressPickerOpen = false
      pickerVm._cat2bugSilentFocusDepth = 0
      pickerVm._cat2bugSilentFocusClosePanel = false
      pickerVm._cat2bugDateOpenOnFocus = true
      pickerVm.handleFocus()
      if (keyCode === 38 || keyCode === 40 || keyCode === 37 || keyCode === 39) {
        event.preventDefault()
        pickerVm.$nextTick(() => {
          if (handleDateRangePanelKeydown(pickerVm, event)) return
          if (pickerVm.picker && typeof pickerVm.picker.handleKeydown === 'function') {
            pickerVm.picker.handleKeydown(event)
          }
        })
        return
      }
      if (isRangeDatePicker(pickerVm) && (keyCode === 13 || keyCode === 32)) {
        event.preventDefault()
        event.stopPropagation()
        pickerVm.$nextTick(() => {
          initRangePanelKeyboardFocus(pickerVm)
          ensureRangePickListener(pickerVm)
        })
        return
      }
    }
    if (pickerVm.pickerVisible && isRangeDatePicker(pickerVm) && (keyCode === 13 || keyCode === 32)) {
      if (handleDateRangePanelKeydown(pickerVm, event)) {
        event.preventDefault()
        event.stopPropagation()
        return
      }
      event.preventDefault()
      event.stopPropagation()
      return
    }
    pickerVm._cat2bugNativeHandleKeydown(event)
  }
}

export function patchDatePickerKeyboard(root) {
  if (!root || typeof root.querySelectorAll !== 'function') return
  root.querySelectorAll('.el-date-editor').forEach((editor) => {
    const vm = resolveDatePickerVm(editor)
    if (vm) wrapDatePickerKeyboard(vm)
  })
}
