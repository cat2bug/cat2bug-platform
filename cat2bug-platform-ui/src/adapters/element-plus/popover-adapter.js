/**
 * Element Plus popover DOM / component instance adapter.
 */

export const POPOVER_SELECTORS = {
  root: '.el-popover',
  reference: '.el-popover__reference',
  referenceWrapper: '.el-popover__reference-wrapper'
}

export const POPOVER_COMPONENT_NAMES = new Set(['ElPopover'])

export function isVisiblePopperEl(el) {
  if (!el || !el.getBoundingClientRect) return false
  if (el.getAttribute('aria-hidden') === 'true') return false
  const style = window.getComputedStyle(el)
  if (style.display === 'none' || style.visibility === 'hidden') return false
  const rect = el.getBoundingClientRect()
  return rect.width > 0 && rect.height > 0
}

export function isPopoverComponentVm(vm) {
  if (!vm || !vm.$options) return false
  const name = vm.$options.name || vm.$options.componentName
  return POPOVER_COMPONENT_NAMES.has(name)
}

/** Resolve ElPopover instance from a DOM node or component vm. */
export function resolvePopoverComponent(fromEl) {
  if (!fromEl) return null
  const node = fromEl.$el || fromEl
  if (node.classList && node.classList.contains('el-popover') && node.__vue__) {
    const vm = node.__vue__
    if (isPopoverComponentVm(vm)) return vm
  }
  let vm = node.__vue__
  while (vm) {
    if (isPopoverComponentVm(vm)) return vm
    vm = vm.$parent
  }
  return null
}

export function isPopoverOpen(vm) {
  return !!(vm && vm.showPopper)
}

export function openPopover(vm) {
  if (!vm) return false
  if (vm.showPopper) return true
  if (typeof vm.doShow === 'function') vm.doShow()
  else vm.showPopper = true
  return true
}

export function closePopover(vm) {
  if (!vm || !vm.showPopper) return false
  if (typeof vm.doClose === 'function') vm.doClose()
  else vm.showPopper = false
  return true
}

export function findPopoverTriggerInContainer(containerEl, focusedEl) {
  const target = focusedEl || document.activeElement
  if (!containerEl || !target || !containerEl.contains(target)) return null
  const host = target.closest(POPOVER_SELECTORS.reference) ||
    target.closest(POPOVER_SELECTORS.referenceWrapper) ||
    (target.matches && target.matches('button') ? target : null)
  if (!host || !containerEl.contains(host)) return null
  const trigger = host.matches('button') ? host : host.querySelector('button')
  if (!trigger || !resolvePopoverComponent(trigger)) return null
  return trigger
}

export function queryVisiblePoppersByClass(className, root = document) {
  const nodes = root.querySelectorAll(`.${className}`)
  const visible = []
  for (let i = 0; i < nodes.length; i++) {
    if (isVisiblePopperEl(nodes[i])) visible.push(nodes[i])
  }
  return visible
}
