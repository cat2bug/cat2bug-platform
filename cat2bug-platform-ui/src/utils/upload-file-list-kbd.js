/**
 * 附件上传：外框（选取按钮）与下方已上传文件列表之间的 ↑↓ 漫游与 Delete 删除。
 */

export const UPLOAD_KBD_SHELL = 'shell'
export const UPLOAD_KBD_LIST = 'list'

export function createUploadListKbdState() {
  return {
    zone: UPLOAD_KBD_SHELL,
    index: 0
  }
}

export function queryUploadListItems(rootEl, listSelector) {
  if (!rootEl || !listSelector) return []
  return Array.from(rootEl.querySelectorAll(listSelector))
}

export function clearUploadListKeyboardFocus(rootEl) {
  if (!rootEl) return
  rootEl.querySelectorAll('.is-keyboard-focus').forEach((el) => {
    el.classList.remove('is-keyboard-focus')
  })
}

/**
 * @returns {{ zone: string, index: number }}
 */
export function applyUploadListKeyboardFocus(rootEl, zone, index, listSelector) {
  clearUploadListKeyboardFocus(rootEl)
  if (zone !== UPLOAD_KBD_LIST) {
    return { zone: UPLOAD_KBD_SHELL, index: 0 }
  }
  const items = queryUploadListItems(rootEl, listSelector)
  if (!items.length) {
    return { zone: UPLOAD_KBD_SHELL, index: 0 }
  }
  let i = index < 0 ? 0 : index
  if (i >= items.length) i = items.length - 1
  items[i].classList.add('is-keyboard-focus')
  if (typeof items[i].scrollIntoView === 'function') {
    items[i].scrollIntoView({ block: 'nearest', inline: 'nearest' })
  }
  return { zone: UPLOAD_KBD_LIST, index: i }
}

/**
 * @param {{ zone: string, index: number }} state
 * @param {{ fileCount: number, onOpenPicker?: Function, onDeleteFile?: Function }} options
 * @returns {{ handled: boolean, state: { zone: string, index: number } }}
 */
export function handleUploadListKeydown(e, state, options) {
  const { fileCount, onOpenPicker, onDeleteFile } = options
  if (e.isComposing) return { handled: false, state }

  const key = e.key
  let { zone, index } = state

  if (key === 'ArrowDown') {
    if (zone === UPLOAD_KBD_SHELL && fileCount > 0) {
      e.preventDefault()
      return { handled: true, state: { zone: UPLOAD_KBD_LIST, index: 0 } }
    }
    if (zone === UPLOAD_KBD_LIST && index < fileCount - 1) {
      e.preventDefault()
      return { handled: true, state: { zone: UPLOAD_KBD_LIST, index: index + 1 } }
    }
    return { handled: false, state }
  }

  if (key === 'ArrowUp') {
    if (zone === UPLOAD_KBD_LIST) {
      e.preventDefault()
      if (index <= 0) {
        return { handled: true, state: { zone: UPLOAD_KBD_SHELL, index: 0 } }
      }
      return { handled: true, state: { zone: UPLOAD_KBD_LIST, index: index - 1 } }
    }
    return { handled: false, state }
  }

  if (key === 'Delete' || key === 'Backspace') {
    if (zone === UPLOAD_KBD_LIST) {
      e.preventDefault()
      if (typeof onDeleteFile === 'function') onDeleteFile(index)
      return { handled: true, state: { zone, index } }
    }
    return { handled: false, state }
  }

  if (
    zone === UPLOAD_KBD_SHELL &&
    (key === 'Enter' || key === ' ' || key === 'Spacebar' || e.code === 'Space')
  ) {
    e.preventDefault()
    if (typeof onOpenPicker === 'function') onOpenPicker()
    return { handled: true, state }
  }

  return { handled: false, state }
}
