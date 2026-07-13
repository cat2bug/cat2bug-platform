const DOC_ICON =
  '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><path fill="currentColor" d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm-1 2l5 5h-5V4zM8 13h8v2H8v-2zm0 4h5v2H8v-2z"/></svg>'

const MENU_VIEWPORT_MARGIN = 12

/**
 * @typedef {import('@milkdown/ctx').Ctx} MilkdownCtx
 */

function isBlockAddButton(target) {
  if (!(target instanceof Element)) {
    return false
  }
  const item = target.closest('.report-template-editor .milkdown-block-handle .operation-item')
  if (!item) {
    return false
  }
  return item === item.parentElement?.querySelector('.operation-item')
}

/**
 * @param {{ getMenuGroups: () => { key: string, label: string, items: { key: string, label: string, onSelect: (ctx: MilkdownCtx) => void }[] }[], runEditorAction: (fn: (ctx: MilkdownCtx) => void) => void, focusEditor?: () => void, isDark?: () => boolean }} options
 * @returns {() => void}
 */
export function installReportBlockAddOverlay(options) {
  const {
    getMenuGroups,
    runEditorAction,
    focusEditor = () => {},
    isDark = () => false
  } = options

  let host = null
  let themeWrap = null
  let menuEl = null
  let backdrop = null
  let open = false
  let activeTab = 0
  let suppressObserver = null
  let openAfterAddFrame = 0
  /** @type {DOMRect | null} */
  let anchorRectAtClick = null

  function ensureDom() {
    if (host) {
      return
    }
    host = document.createElement('div')
    host.className = 'report-block-add-menu-host'
    host.style.display = 'none'

    backdrop = document.createElement('div')
    backdrop.className = 'report-block-add-menu-backdrop'
    backdrop.addEventListener('click', close)

    themeWrap = document.createElement('div')
    themeWrap.className = 'report-block-add-menu-theme milkdown'

    menuEl = document.createElement('div')
    menuEl.className = 'report-block-add-menu milkdown-slash-menu'
    menuEl.dataset.show = 'true'
    menuEl.addEventListener('pointerdown', (event) => {
      event.preventDefault()
    })

    themeWrap.appendChild(menuEl)
    host.append(backdrop, themeWrap)
    document.body.appendChild(host)
  }

  function syncThemeClass() {
    if (!themeWrap) {
      return
    }
    themeWrap.classList.toggle('is-dark', isDark())
  }

  function hideCrepeSlashMenu() {
    document.querySelectorAll('.report-template-editor .milkdown-slash-menu').forEach((node) => {
      if (node instanceof HTMLElement) {
        node.dataset.show = 'false'
        node.style.display = 'none'
      }
    })
  }

  function startSuppressingNativeMenu() {
    hideCrepeSlashMenu()
    suppressObserver?.disconnect()
    suppressObserver = new MutationObserver(() => {
      if (!open) {
        return
      }
      suppressObserver.disconnect()
      hideCrepeSlashMenu()
      const editorRoot = document.querySelector('.report-template-editor .report-milkdown-editor')
      if (editorRoot && open) {
        suppressObserver.observe(editorRoot, {
          subtree: true,
          attributes: true,
          attributeFilter: ['data-show', 'style', 'class']
        })
      }
    })
    const editorRoot = document.querySelector('.report-template-editor .report-milkdown-editor')
    if (editorRoot) {
      suppressObserver.observe(editorRoot, {
        subtree: true,
        attributes: true,
        attributeFilter: ['data-show', 'style', 'class']
      })
    }
  }

  function stopSuppressingNativeMenu() {
    suppressObserver?.disconnect()
    suppressObserver = null
  }

  function resolveMenuPosition(anchorRect) {
    const left = Math.min(
      anchorRect.right + MENU_VIEWPORT_MARGIN,
      window.innerWidth - 360 - MENU_VIEWPORT_MARGIN
    )
    return {
      left: Math.max(MENU_VIEWPORT_MARGIN, left),
      top: anchorRect.top
    }
  }

  function getAnchorRect() {
    if (anchorRectAtClick) {
      const rect = anchorRectAtClick
      anchorRectAtClick = null
      return resolveMenuPosition(rect)
    }
    const handle = document.querySelector('.report-template-editor .milkdown-block-handle')
    if (handle instanceof HTMLElement) {
      return resolveMenuPosition(handle.getBoundingClientRect())
    }
    return { left: 120, top: 120 }
  }

  function clampMenuToViewport() {
    if (!themeWrap || !menuEl) {
      return
    }
    const margin = MENU_VIEWPORT_MARGIN
    let left = Number.parseFloat(themeWrap.style.left)
    let top = Number.parseFloat(themeWrap.style.top)
    if (Number.isNaN(left)) {
      left = margin
    }
    if (Number.isNaN(top)) {
      top = margin
    }

    let menuRect = menuEl.getBoundingClientRect()

    if (menuRect.right > window.innerWidth - margin) {
      left = Math.max(margin, window.innerWidth - menuRect.width - margin)
      themeWrap.style.left = `${left}px`
    }

    if (menuRect.bottom > window.innerHeight - margin) {
      top = Math.max(margin, window.innerHeight - menuRect.height - margin)
      themeWrap.style.top = `${top}px`
    }

    if (menuRect.top < margin) {
      themeWrap.style.top = `${margin}px`
      top = margin
    }

    menuRect = menuEl.getBoundingClientRect()
    const scrollRoot = menuEl.querySelector('.menu-groups')
    const tabNav = menuEl.querySelector('.tab-group')
    if (scrollRoot instanceof HTMLElement) {
      const tabHeight = tabNav instanceof HTMLElement ? tabNav.offsetHeight : 0
      const maxScroll = window.innerHeight - margin - menuRect.top - tabHeight - margin
      scrollRoot.style.maxHeight = `${Math.max(120, maxScroll)}px`
    }
  }

  function scrollActiveTabIntoView() {
    const selected = menuEl?.querySelector('.tab-group li.selected')
    if (selected instanceof HTMLElement && typeof selected.scrollIntoView === 'function') {
      selected.scrollIntoView({ block: 'nearest', inline: 'nearest' })
    }
  }

  function buildTabList(groups) {
    const ul = document.createElement('ul')
    groups.forEach((group, index) => {
      const li = document.createElement('li')
      li.textContent = group.label
      li.dataset.tabIndex = String(index)
      if (index === activeTab) {
        li.classList.add('selected')
      }
      li.addEventListener('pointerdown', (event) => {
        event.preventDefault()
        activeTab = index
        renderMenu(getMenuGroups())
        const scrollRoot = menuEl.querySelector('.menu-groups')
        const target = menuEl.querySelector(`[data-group-index="${index}"]`)
        if (scrollRoot instanceof HTMLElement && target instanceof HTMLElement) {
          scrollRoot.scrollTop = target.offsetTop - scrollRoot.offsetTop
        }
      })
      ul.appendChild(li)
    })
    ul.addEventListener(
      'wheel',
      (event) => {
        if (ul.scrollWidth <= ul.clientWidth) {
          return
        }
        event.stopPropagation()
        event.preventDefault()
        ul.scrollLeft += event.deltaY || event.deltaX
      },
      { passive: false }
    )
    return ul
  }

  function renderMenu(groups) {
    if (!menuEl) {
      return
    }
    syncThemeClass()
    menuEl.className = 'report-block-add-menu milkdown-slash-menu'

    const ranges = []
    let offset = 0
    for (const group of groups) {
      ranges.push([offset, offset + group.items.length])
      offset += group.items.length
    }

    if (activeTab >= groups.length) {
      activeTab = 0
    }

    const tabNav = document.createElement('nav')
    tabNav.className = 'tab-group'
    tabNav.appendChild(buildTabList(groups))

    const scrollRoot = document.createElement('div')
    scrollRoot.className = 'menu-groups'
    scrollRoot.addEventListener(
      'wheel',
      (event) => {
        event.stopPropagation()
        event.preventDefault()
        scrollRoot.scrollTop += event.deltaY
      },
      { passive: false }
    )

    groups.forEach((group, groupIndex) => {
      const groupEl = document.createElement('div')
      groupEl.className = 'menu-group'
      groupEl.dataset.groupIndex = String(groupIndex)

      const title = document.createElement('h6')
      title.textContent = group.label
      groupEl.appendChild(title)

      const ul = document.createElement('ul')
      group.items.forEach((item, itemIndex) => {
        const globalIndex = ranges[groupIndex][0] + itemIndex
        const li = document.createElement('li')
        li.dataset.index = String(globalIndex)
        li.innerHTML = `<span class="report-block-add-menu__icon">${DOC_ICON}</span><span>${item.label}</span>`
        li.addEventListener('pointerenter', () => {
          li.classList.add('hover')
        })
        li.addEventListener('pointerleave', () => {
          li.classList.remove('hover')
        })
        li.addEventListener('pointerup', (event) => {
          event.preventDefault()
          event.stopPropagation()
          runEditorAction((ctx) => {
            item.onSelect(ctx)
            focusEditor()
          })
          close()
        })
        ul.appendChild(li)
      })
      groupEl.appendChild(ul)
      scrollRoot.appendChild(groupEl)
    })

    menuEl.replaceChildren(tabNav, scrollRoot)
    scrollActiveTabIntoView()
  }

  function openMenu() {
    ensureDom()
    hideCrepeSlashMenu()
    const groups = getMenuGroups()
    if (!groups.length) {
      return
    }
    activeTab = 0
    renderMenu(groups)

    const { left, top } = getAnchorRect()
    themeWrap.style.left = `${left}px`
    themeWrap.style.top = `${top}px`

    host.style.display = 'block'
    open = true
    clampMenuToViewport()
    startSuppressingNativeMenu()
  }

  function close() {
    if (!host) {
      return
    }
    host.style.display = 'none'
    open = false
    anchorRectAtClick = null
    stopSuppressingNativeMenu()
    hideCrepeSlashMenu()
  }

  function scheduleOpenAfterBlockAdd() {
    if (openAfterAddFrame) {
      cancelAnimationFrame(openAfterAddFrame)
    }
    // Crepe BlockHandle stops pointerup propagation; run after its onAdd + menuAPI.show.
    openAfterAddFrame = requestAnimationFrame(() => {
      openAfterAddFrame = requestAnimationFrame(() => {
        openAfterAddFrame = 0
        hideCrepeSlashMenu()
        openMenu()
      })
    })
  }

  function onBlockAddPointerUpCapture(event) {
    if (!isBlockAddButton(event.target)) {
      return
    }
    const handle = event.target instanceof Element
      ? event.target.closest('.report-template-editor .milkdown-block-handle')
      : null
    if (handle instanceof HTMLElement) {
      // Capture before Crepe onAdd inserts a new block and moves the handle.
      anchorRectAtClick = handle.getBoundingClientRect()
    }
    scheduleOpenAfterBlockAdd()
  }

  function onDocumentKeyDown(event) {
    if (event.key === 'Escape' && open) {
      event.preventDefault()
      close()
    }
  }

  // Capture phase: Crepe add button calls stopPropagation on bubble.
  document.addEventListener('pointerup', onBlockAddPointerUpCapture, true)
  document.addEventListener('keydown', onDocumentKeyDown, true)

  return () => {
    if (openAfterAddFrame) {
      cancelAnimationFrame(openAfterAddFrame)
      openAfterAddFrame = 0
    }
    close()
    document.removeEventListener('pointerup', onBlockAddPointerUpCapture, true)
    document.removeEventListener('keydown', onDocumentKeyDown, true)
    host?.remove()
    host = null
    themeWrap = null
    menuEl = null
    backdrop = null
  }
}
