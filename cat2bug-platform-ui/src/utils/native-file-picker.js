/**
 * 打开原生文件选择框时，页面内 capture 键盘监听（如 vue-excel-editor、上传外框）
 * 仍会拦截方向键。在 click() 前标记会话、对话框关闭后结束。
 */

let sessionDepth = 0
let windowFocusResumeHandler = null

export function isNativeFilePickerOpen() {
  return sessionDepth > 0
}

/** 打开文件框前调用（可嵌套，与 endNativeFilePickerSession 成对） */
export function beginNativeFilePickerSession() {
  sessionDepth++
  if (sessionDepth === 1) {
    if (document.documentElement) {
      document.documentElement.setAttribute('data-cat2bug-native-file-picker', '1')
    }
    bindWindowFocusResume()
  }
}

/** 选完文件或取消后调用 */
export function endNativeFilePickerSession() {
  if (sessionDepth <= 0) return
  sessionDepth = 0
  if (document.documentElement) {
    document.documentElement.removeAttribute('data-cat2bug-native-file-picker')
  }
  unbindWindowFocusResume()
}

function bindWindowFocusResume() {
  if (windowFocusResumeHandler) return
  windowFocusResumeHandler = () => {
    endNativeFilePickerSession()
  }
  window.addEventListener('focus', windowFocusResumeHandler, true)
}

function unbindWindowFocusResume() {
  if (!windowFocusResumeHandler) return
  window.removeEventListener('focus', windowFocusResumeHandler, true)
  windowFocusResumeHandler = null
}

/**
 * @param {Function} resume
 * @returns {Function} 已注册的 handler，便于提前 remove
 */
export function bindNativeFilePickerResume(resume) {
  if (typeof resume !== 'function') return null
  const handler = () => {
    window.removeEventListener('focus', handler, true)
    endNativeFilePickerSession()
    resume()
  }
  window.addEventListener('focus', handler, true)
  return handler
}

/**
 * @param {object|null} ed vue-excel-editor 实例
 * @returns {{ focused: boolean, mousein: boolean }|null}
 */
export function suspendExcelEditorKeyboard(ed) {
  if (!ed) return null
  const prev = { focused: ed.focused, mousein: ed.mousein }
  ed.focused = false
  ed.mousein = false
  if (ed.inputBox && typeof ed.inputBox.blur === 'function') {
    ed.inputBox.blur()
  }
  return prev
}

/**
 * @param {object|null} ed
 * @param {{ focused: boolean, mousein: boolean }|null} prev
 */
export function resumeExcelEditorKeyboard(ed, prev) {
  if (!ed || !prev) return
  ed.focused = prev.focused
  ed.mousein = prev.mousein
}

/**
 * 包装 vue-excel-editor.winKeydown：文件框打开时不拦截方向键
 * @param {object} ed
 */
export function patchExcelEditorWinKeydownForFilePicker(ed) {
  if (!ed || ed._cat2bugFilePickerKeydownPatched || typeof ed.winKeydown !== 'function') return
  ed._cat2bugFilePickerKeydownPatched = true
  const orig = ed.winKeydown.bind(ed)
  ed.winKeydown = (e) => {
    if (isNativeFilePickerOpen()) return
    return orig(e)
  }
}
