import { computed, getCurrentInstance, onBeforeUnmount, watch } from 'vue'
import {
  registerDefectDrawerShortcuts,
  unregisterDefectDrawerShortcuts,
  findTopFormDrawerVm,
  isEscapeCloseKey,
  isSaveShortcutKey
} from '@/utils/defect-drawer-shortcuts'
import { isAnyProjectIconPopoverOpenInDom } from '@/utils/project-icon-popover-kbd'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'
import { dismissToolbarSplitDropdownSessions } from '@/utils/split-dropdown-kbd'

export const SAVE_SHORTCUT_LABEL = '↵'

export function useDialogFormShortcuts() {
  const instance = getCurrentInstance()
  const vm = () => instance && instance.proxy

  let $_dialogShortcutsRegistered = false

  // 组件可自定义「表单浮层是否可见」（如文档页用 open/moveDialogVisible 而非 visible/dialogVisible）。
  const type = instance && instance.type
  const hostOwnsSurface = !!(type && type.computed && type.computed.$_formShortcutSurfaceVisible)

  const $_formShortcutSurfaceVisible = computed(() => {
    const host = vm()
    if (!host) return false
    // 组件自身定义了同名计算属性时，以组件的为准（避免读取本 composable 暴露值造成递归）。
    if (hostOwnsSurface) return host.$_formShortcutSurfaceVisible === true
    return host.visible === true || host.dialogVisible === true
  })

  const dialogSaveShortcutLabel = computed(() => {
    const host = vm()
    return host && host.fieldHintsActive ? SAVE_SHORTCUT_LABEL : ''
  })

  const $_syncFormShortcutBinding = (open) => {
    if (open) {
      $_dialogShortcutsRegistered = true
      registerDefectDrawerShortcuts(vm())
      $_hideAncestorPageActionHints()
    } else if (!$_formShortcutSurfaceVisible.value) {
      $_unbindDialogShortcuts()
    }
  }

  const $_ownsTopFormDrawer = () => {
    if (!$_formShortcutSurfaceVisible.value) return false
    const top = findTopFormDrawerVm()
    const host = vm()
    return !top || top === host
  }

  const $_canCloseDrawerByShortcut = (e) => {
    if (!$_formShortcutSurfaceVisible.value) return false
    if (!isEscapeCloseKey(e)) return false
    if (isAnyProjectIconPopoverOpenInDom()) return false
    return !hasBlockingUiLayer({
      excludeDefectFormDrawer: true,
      excludeHandleDefectDrawer: true,
      excludeViewReportDrawer: true,
      excludeDefectToolDialog: true,
      excludeCaseImportDialog: true
    })
  }

  const $_invokeDrawerShortcutSave = (e) => {
    const host = vm()
    if (!host || !$_ownsTopFormDrawer()) return false
    if (!$_formShortcutSurfaceVisible.value || !isSaveShortcutKey(e)) return false
    if (e) {
      e.preventDefault()
      e.stopPropagation()
      if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
    }
    if (typeof host.shortcutSave === 'function') host.shortcutSave()
    else if (typeof host.submitForm === 'function') host.submitForm()
    return true
  }

  const $_invokeDrawerShortcutClose = (e) => {
    const host = vm()
    if (!host || !$_ownsTopFormDrawer()) return false
    if (!$_canCloseDrawerByShortcut(e)) return false
    if (e) {
      e.preventDefault()
      e.stopPropagation()
      if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
    }
    if (typeof host.shortcutClose === 'function') host.shortcutClose(e)
    else if (typeof host.requestCloseDefectFormDrawer === 'function') host.requestCloseDefectFormDrawer()
    else if (typeof host.requestCloseCaseFormDrawer === 'function') host.requestCloseCaseFormDrawer()
    else if (typeof host.close === 'function') host.close()
    else if (typeof host.cancel === 'function') host.cancel()
    return true
  }

  const $_handleDrawerShortcutKey = (e) => {
    if (!$_formShortcutSurfaceVisible.value || !$_ownsTopFormDrawer() || e.isComposing) return
    if (isSaveShortcutKey(e)) {
      $_invokeDrawerShortcutSave(e)
      return
    }
    if (isEscapeCloseKey(e)) {
      $_invokeDrawerShortcutClose(e)
    }
  }

  const $_hideAncestorPageActionHints = () => {
    const host = vm()
    let parent = host && host.$parent
    while (parent) {
      if (typeof parent.$_hidePageActionHints === 'function') {
        parent.$_hidePageActionHints()
        if (parent.$el) dismissToolbarSplitDropdownSessions(parent.$el)
      }
      if (typeof parent.exitListQueryKeyboardNav === 'function') {
        parent.exitListQueryKeyboardNav()
      }
      if (typeof parent.$_hidePageActionHints === 'function') return
      parent = parent.$parent
    }
  }

  const $_unbindDialogShortcuts = () => {
    if (!$_dialogShortcutsRegistered) return
    $_dialogShortcutsRegistered = false
    unregisterDefectDrawerShortcuts(vm())
  }

  watch(() => vm()?.visible, (val) => {
    $_syncFormShortcutBinding(!!val)
  }, { immediate: true })

  watch(() => vm()?.dialogVisible, (val) => {
    $_syncFormShortcutBinding(!!val)
  }, { immediate: true })

  onBeforeUnmount(() => {
    $_unbindDialogShortcuts()
  })

  return {
    $_formShortcutSurfaceVisible,
    dialogSaveShortcutLabel,
    $_syncFormShortcutBinding,
    $_ownsTopFormDrawer,
    $_canCloseDrawerByShortcut,
    $_invokeDrawerShortcutSave,
    $_invokeDrawerShortcutClose,
    $_handleDrawerShortcutKey,
    $_hideAncestorPageActionHints,
    $_unbindDialogShortcuts
  }
}
