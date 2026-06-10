/**
 * 缺陷表单表面：新建/编辑抽屉 + 工具栏操作弹框。
 * 全局 Cmd/Ctrl+Enter 提交、Esc 关闭（抽屉未保存时确认）。
 */
import { isNativeFilePickerOpen } from '@/utils/native-file-picker'
import { isAnyProjectIconPopoverOpenInDom } from '@/utils/project-icon-popover-kbd'
import { hasBlockingUiLayer, shortcutService } from '@/plugins/shortcut/service'

const stack = []
let installed = false

const FORM_DRAWER_HEADER_SEL = '.defect-add-header, .defect-edit-form-header, .case-add-header, .case-search-header'

const FORM_DRAWER_VM_NAMES = new Set(['AddDefect', 'EditDefectDialog', 'AddCase'])
const DEFECT_TOOL_DIALOG_VM_NAMES = new Set([
  'AssignDialog',
  'RepairDialog',
  'RejectDialog',
  'PassDialog',
  'CloseDialog',
  'OpenDialog'
])

const STATISTIC_DIALOG_VM_NAMES = new Set([
  'PersonalRemindTimer',
  'MyLife'
])

const NOTICE_OPTION_DIALOG_VM_NAMES = new Set([
  'OptionNotice'
])

const FORM_SHORTCUT_DIALOG_VM_NAMES = new Set([
  ...DEFECT_TOOL_DIALOG_VM_NAMES,
  ...STATISTIC_DIALOG_VM_NAMES,
  ...NOTICE_OPTION_DIALOG_VM_NAMES,
  'ModuleDialog',
  'Account',
  'CreateTeamMember',
  'InviteTeamMember',
  'AddProjectMember',
  'Document',
  'SendNoticeDialog'
])

function isModifierKeyEvent(e) {
  if (!e) return false
  const k = e.key
  return k === 'Control' || k === 'Meta' || k === 'OS' ||
    e.keyCode === 17 || e.keyCode === 91 || e.keyCode === 93
}

export function isEscapeCloseKey(e) {
  if (!e || e.isComposing) return false
  if (e.metaKey || e.ctrlKey || e.altKey) return false
  return e.key === 'Escape' || e.key === 'Esc' || e.keyCode === 27
}

export function isSaveShortcutKey(e) {
  if (!e || e.isComposing || e.altKey) return false
  if (!(e.metaKey || e.ctrlKey)) return false
  return e.key === 'Enter' || e.keyCode === 13
}

function isVisibleEl(el) {
  if (!el || !el.getClientRects) return false
  const st = window.getComputedStyle(el)
  if (st.display === 'none' || st.visibility === 'hidden') return false
  return el.getClientRects().length > 0
}

export function isFormShortcutSurfaceOpen(vm) {
  if (!vm) return false
  return vm.visible === true || vm.dialogVisible === true
}

function findFormVmFromNode(node, nameSet) {
  let el = node
  while (el) {
    let vm = el.__vue__
    while (vm) {
      const name = vm.$options && vm.$options.name
      if (nameSet.has(name)) return vm
      vm = vm.$parent
    }
    el = el.parentElement
  }
  return null
}

function findTopDrawerVmFromDom() {
  const candidates = []
  document.querySelectorAll('.el-drawer__wrapper').forEach((wrapper) => {
    if (!isVisibleEl(wrapper)) return
    const header = wrapper.querySelector(FORM_DRAWER_HEADER_SEL)
    if (!header || !isVisibleEl(header)) return
    const accent = wrapper.querySelector('.defect-drawer-accent')
    const z = parseInt(window.getComputedStyle(wrapper).zIndex, 10) || 0
    const vm = (accent && findFormVmFromNode(accent, FORM_DRAWER_VM_NAMES)) ||
      findFormVmFromNode(header, FORM_DRAWER_VM_NAMES)
    if (vm && isFormShortcutSurfaceOpen(vm)) candidates.push({ vm, z })
  })
  if (!candidates.length) return null
  candidates.sort((a, b) => b.z - a.z)
  return candidates[0].vm
}

function findTopToolDialogVmFromDom() {
  const candidates = []
  document.querySelectorAll('.el-dialog__wrapper').forEach((wrapper) => {
    if (!isVisibleEl(wrapper)) return
    const vm = findFormVmFromNode(wrapper, FORM_SHORTCUT_DIALOG_VM_NAMES)
    if (!vm || !isFormShortcutSurfaceOpen(vm)) return
    const z = parseInt(window.getComputedStyle(wrapper).zIndex, 10) || 0
    candidates.push({ vm, z })
  })
  if (!candidates.length) return null
  candidates.sort((a, b) => b.z - a.z)
  return candidates[0].vm
}

