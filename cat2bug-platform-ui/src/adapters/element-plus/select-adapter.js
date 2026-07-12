/**
 * Element Plus select DOM / component instance adapter.
 */

export const SELECT_SELECTORS = {
  root: '.el-select',
  dropdown: '.el-select-dropdown',
  item: '.el-select-dropdown__item:not(.is-disabled)',
  popper: '.el-select-dropdown.el-popper'
}

export const SELECT_COMPONENT_NAMES = new Set(['ElSelect'])

export function isSelectComponentVm(vm) {
  if (!vm || !vm.$options) return false
  const name = vm.$options.name || vm.$options.componentName
  return SELECT_COMPONENT_NAMES.has(name)
}

/** Resolve ElSelect instance from host element (often .el-select root). */
export function resolveSelectComponent(hostEl) {
  if (!hostEl) return null
  let el = hostEl
  if (hostEl.$el) {
    const fromVm = walkSelectVmChain(hostEl)
    if (fromVm) return fromVm
    el = hostEl.$el
  }
  if (el.classList && el.classList.contains('el-select') && el.__vue__) {
    const vm = el.__vue__
    if (isSelectComponentVm(vm)) return vm
  }
  while (el) {
    const vm = el.__vue__
    if (vm && isSelectComponentVm(vm)) return vm
    el = el.parentElement
  }
  return null
}

function walkSelectVmChain(vm) {
  let cur = vm
  while (cur) {
    if (isSelectComponentVm(cur)) return cur
    cur = cur.$parent
  }
  return null
}

export function isSelectOpen(vm) {
  return !!(vm && vm.visible)
}

export function closeSelect(vm) {
  if (!vm || !vm.visible) return false
  if (typeof vm.handleClose === 'function') {
    vm.handleClose()
    return true
  }
  vm.visible = false
  return true
}

export function getSelectDropdownEl(vm) {
  if (!vm) return null
  const popperRef = vm.$refs && vm.$refs.popper
  const popperEl = popperRef && (popperRef.$el || popperRef)
  if (!popperEl) return null
  if (popperEl.classList && popperEl.classList.contains('el-select-dropdown')) {
    return popperEl
  }
  return popperEl.querySelector(SELECT_SELECTORS.dropdown) || popperEl
}

export function querySelectDropdownItems(dropdownEl) {
  if (!dropdownEl || typeof dropdownEl.querySelectorAll !== 'function') return []
  return Array.from(dropdownEl.querySelectorAll(SELECT_SELECTORS.item))
}
