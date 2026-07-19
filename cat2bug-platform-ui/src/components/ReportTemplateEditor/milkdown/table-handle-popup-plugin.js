import { Plugin } from '@milkdown/kit/prose/state'
import { $prose } from '@milkdown/kit/utils'
import {
  clearOpsOpen,
  isTableColOrRowSelection,
  syncTableHandlePopup
} from './table-handle-popup'

/**
 * Keep Milkdown column/row operation popup visible while CellSelection is active.
 * Only reacts to col/row selection changes + pointer events while that selection is active.
 * Avoids work on every keystroke (TextSelection updates) which froze large tables.
 */
export const tableHandlePopupPlugin = $prose(() => new Plugin({
  view(view) {
    let raf = 0
    let leaveTimer = 0

    const sync = () => {
      syncTableHandlePopup(view)
    }

    const scheduleSync = () => {
      if (!isTableColOrRowSelection(view.state.selection)) {
        sync()
        return
      }
      sync()
      if (raf) {
        cancelAnimationFrame(raf)
      }
      raf = requestAnimationFrame(() => {
        raf = 0
        sync()
      })
    }

    const onPointerMove = () => {
      if (!isTableColOrRowSelection(view.state.selection)) {
        return
      }
      scheduleSync()
    }

    const onPointerLeave = () => {
      if (!isTableColOrRowSelection(view.state.selection)) {
        return
      }
      scheduleSync()
      if (leaveTimer) {
        clearTimeout(leaveTimer)
      }
      leaveTimer = setTimeout(() => {
        leaveTimer = 0
        sync()
      }, 220)
    }

    view.dom.addEventListener('pointermove', onPointerMove)
    view.dom.addEventListener('pointerleave', onPointerLeave, true)

    return {
      update(_view, prevState) {
        const now = isTableColOrRowSelection(view.state.selection)
        const was = isTableColOrRowSelection(prevState.selection)
        // Typing in cells is TextSelection → TextSelection; skip entirely.
        if (!now && !was) {
          return
        }
        if (now && was && view.state.selection.eq(prevState.selection)) {
          return
        }
        scheduleSync()
      },
      destroy() {
        if (raf) {
          cancelAnimationFrame(raf)
        }
        if (leaveTimer) {
          clearTimeout(leaveTimer)
        }
        view.dom.removeEventListener('pointermove', onPointerMove)
        view.dom.removeEventListener('pointerleave', onPointerLeave, true)
        clearOpsOpen(view.dom)
      }
    }
  }
}))
