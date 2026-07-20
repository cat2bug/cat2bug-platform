/**
* v-clipboard 文字复制剪贴
* Copyright (c) 2023 cat2bug
*/

import Clipboard from 'clipboard'

function bindClipboard(el, binding) {
  switch (binding.arg) {
    case 'success':
      el._vClipBoard_success = binding.value
      break
    case 'error':
      el._vClipBoard_error = binding.value
      break
    default: {
      const clipboard = new Clipboard(el, {
        text: () => binding.value,
        action: () => (binding.arg === 'cut' ? 'cut' : 'copy')
      })
      clipboard.on('success', e => {
        const callback = el._vClipBoard_success
        callback && callback(e)
      })
      clipboard.on('error', e => {
        const callback = el._vClipBoard_error
        callback && callback(e)
      })
      el._vClipBoard = clipboard
    }
  }
}

function updateClipboard(el, binding) {
  if (binding.arg === 'success') {
    el._vClipBoard_success = binding.value
  } else if (binding.arg === 'error') {
    el._vClipBoard_error = binding.value
  } else if (el._vClipBoard) {
    el._vClipBoard.text = function () { return binding.value }
    el._vClipBoard.action = () => (binding.arg === 'cut' ? 'cut' : 'copy')
  }
}

function unbindClipboard(el, binding) {
  if (binding.arg === 'success') {
    delete el._vClipBoard_success
  } else if (binding.arg === 'error') {
    delete el._vClipBoard_error
  } else if (el._vClipBoard) {
    el._vClipBoard.destroy()
    delete el._vClipBoard
  }
}

export default {
  // Vue 3
  mounted: bindClipboard,
  updated: updateClipboard,
  unmounted: unbindClipboard,
  // Vue 2 兼容别名（若仍有旧运行时）
  bind: bindClipboard,
  update: updateClipboard,
  unbind: unbindClipboard
}
