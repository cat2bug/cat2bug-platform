/** @jest-environment jsdom */

import {
  PAGE_KBD_OVERLAY_ID,
  hasActivePageRowKbdHints,
  resolveElTableRoot,
  resolvePageActionLetter,
  resolveTableRowFirstColumnHintAnchor,
  resolveTableRowFirstColumnKbdBadgeLayout
} from '@/utils/defect-row-kbd-hints'

describe('page-action-kbd helpers', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })

  it('resolvePageActionLetter normalizes digit row keys', () => {
    expect(resolvePageActionLetter({ key: '1' })).toBe('1')
    expect(resolvePageActionLetter({ key: 'a' })).toBe('A')
    expect(resolvePageActionLetter({ key: 'Z' })).toBe('Z')
    expect(resolvePageActionLetter({ code: 'Digit3', key: 'Process' })).toBe('3')
    expect(resolvePageActionLetter({ code: 'Numpad7', key: 'Process' })).toBe('7')
  })

  it('hasActivePageRowKbdHints detects row hint badges in overlay', () => {
    expect(hasActivePageRowKbdHints()).toBe(false)
    const overlay = document.createElement('div')
    overlay.id = PAGE_KBD_OVERLAY_ID
    const badge = document.createElement('span')
    badge.className = 'cat2bug-page-kbd-hint-float defect-row-kbd-hint-float'
    badge.textContent = '1'
    overlay.appendChild(badge)
    document.body.appendChild(overlay)
    expect(hasActivePageRowKbdHints()).toBe(true)
  })

  it('resolveTableRowFirstColumnHintAnchor prefers fixed column first visible cell', () => {
    document.body.innerHTML = `
      <div class="cat2-bug-table-wrap">
        <div class="el-table">
          <div class="el-table__fixed">
            <div class="el-table__fixed-body-wrapper">
              <table><tbody>
                <tr>
                  <td class="is-hidden el-table__cell" style="width:0;height:0;overflow:hidden"><div class="cell">#0</div></td>
                  <td class="el-table__cell"><div class="cell plan-name">Plan A</div></td>
                </tr>
              </tbody></table>
            </div>
          </div>
          <div class="el-table__body-wrapper">
            <table><tbody>
              <tr class="el-table__row">
                <td class="is-hidden el-table__cell"><div class="cell">#0</div></td>
                <td class="el-table__cell"><div class="cell">Plan A</div></td>
              </tr>
            </tbody></table>
          </div>
        </div>
      </div>
    `
    const wrap = document.querySelector('.cat2-bug-table-wrap')
    const tableRoot = resolveElTableRoot(wrap)
    const mainTr = document.querySelector('.el-table__body-wrapper tr')
    const fixedCell = document.querySelector('.el-table__fixed .plan-name')

    HTMLElement.prototype.getBoundingClientRect = function mockRect() {
      if (this.classList && this.classList.contains('is-hidden')) {
        return { top: 0, left: 0, right: 0, bottom: 0, width: 0, height: 0 }
      }
      if (this.classList && this.classList.contains('plan-name')) {
        return { top: 10, left: 20, right: 180, bottom: 34, width: 160, height: 24 }
      }
      if (this.classList && this.classList.contains('el-table__cell') && this.querySelector('.plan-name')) {
        return { top: 8, left: 18, right: 182, bottom: 36, width: 164, height: 28 }
      }
      return { top: 8, left: 18, right: 182, bottom: 36, width: 164, height: 28 }
    }

    const anchor = resolveTableRowFirstColumnHintAnchor(
      mainTr,
      tableRoot,
      null,
      { field: 'planId', value: '1' }
    )
    expect(anchor).toBe(fixedCell)

    const rect = resolveTableRowFirstColumnKbdBadgeLayout(mainTr, tableRoot, null, { field: 'planId', value: '1' })
    expect(rect.floatOffset.placement).toBe('bottom-right-outset')
    expect(rect.rect.left).toBe(20)
    expect(rect.rect.right).toBe(180)
  })

  it('resolveTableRowFirstColumnKbdBadgeLayout centers badge when cell text is empty', () => {
    document.body.innerHTML = `
      <div class="el-table">
        <div class="el-table__body-wrapper">
          <table><tbody>
            <tr class="el-table__row">
              <td class="el-table__cell"><div class="cell empty-cell"></div></td>
            </tr>
          </tbody></table>
        </div>
      </div>
    `
    const tableRoot = document.querySelector('.el-table')
    const mainTr = document.querySelector('tr')
    const cell = document.querySelector('.cell')

    HTMLElement.prototype.getBoundingClientRect = function mockRect() {
      if (this.classList && this.classList.contains('empty-cell')) {
        return { top: 10, left: 20, right: 120, bottom: 40, width: 100, height: 30 }
      }
      if (this.classList && this.classList.contains('el-table__cell')) {
        return { top: 8, left: 18, right: 122, bottom: 42, width: 104, height: 34 }
      }
      return { top: 8, left: 18, right: 122, bottom: 42, width: 104, height: 34 }
    }

    const layout = resolveTableRowFirstColumnKbdBadgeLayout(mainTr, tableRoot, null, null)
    expect(layout.floatOffset.placement).toBe('center-cell')
    expect(layout.rect.width).toBe(104)
  })
})
