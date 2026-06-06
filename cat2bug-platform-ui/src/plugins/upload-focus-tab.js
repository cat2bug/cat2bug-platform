/**
 * 全局修补 FileUpload / ImageUpload 的 Tab 顺序（外框单停靠点）。
 */

import {
  observeUploadFocusTarget,
  onUploadFocusIn,
  onUploadTabKeydown,
  onUploadAreaClickCapture,
  patchAllUploadFocusTargets
} from '@/utils/upload-focus-tab'

const UploadFocusTabPlugin = {
  install() {
    patchAllUploadFocusTargets()
    document.querySelectorAll('.cat2bug-upload-focus-target').forEach(observeUploadFocusTarget)

    const observer = new MutationObserver((mutations) => {
      mutations.forEach((m) => {
        m.addedNodes.forEach((node) => {
          if (node.nodeType !== 1) return
          if (node.classList && node.classList.contains('cat2bug-upload-focus-target')) {
            observeUploadFocusTarget(node)
          }
          if (node.querySelectorAll) {
            node.querySelectorAll('.cat2bug-upload-focus-target').forEach(observeUploadFocusTarget)
          }
        })
      })
    })
    if (document.body) {
      observer.observe(document.body, { childList: true, subtree: true })
    }
    document.addEventListener('focusin', onUploadFocusIn, true)
    document.addEventListener('keydown', onUploadTabKeydown, true)
    document.addEventListener('click', onUploadAreaClickCapture, true)
  }
}

export default UploadFocusTabPlugin