function findTopFormSurfaceVmFromDom() {
  const drawerVm = findTopDrawerVmFromDom()
  const dialogVm = findTopToolDialogVmFromDom()
  if (drawerVm && dialogVm) {
    const drawerZ = getSurfaceZIndex(drawerVm, '.el-drawer__wrapper')
    const dialogZ = getSurfaceZIndex(dialogVm, '.el-dialog__wrapper')
    return dialogZ >= drawerZ ? dialogVm : drawerVm
  }
  return dialogVm || drawerVm
}

function getSurfaceZIndex(vm, wrapperSel) {
  let z = 0
  const root = vm.$el
  if (!root || typeof root.closest !== 'function') return z
  const wrapper = root.closest(wrapperSel)
  if (wrapper) {
    z = parseInt(window.getComputedStyle(wrapper).zIndex, 10) || 0
  }
  return z
}

function resolveFormSurfaceVm(preferredVm) {
  if (preferredVm && isFormShortcutSurfaceOpen(preferredVm)) return preferredVm
  for (let i = stack.length - 1; i >= 0; i--) {
    const vm = stack[i]
    if (vm && isFormShortcutSurfaceOpen(vm)) return vm
  }
  return findTopFormSurfaceVmFromDom()
}

export function findTopFormDrawerVm() {
  return resolveFormSurfaceVm()
}

function invokeSave(vm) {
  if (typeof vm.shortcutSave === 'function') vm.shortcutSave()
  else if (typeof vm.submitForm === 'function') vm.submitForm()
  else if (typeof vm.onSubmit === 'function') vm.onSubmit()
}

function invokeClose(vm, e) {
  if (!vm) return false
  if (typeof vm.shortcutClose === 'function') {
    vm.shortcutClose(e)
    return true
  }
  if (typeof vm.requestCloseDefectFormDrawer === 'function') {
    vm.requestCloseDefectFormDrawer()
    return true
  }
  if (typeof vm.requestCloseCaseFormDrawer === 'function') {
    vm.requestCloseCaseFormDrawer()
    return true
  }
  if (typeof vm.close === 'function') {
    vm.close()
    return true
  }
  if (typeof vm.cancel === 'function') {
    vm.cancel()
    return true
  }
  return false
}

/** 下拉/日期面板等仍打开时，Esc 先交给各浮层自身处理 */
function shouldDeferFormEscClose() {
  if (isAnyProjectIconPopoverOpenInDom()) return true
  return hasBlockingUiLayer({
    excludeDefectFormDrawer: true,
    excludeHandleDefectDrawer: true,
    excludeViewReportDrawer: true,
    excludeDefectToolDialog: true,
    excludeCaseImportDialog: true
  })
}

/** 供 form-field-hints 等 mixin 主动触发关闭 */
export function tryCloseDefectDrawer(preferredVm, e) {
  if (!isEscapeCloseKey(e)) return false
  if (shouldDeferFormEscClose()) return false
  if (preferredVm && typeof preferredVm.$_invokeDrawerShortcutClose === 'function') {
    return preferredVm.$_invokeDrawerShortcutClose(e)
  }
  const vm = resolveFormSurfaceVm(preferredVm)
  if (!vm) return false
  e.preventDefault()
  e.stopImmediatePropagation()
  return invokeClose(vm, e)
}

function onKeyup(e) {
  if (!isModifierKeyEvent(e)) return
  document.querySelectorAll('.defect-page').forEach((el) => {
    const vm = el.__vue__
    if (vm && typeof vm.$_hidePageActionHints === 'function') {
      vm.$_hidePageActionHints()
    }
  })
  const surface = findTopFormDrawerVm()
  if (surface && typeof surface.$_hideFieldHints === 'function') {
    surface.$_hideFieldHints()
  }
  document.querySelectorAll('.defect-drawer-accent').forEach((el) => {
    let vm = el.__vue__
    while (vm) {
      if (typeof vm.$_hideHandleKbdHints === 'function') {
        vm.$_hideHandleKbdHints()
        break
      }
      vm = vm.$parent
    }
  })
}

function onKeydown(e) {
  if (e.isComposing) return

  const vm = resolveFormSurfaceVm()
  if (!vm) return

  if (isSaveShortcutKey(e)) {
    if (shortcutService.palette && shortcutService.palette.open) return
    e.preventDefault()
    e.stopImmediatePropagation()
    if (typeof vm.$_invokeDrawerShortcutSave === 'function') {
      vm.$_invokeDrawerShortcutSave(e)
    } else {
      invokeSave(vm)
    }
    return
  }

  if (!isEscapeCloseKey(e)) return
  if (shortcutService.palette && shortcutService.palette.open) return
  if (shouldDeferFormEscClose()) return

  e.preventDefault()
  e.stopImmediatePropagation()
  if (typeof vm.$_invokeDrawerShortcutClose === 'function') {
    vm.$_invokeDrawerShortcutClose(e)
  } else {
    invokeClose(vm, e)
  }
}

function onWindowBlur() {
  if (isNativeFilePickerOpen()) return
}

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
    if (!stack[i] || !isFormShortcutSurfaceOpen(stack[i])) stack.splice(i, 1)
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
