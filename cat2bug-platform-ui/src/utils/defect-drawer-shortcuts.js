/**
 * 缺陷新建/编辑抽屉：全局 Cmd/Ctrl+Enter 保存、Cmd/Ctrl+B 关闭。
 * 单例监听 + DOM/栈双路解析栈顶抽屉，避免多实例 EditDefectDialog 注册错乱。
 */
import { isNativeFilePickerOpen } from '@/utils/native-file-picker'

const stack = []
let installed = false

const FORM_DRAWER_HEADER_SEL = '.defect-add-header, .defect-edit-form-header'

function isModifierKeyEvent(e) {
  if (!e) return false
  const k = e.key
  return k === 'Control' || k === 'Meta' || k === 'OS' ||
    e.keyCode === 17 || e.keyCode === 91 || e.keyCode === 93
}

function isCloseShortcutKey(e) {
  if (!e || e.altKey) return false
  if (!(e.metaKey || e.ctrlKey)) return false
  const k = e.key
  return k === 'B' || k === 'b'
}

function isVisibleEl(el) {
  if (!el || !el.getClientRects) return false
  const st = window.getComputedStyle(el)
  if (st.display === 'none' || st.visibility === 'hidden') return false
  return el.getClientRects().length > 0
}

function findDrawerVmFromNode(node) {
  let el = node
  while (el) {
    let vm = el.__vue__
    while (vm) {
      const name = vm.$options && vm.$options.name
      if (name === 'AddDefect' || name === 'EditDefectDialog') return vm
      vm = vm.$parent
    }
    el = el.parentElement
  }
  return null
}

/** 从可见 DOM 找最上层新建/编辑表单抽屉 VM（排除 HandleDefect 详情抽屉） */
function findTopFormDrawerVmFromDom() {
  const candidates = []
  document.querySelectorAll('.el-drawer__wrapper').forEach((wrapper) => {
    if (!isVisibleEl(wrapper)) return
    const header = wrapper.querySelector(FORM_DRAWER_HEADER_SEL)
    if (!header || !isVisibleEl(header)) return
    const accent = wrapper.querySelector('.defect-drawer-accent')
    if (!accent) return
    const z = parseInt(window.getComputedStyle(wrapper).zIndex, 10) || 0
    const vm = findDrawerVmFromNode(accent) || findDrawerVmFromNode(header)
    if (vm) candidates.push({ vm, z })
  })
  if (!candidates.length) return null
  candidates.sort((a, b) => b.z - a.z)
  return candidates[0].vm
}

function resolveDrawerVm(preferredVm) {
  if (preferredVm && preferredVm.visible) return preferredVm
  for (let i = stack.length - 1; i >= 0; i--) {
    const vm = stack[i]
    if (vm && vm.visible) return vm
  }
  return findTopFormDrawerVmFromDom()
}

export function findTopFormDrawerVm() {
  return resolveDrawerVm()
}

function invokeSave(vm) {
  if (typeof vm.shortcutSave === 'function') vm.shortcutSave()
  else if (typeof vm.submitForm === 'function') vm.submitForm()
}

function invokeClose(vm) {
  if (!vm) return false
  if (typeof vm.shortcutClose === 'function') {
    vm.shortcutClose()
    return true
  }
  if (typeof vm.cancel === 'function') {
    vm.cancel()
    return true
  }
  return false
}

/** 供 form-field-hints 等 mixin 主动触发关闭 */
export function tryCloseDefectDrawer(preferredVm, e) {
  if (preferredVm && typeof preferredVm.$_invokeDrawerShortcutClose === 'function') {
    return preferredVm.$_invokeDrawerShortcutClose(e)
  }
  const vm = resolveDrawerVm(preferredVm)
  if (!vm || !e || !isCloseShortcutKey(e)) return false
  e.preventDefault()
  e.stopImmediatePropagation()
  return invokeClose(vm)
}

function onKeyup(e) {
  if (!isModifierKeyEvent(e)) return
  document.querySelectorAll('.defect-page').forEach((el) => {
    const vm = el.__vue__
    if (vm && typeof vm.$_hidePageActionHints === 'function') {
      vm.$_hidePageActionHints()
    }
  })
  const drawer = findTopFormDrawerVm()
  if (drawer && typeof drawer.$_hideFieldHints === 'function') {
    drawer.$_hideFieldHints()
  }
}

function onKeydown(e) {
  if (e.isComposing) return

  const vm = resolveDrawerVm()
  if (!vm) return

  if ((e.metaKey || e.ctrlKey) && !e.altKey && (e.key === 'Enter' || e.keyCode === 13)) {
    e.preventDefault()
    e.stopImmediatePropagation()
    invokeSave(vm)
    return
  }

  if (isCloseShortcutKey(e)) {
    e.preventDefault()
    e.stopImmediatePropagation()
    if (typeof vm.$_invokeDrawerShortcutClose === 'function') {
      vm.$_invokeDrawerShortcutClose(e)
    } else {
      invokeClose(vm)
    }
  }
}

function onWindowBlur() {
  if (isNativeFilePickerOpen()) return
}

/** 每次抽屉打开时 bump 到 capture 链最前（最后注册 = 最先执行） */
function ensureInstalled() {
  if (installed) {
    window.removeEventListener('keydown', onKeydown, true)
    window.removeEventListener('keyup', onKeyup, true)
    document.removeEventListener('keydown', onKeydown, true)
    document.removeEventListener('keyup', onKeyup, true)
  }
  installed = true
  window.addEventListener('keydown', onKeydown, true)
  window.addEventListener('keyup', onKeyup, true)
  document.addEventListener('keydown', onKeydown, true)
  document.addEventListener('keyup', onKeyup, true)
  window.addEventListener('blur', onWindowBlur)
}

function ensureUninstalled() {
  if (stack.length > 0) return
}

export function registerDefectDrawerShortcuts(vm) {
  if (!vm) return
  for (let i = stack.length - 1; i >= 0; i--) {
    if (!stack[i] || !stack[i].visible) stack.splice(i, 1)
  }
  const i = stack.indexOf(vm)
  if (i >= 0) stack.splice(i, 1)
  stack.push(vm)
  ensureInstalled()
}

export function unregisterDefectDrawerShortcuts(vm) {
  const i = stack.indexOf(vm)
  if (i >= 0) stack.splice(i, 1)
  ensureUninstalled()
}

export function bootDefectDrawerShortcuts() {
  ensureInstalled()
}
