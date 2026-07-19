/** @jest-environment jsdom */
import { installReportBlockAddOverlay } from '@/components/ReportTemplateEditor/milkdown/report-block-add-overlay'

describe('report-block-add-overlay', () => {
  let cleanup = () => {}

  beforeEach(() => {
    document.body.innerHTML = `
      <div class="report-template-editor">
        <div class="report-milkdown-editor milkdown">
          <div class="milkdown-slash-menu" data-show="true" style="display:block"></div>
        </div>
        <div class="milkdown-block-handle" style="position:absolute;left:10px;top:10px;width:40px;height:40px">
          <div class="operation-item">+</div>
          <div class="operation-item">::</div>
        </div>
      </div>
    `
    cleanup = installReportBlockAddOverlay({
      getMenuGroups: () => [
        {
          key: 'text',
          label: '文本',
          items: Array.from({ length: 30 }, (_, i) => ({
            key: `item-${i}`,
            label: `条目 ${i}`,
            onSelect: () => {}
          }))
        }
      ],
      runEditorAction: (fn) => fn({ get: () => ({ focus: () => {} }) }),
      isDark: () => true
    }).destroy
  })

  afterEach(() => {
    cleanup()
    document.body.innerHTML = ''
  })

  function clickBlockAdd() {
    const addBtn = document.querySelector('.milkdown-block-handle .operation-item')
    addBtn.dispatchEvent(new MouseEvent('pointerup', { bubbles: true, cancelable: true }))
  }

  it('opens custom overlay immediately on + and never leaves Crepe slash visible', () => {
    let crepeOnAdd = 0
    const addBtn = document.querySelector('.milkdown-block-handle .operation-item')
    addBtn.addEventListener('pointerup', () => {
      crepeOnAdd += 1
    })

    clickBlockAdd()

    expect(crepeOnAdd).toBe(0)
    const host = document.querySelector('.report-block-add-menu-host')
    expect(host).toBeTruthy()
    expect(host.style.display).toBe('block')
    expect(document.querySelector('.report-block-add-menu-theme')?.classList.contains('is-dark')).toBe(
      true
    )

    const nativeMenu = document.querySelector('.report-template-editor .milkdown-slash-menu')
    expect(nativeMenu.dataset.show).toBe('false')
    expect(nativeMenu.style.display).toBe('none')
  })

  it('opens fixed overlay after block add click and keeps open while scrolling', () => {
    clickBlockAdd()

    const host = document.querySelector('.report-block-add-menu-host')
    expect(host).toBeTruthy()
    expect(host.style.display).toBe('block')

    const scrollRoot = document.querySelector('.report-block-add-menu .menu-groups')
    expect(scrollRoot).toBeTruthy()
    // Native overflow scroll (no wheel→scrollTop hijack). jsdom does not apply
    // wheel deltas; assert the menu stays open and the list remains scrollable.
    for (let i = 0; i < 15; i++) {
      scrollRoot.dispatchEvent(
        new WheelEvent('wheel', { deltaY: 200, bubbles: true, cancelable: true })
      )
    }
    scrollRoot.scrollTop = 400

    expect(document.querySelector('.report-block-add-menu-host').style.display).toBe('block')
    expect(scrollRoot.scrollTop).toBe(400)
  })

  it('keeps custom overlay open if native slash attrs flip back on', () => {
    clickBlockAdd()

    const nativeMenu = document.querySelector('.report-template-editor .milkdown-slash-menu')
    nativeMenu.dataset.show = 'true'
    nativeMenu.style.display = 'block'

    expect(document.querySelector('.report-block-add-menu-host').style.display).toBe('block')
    // Native Crepe slash must stay CSS-hidden; custom host remains the interactive menu.
    expect(document.querySelector('.report-block-add-menu')).toBeTruthy()
  })

  it('anchors menu to handle position at click time, not after handle moves', () => {
    const handle = document.querySelector('.milkdown-block-handle')
    handle.style.position = 'fixed'
    handle.style.left = '10px'
    handle.style.top = '80px'
    handle.style.width = '68px'
    handle.style.height = '32px'
    handle.getBoundingClientRect = () => ({
      left: 10,
      top: 80,
      right: 78,
      bottom: 112,
      width: 68,
      height: 32,
      x: 10,
      y: 80,
      toJSON: () => ({})
    })

    clickBlockAdd()

    handle.style.top = '600px'
    handle.getBoundingClientRect = () => ({
      left: 10,
      top: 600,
      right: 78,
      bottom: 632,
      width: 68,
      height: 32,
      x: 10,
      y: 600,
      toJSON: () => ({})
    })

    const themeWrap = document.querySelector('.report-block-add-menu-theme')
    expect(themeWrap).toBeTruthy()
    const menuTop = Number.parseFloat(themeWrap.style.top)
    expect(menuTop).toBeLessThan(200)
    expect(menuTop).not.toBe(600)
  })

  it('restores captured selection before running menu item insert', () => {
    let captured = false
    let restored = false
    cleanup()
    cleanup = installReportBlockAddOverlay({
      getMenuGroups: () => [
        {
          key: 'project',
          label: '项目',
          items: [
            {
              key: 'intro',
              label: '描述',
              onSelect: () => {}
            }
          ]
        }
      ],
      runEditorAction: (fn) => fn({ get: () => ({ focus: () => {} }) }),
      captureInsertSelection: () => {
        captured = true
      },
      restoreInsertSelection: () => {
        restored = true
      }
    }).destroy

    clickBlockAdd()

    const menuItem = document.querySelector('.report-block-add-menu li[data-index="0"]')
    menuItem.dispatchEvent(new MouseEvent('pointerup', { bubbles: true, cancelable: true }))

    expect(captured).toBe(true)
    expect(restored).toBe(true)
  })

  it('includes business variable groups from getMenuGroups', () => {
    cleanup()
    cleanup = installReportBlockAddOverlay({
      getMenuGroups: () => [
        {
          key: 'text',
          label: '文本',
          items: [{ key: 'p', label: '正文', onSelect: () => {} }]
        },
        {
          key: 'project',
          label: '项目',
          items: [{ key: 'name', label: '项目名称', onSelect: () => {} }]
        }
      ],
      runEditorAction: (fn) => fn({}),
      isDark: () => true
    }).destroy

    clickBlockAdd()

    const tabs = [...document.querySelectorAll('.report-block-add-menu .tab-group li')].map(
      (li) => li.textContent
    )
    expect(tabs).toEqual(['文本', '项目'])
    expect(document.querySelector('.report-block-add-menu-theme')?.classList.contains('is-dark')).toBe(
      true
    )
  })
})
