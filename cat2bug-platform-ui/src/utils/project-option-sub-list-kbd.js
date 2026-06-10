/**
 * 项目设置列表类子页：分页快捷键（与返回 B 错开，默认 U/P）。
 */
import { shortcutStore } from '@/plugins/shortcut/shortcut-store'
import { PROJECT_OPTION_KBD_SCOPE } from '@/mixins/project-option-sub-kbd'

const PAGINATION_FLOAT = { placement: 'bottom-right-outset', outset: 2 }

export function resolveProjectOptionSubPaginationTotal(vm) {
  if (typeof vm.getProjectOptionSubPaginationTotal === 'function') {
    return vm.getProjectOptionSubPaginationTotal()
  }
  return vm.total || 0
}

export function buildProjectOptionSubPaginationShortcuts(vm) {
  const selector = typeof vm.getProjectOptionSubPaginationSelector === 'function'
    ? vm.getProjectOptionSubPaginationSelector()
    : null
  if (!selector) {
    return { register: [], hints: [] }
  }
  const scope = vm.getProjectOptionSubKbdScope
    ? vm.getProjectOptionSubKbdScope()
    : PROJECT_OPTION_KBD_SCOPE
  const L = (key, def) => shortcutStore.getLetter(`action.${scope}.${key}`, def)
  const visible = () => resolveProjectOptionSubPaginationTotal(vm) > 0
  const runPrev = () => vm.shortcutChangePage(-1)
  const runNext = () => vm.shortcutChangePage(1)
  return {
    register: [
      { key: 'prevPage', defaultLetter: 'U', run: runPrev },
      { key: 'nextPage', defaultLetter: 'P', run: runNext }
    ],
    hints: [
      {
        key: 'prevPage',
        letter: L('prevPage', 'U'),
        badgeSelector: `${selector} .btn-prev`,
        floatOffset: PAGINATION_FLOAT,
        run: runPrev,
        visible
      },
      {
        key: 'nextPage',
        letter: L('nextPage', 'P'),
        badgeSelector: `${selector} .btn-next`,
        floatOffset: PAGINATION_FLOAT,
        run: runNext,
        visible
      }
    ]
  }
}

export function clickProjectOptionSubPagination(vm, delta) {
  const selector = typeof vm.getProjectOptionSubPaginationSelector === 'function'
    ? vm.getProjectOptionSubPaginationSelector()
    : null
  if (!selector) return
  const root = typeof vm.getPageActionHintContainer === 'function'
    ? vm.getPageActionHintContainer()
    : vm.$el
  if (!root || typeof root.querySelector !== 'function') return
  const btn = root.querySelector(
    delta < 0 ? `${selector} .btn-prev` : `${selector} .btn-next`
  )
  if (btn && !btn.classList.contains('disabled') && typeof btn.click === 'function') {
    btn.click()
  }
}
