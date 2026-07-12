/**
 * Global dropdown keyboard behavior (Element Plus adapter + utils).
 */
import { initSplitDropdownKbd } from '@/utils/split-dropdown-kbd'
import { DROPDOWN_SELECTORS } from '@/adapters/element-plus/dropdown-adapter'

function patchDropdownHosts(root) {
  if (!root) return
  initSplitDropdownKbd(root)
}

function observeDropdownHosts(root) {
  const observer = new MutationObserver((mutations) => {
    mutations.forEach((m) => {
      m.addedNodes.forEach((node) => {
        if (node.nodeType !== 1) return
        if (node.classList && node.classList.contains('el-dropdown')) {
          patchDropdownHosts(node.parentElement || document.body)
          return
        }
        if (node.querySelectorAll) {
          const hosts = node.querySelectorAll(DROPDOWN_SELECTORS.host)
          if (hosts.length) patchDropdownHosts(node)
        }
      })
    })
  })
  if (root) {
    observer.observe(root, { childList: true, subtree: true })
  }
  return observer
}

export function installDropdownKbd() {
  patchDropdownHosts(document.body)
  observeDropdownHosts(document.body)
}

export function useDropdownKbd() {
  return {
    install: installDropdownKbd,
    patchDropdownHosts
  }
}
