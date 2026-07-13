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
      runEditorAction: (fn) => fn({ get: () => ({ focus: () => {} }) })
    })
  })

  afterEach(() => {
    cleanup()
    document.body.innerHTML = ''
  })

  function clickBlockAddLikeCrepe() {
    const addBtn = document.querySelector('.milkdown-block-handle .operation-item')
    addBtn.addEventListener('pointerup', (event) => {
      event.preventDefault()
      event.stopPropagation()
    })
    addBtn.dispatchEvent(new MouseEvent('pointerup', { bubbles: true }))
  }

  it('opens fixed overlay after block add click and keeps open while scrolling', async () => {
    clickBlockAddLikeCrepe()

    await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)))

    const host = document.querySelector('.report-block-add-menu-host')
    expect(host).toBeTruthy()
    expect(host.style.display).toBe('block')

    const scrollRoot = document.querySelector('.report-block-add-menu .menu-groups')
    for (let i = 0; i < 15; i++) {
      scrollRoot.dispatchEvent(
        new WheelEvent('wheel', { deltaY: 200, bubbles: true, cancelable: true })
      )
    }

    expect(document.querySelector('.report-block-add-menu-host').style.display).toBe('block')
    expect(scrollRoot.scrollTop).toBeGreaterThan(0)
  })

  it('keeps overlay open after simulated preview refresh mutates native slash menu', async () => {
    clickBlockAddLikeCrepe()
    await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)))

    const nativeMenu = document.querySelector('.report-template-editor .milkdown-slash-menu')
    nativeMenu.dataset.show = 'true'
    nativeMenu.style.display = 'block'

    await new Promise((resolve) => setTimeout(resolve, 20))

    expect(document.querySelector('.report-block-add-menu-host').style.display).toBe('block')
    expect(nativeMenu.dataset.show).toBe('false')
    expect(nativeMenu.style.display).toBe('none')
  })

  it('anchors menu to handle position at click time, not after handle moves', async () => {
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

    clickBlockAddLikeCrepe()

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

    await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)))

    const themeWrap = document.querySelector('.report-block-add-menu-theme')
    expect(themeWrap).toBeTruthy()
    const menuTop = Number.parseFloat(themeWrap.style.top)
    expect(menuTop).toBeLessThan(200)
    expect(menuTop).not.toBe(600)
  })

  it('restores captured selection before running menu item insert', async () => {
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
    })

    clickBlockAddLikeCrepe()
    await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(resolve)))

    const menuItem = document.querySelector('.report-block-add-menu li[data-index="0"]')
    menuItem.dispatchEvent(new MouseEvent('pointerup', { bubbles: true, cancelable: true }))

    expect(captured).toBe(true)
    expect(restored).toBe(true)
  })
})
