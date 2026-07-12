/**
 * Element Plus dropdown DOM / component instance adapter.
 */

export const DROPDOWN_SELECTORS = {
  host: '.el-dropdown',
  menu: '.el-dropdown-menu',
  buttonGroup: '.el-button-group',
  trigger: 'button, .el-dropdown-link, [role="button"]'
}

export const DROPDOWN_COMPONENT_NAMES = new Set(['ElDropdown'])

export function isElDropdownVm(vm) {
  if (!vm || !vm.$options) return false
  const name = vm.$options.name || vm.$options.componentName
  return DROPDOWN_COMPONENT_NAMES.has(name)
}

export function resolveDropdownComponent(hostEl) {
  if (!hostEl) return null
  let el = hostEl
  while (el) {
    const vm = el.__vue__
    if (vm && isElDropdownVm(vm)) return vm
    el = el.parentElement
  }
  return null
}

export function findDropdownTrigger(hostEl) {
  if (!hostEl) return null
  const direct = hostEl.querySelector(
    `:scope > button, :scope > .el-button, :scope > .el-dropdown-link, :scope > span, :scope > div`
  )
  if (direct && !direct.classList.contains('el-dropdown-menu')) return direct
  return hostEl.querySelector(DROPDOWN_SELECTORS.trigger)
}

export function queryDropdownHosts(root = document) {
  return root.querySelectorAll(DROPDOWN_SELECTORS.host)
}
