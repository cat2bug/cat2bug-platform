/**
 * 下拉 / 浮层失去焦点后自动收起：焦点不在触发器与 popper 内时关闭可见的下拉。
 * 覆盖 Element UI dropdown / select / popover / cascader / autocomplete / date-picker，
 * 以及成员、交付物、显示字段等自定义 popper。
 */

const POPPER_CONTENT_SELECTORS = [
  '.el-dropdown-menu',
  '.el-select-dropdown',
  '.el-picker-panel',
  '.el-time-panel',
  '.el-cascader-panel',
  '.el-cascader-menu',
  '.el-autocomplete-suggestion',
  '.select-project-member-popover',
  '.select-module-popover',
  '.defect-column-picker-popover',
  '.defect-excel-column-picker-popover'
]

const DROPDOWN_RELATED_SELECTORS = [
  '.el-dropdown',
  '.el-select',
  '.el-cascader',
  '.el-autocomplete',
  '.el-date-editor',
  '.el-time-editor',
  '.el-time-panel',
  '.cat2bug-combo-focus-target',
  '.el-popper',
  '.el-popover',
  '.el-dropdown-menu',
  '.el-select-dropdown',
  '.el-picker-panel',
  '.el-cascader-panel',
  '.el-autocomplete-suggestion',
  '.select-project-member-popover',
  '.select-module-popover',
  '.defect-column-picker-popover',
  '.defect-excel-column-picker-popover'
]

let blurCloseTimer = null

function isVisibleLayer(el) {
  if (!el || !el.getBoundingClientRect) return false
  if (el.getAttribute('aria-hidden') === 'true') return false
  const style = window.getComputedStyle(el)
  if (style.display === 'none' || style.visibility === 'hidden') return false
  const rect = el.getBoundingClientRect()
  return rect.width > 0 && rect.height > 0
}

function containsNode(root, node) {
  return !!(root && node && (root === node || root.contains(node)))
}

export function isDropdownRelatedElement(el) {
  if (!el || !el.closest) return false
  return DROPDOWN_RELATED_SELECTORS.some((sel) => el.closest(sel))
}

function getVisiblePopperRoots() {
  const roots = new Set()
  POPPER_CONTENT_SELECTORS.forEach((sel) => {
    document.querySelectorAll(sel).forEach((el) => {
      if (!isVisibleLayer(el)) return
      roots.add(el.closest('.el-popper') || el)
    })
  })
  return Array.from(roots)
}

function findElementByAriaDescribedby(id) {
  if (!id) return null
  const nodes = document.querySelectorAll('[aria-describedby]')
  for (let i = 0; i < nodes.length; i++) {
    if (nodes[i].getAttribute('aria-describedby') === id) return nodes[i]
  }
  return null
}

function resolveDropdownVm(startEl) {
  let rootEl = startEl
  if (rootEl && rootEl.classList) {
    if (rootEl.classList.contains('el-time-panel') || rootEl.classList.contains('el-picker-panel')) {
      const editor = rootEl.closest('.el-date-editor')
      if (editor) rootEl = editor
    }
  }
  let vm = rootEl && rootEl.__vue__
  if (!vm && rootEl && rootEl.id) {
    const ref = findElementByAriaDescribedby(rootEl.id)
    if (ref) vm = ref.__vue__
  }
  while (vm) {
    const name = vm.$options && vm.$options.name
    if (name === 'ElDropdown' || name === 'ElSelect' || name === 'ElPopover' ||
      name === 'ElCascader' || name === 'ElAutocomplete' || name === 'ElDatePicker' ||
      name === 'ElTimePicker' || name === 'ElTimeSelect') {
      return vm
    }
    vm = vm.$parent
  }
  return null
}

function getDropdownTriggerEl(vm) {
  if (!vm) return null
  const name = vm.$options && vm.$options.name
  if (name === 'ElDropdown') {
    return vm.$el
  }
  if (name === 'ElSelect') {
    const ref = vm.$refs.reference
    return (ref && ref.$el) || ref || vm.$el
  }
  if (name === 'ElPopover') {
    const ref = vm.referenceElm || vm.reference || (vm.$refs && vm.$refs.reference)
    if (!ref) return vm.$el
    return ref.$el || ref
  }
  if (name === 'ElCascader' || name === 'ElAutocomplete') {
    return vm.$el
  }
  if (name === 'ElDatePicker' || name === 'ElTimePicker' || name === 'ElTimeSelect') {
    return vm.$el
  }
  return vm.$el
}

