/**
 * Milkdown block-handle calls setDragImage(activeEl) on the full block DOM.
 * Substitute a faithful clone inside a .report-template-editor host so theme
 * CSS still applies, while clipping overflow/scrollbars to the block bounds.
 */

let activeGhost = null
let restoreSetDragImage = null

function resolveBlockElement(element) {
  if (!(element instanceof HTMLElement)) {
    return null
  }

  const selectors = [
    '.cat2bug-dsl-block__view',
    '.cat2bug-dsl-block',
    '[data-cat2bug-defect-table]',
    '[data-cat2bug-case-card]',
    '[data-cat2bug-dynamic-image]',
    '[data-cat2bug-raw-block]'
  ]

  for (const selector of selectors) {
    if (element.matches(selector)) {
      return element
    }
    const nested = element.querySelector(selector)
    if (nested instanceof HTMLElement) {
      return nested
    }
  }

  const editor = element.closest('.report-template-editor')
  if (!editor) {
    return null
  }

  const selected = editor.querySelector('.ProseMirror-selectednode')
  if (!(selected instanceof HTMLElement)) {
    return null
  }

  for (const selector of selectors) {
    if (selected.matches(selector)) {
      return selected
    }
    const nested = selected.querySelector(selector) || selected.closest(selector)
    if (nested instanceof HTMLElement) {
      return nested
    }
  }

  return null
}

function clipScrollbars(clone) {
  clone.querySelectorAll('.cat2bug-dsl-preview-table__scroll').forEach((node) => {
    if (node instanceof HTMLElement) {
      node.style.overflow = 'hidden'
    }
  })
  clone.querySelectorAll('.cat2bug-dsl-block__view-token--raw').forEach((node) => {
    if (node instanceof HTMLElement) {
      node.style.overflow = 'hidden'
    }
  })
}

/** Clone keeps ProseMirror-selectednode; outside .milkdown .ProseMirror that gets blue outline (#8cf). */
function stripSelectionState(clone) {
  const nodes = [clone, ...clone.querySelectorAll('.ProseMirror-selectednode')]
  nodes.forEach((node) => {
    if (!(node instanceof HTMLElement)) {
      return
    }
    node.classList.remove('ProseMirror-selectednode')
    node.style.outline = 'none'
    node.style.boxShadow = 'none'
    node.removeAttribute('draggable')
  })
}

function createDragGhost(block) {
  const editor = block.closest('.report-template-editor')
  const rect = block.getBoundingClientRect()

  const host = document.createElement('div')
  host.className = 'report-template-editor report-template-block-drag-ghost-host'
  if (editor?.classList.contains('report-template-editor--dark')) {
    host.classList.add('report-template-editor--dark')
  }

  const clone = block.cloneNode(true)
  if (!(clone instanceof HTMLElement)) {
    return host
  }

  clipScrollbars(clone)
  stripSelectionState(clone)

  Object.assign(host.style, {
    position: 'fixed',
    top: '-9999px',
    left: '0',
    width: `${Math.ceil(rect.width)}px`,
    height: `${Math.ceil(rect.height)}px`,
    overflow: 'hidden',
    boxSizing: 'border-box',
    margin: '0',
    padding: '0',
    pointerEvents: 'none',
    zIndex: '-1'
  })

  Object.assign(clone.style, {
    width: `${Math.ceil(rect.width)}px`,
    maxWidth: `${Math.ceil(rect.width)}px`,
    margin: '0',
    boxSizing: 'border-box'
  })

  host.appendChild(clone)
  document.body.appendChild(host)

  const scheduleCleanup = () => {
    host.remove()
    document.removeEventListener('dragend', scheduleCleanup)
  }
  document.addEventListener('dragend', scheduleCleanup, { once: true })

  return host
}

function patchSetDragImage() {
  if (restoreSetDragImage) {
    return restoreSetDragImage
  }

  const original = DataTransfer.prototype.setDragImage
  DataTransfer.prototype.setDragImage = function setDragImagePatched(element, xOffset, yOffset) {
    const block = resolveBlockElement(element)
    if (block?.closest('.report-template-editor')) {
      activeGhost?.remove()
      activeGhost = createDragGhost(block)
      original.call(this, activeGhost, xOffset, yOffset)
      return
    }
    original.call(this, element, xOffset, yOffset)
  }

  restoreSetDragImage = () => {
    DataTransfer.prototype.setDragImage = original
    restoreSetDragImage = null
    activeGhost?.remove()
    activeGhost = null
  }
  return restoreSetDragImage
}

/** @returns {() => void} */
export function installBlockDragImageFix() {
  return patchSetDragImage()
}
