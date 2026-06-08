/**
 * 全局：所有 Element UI el-dropdown 展开后默认高亮首项，↑/↓ 切换、Enter 确认、Esc 关闭。
 */
import { initSplitDropdownKbd } from '@/utils/split-dropdown-kbd'

function patchDropdownHosts(root) {
  if (!root) return
  initSplitDropdownKbd(root)
}

const DropdownKbdPlugin = {
  install() {
    patchDropdownHosts(document.body)

    const observer = new MutationObserver((mutations) => {
      mutations.forEach((m) => {
        m.addedNodes.forEach((node) => {
          if (node.nodeType !== 1) return
          if (node.classList && node.classList.contains('el-dropdown')) {
            patchDropdownHosts(node.parentElement || document.body)
            return
          }
          if (node.querySelectorAll) {
            const hosts = node.querySelectorAll('.el-dropdown')
            if (hosts.length) patchDropdownHosts(node)
          }
        })
      })
    })

    if (document.body) {
      observer.observe(document.body, { childList: true, subtree: true })
    }
  }
}

export default DropdownKbdPlugin