function isDropdownOpen(vm) {
  if (!vm) return false
  const name = vm.$options && vm.$options.name
  if (name === 'ElDropdown') return !!vm.visible
  if (name === 'ElSelect') return !!vm.visible
  if (name === 'ElPopover') return !!vm.showPopper
  if (name === 'ElCascader') return !!vm.dropDownVisible
  if (name === 'ElAutocomplete') return !!vm.suggestionVisible
  if (name === 'ElDatePicker' || name === 'ElTimePicker' || name === 'ElTimeSelect') {
    return !!vm.pickerVisible
  }
  return false
}

function closeDropdownVm(vm) {
  if (!vm || !isDropdownOpen(vm)) return
  const name = vm.$options && vm.$options.name
  if (name === 'ElDropdown' && typeof vm.hide === 'function') {
    vm.hide()
    return
  }
  if (name === 'ElSelect' && typeof vm.handleClose === 'function') {
    vm.handleClose()
    return
  }
  if (name === 'ElPopover' && typeof vm.doClose === 'function') {
    vm.doClose()
    return
  }
  if (name === 'ElCascader' && typeof vm.toggleDropDownVisible === 'function') {
    vm.toggleDropDownVisible(false)
    return
  }
  if (name === 'ElAutocomplete' && typeof vm.close === 'function') {
    vm.close()
    return
  }
  if ((name === 'ElDatePicker' || name === 'ElTimePicker' || name === 'ElTimeSelect') &&
    typeof vm.handleClose === 'function') {
    vm.handleClose()
  }
}

const PICKER_PANEL_SELECTORS = ['.el-time-panel', '.el-picker-panel']

function isFocusInsideVisiblePickerPanel(activeEl) {
  if (!activeEl) return false
  for (let i = 0; i < PICKER_PANEL_SELECTORS.length; i++) {
    const panels = document.querySelectorAll(PICKER_PANEL_SELECTORS[i])
    for (let j = 0; j < panels.length; j++) {
      const panel = panels[j]
      if (isVisibleLayer(panel) && containsNode(panel, activeEl)) return true
    }
  }
  return false
}

/** 日期/时间面板常 append 到 body；单时间选择器无 is-active，需遍历 .el-date-editor 查 pickerVisible */
function closeOpenDateEditorsOnBlur(activeEl) {
  document.querySelectorAll('.el-date-editor').forEach((editor) => {
    const vm = resolveDropdownVm(editor)
    if (!vm || !isDropdownOpen(vm)) return
    const trigger = getDropdownTriggerEl(vm)
    if (containsNode(trigger, activeEl)) return
    if (isFocusInsideVisiblePickerPanel(activeEl)) return
    closeDropdownVm(vm)
  })
}

export function closeDropdownsOnBlur(activeEl = document.activeElement) {
  closeOpenDateEditorsOnBlur(activeEl)
  getVisiblePopperRoots().forEach((popperRoot) => {
    const vm = resolveDropdownVm(popperRoot)
    if (!vm || !isDropdownOpen(vm)) return
    const trigger = getDropdownTriggerEl(vm)
    if (containsNode(trigger, activeEl)) return
    if (containsNode(popperRoot, activeEl)) return
    closeDropdownVm(vm)
  })
}

export function scheduleDropdownBlurClose() {
  if (blurCloseTimer) clearTimeout(blurCloseTimer)
  blurCloseTimer = setTimeout(() => {
    blurCloseTimer = null
    closeDropdownsOnBlur()
  }, 0)
}

export function onDropdownBlurFocusOut(e) {
  if (!isDropdownRelatedElement(e.target)) return
  scheduleDropdownBlurClose()
}

export function onDropdownBlurFocusIn() {
  scheduleDropdownBlurClose()
}
