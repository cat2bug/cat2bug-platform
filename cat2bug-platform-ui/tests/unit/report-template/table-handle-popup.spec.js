/** @jest-environment jsdom */

import { Schema } from 'prosemirror-model'
import { EditorState, TextSelection } from 'prosemirror-state'
import { tableNodes, CellSelection } from 'prosemirror-tables'
import {
  TABLE_OPS_OPEN_CLASS,
  isTableColOrRowSelection,
  syncTableHandlePopup
} from '@/components/ReportTemplateEditor/milkdown/table-handle-popup'

const tableNodeSpecs = tableNodes({
  tableGroup: 'block',
  cellContent: 'paragraph+',
  cellAttributes: {}
})

const schema = new Schema({
  nodes: {
    doc: { content: 'block+' },
    text: { group: 'inline' },
    paragraph: { group: 'block', content: 'inline*' },
    ...tableNodeSpecs
  }
})

function buildTableDoc() {
  const paragraph = (text) => schema.nodes.paragraph.create(null, schema.text(text))
  const header = (text) => schema.nodes.table_header.create(null, paragraph(text))
  const cell = (text) => schema.nodes.table_cell.create(null, paragraph(text))
  const row = (...cells) => schema.nodes.table_row.create(null, cells)
  const table = schema.nodes.table.create(null, [
    row(header('a'), header('b')),
    row(cell('c'), cell('d'))
  ])
  return schema.nodes.doc.create(null, table)
}

function mockView(selection, handleHtml) {
  const wrap = document.createElement('div')
  wrap.className = 'ProseMirror'
  wrap.innerHTML = `
    <div class="milkdown-table-block">
      ${handleHtml}
    </div>
  `
  document.body.appendChild(wrap)
  const block = wrap.querySelector('.milkdown-table-block')
  const state = EditorState.create({
    doc: selection.$anchor.doc,
    schema,
    selection
  })
  return {
    dom: wrap,
    state,
    dispatch: () => {},
    nodeDOM: () => block,
    destroy() {
      wrap.remove()
    }
  }
}

describe('table-handle-popup', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('detects column CellSelection', () => {
    const doc = buildTableDoc()
    const selection = CellSelection.create(doc, 2, 14)
    expect(selection.isColSelection()).toBe(true)
    expect(isTableColOrRowSelection(selection)).toBe(true)
  })

  it('keeps popup via is-ops-open even when data-show was cleared', () => {
    const doc = buildTableDoc()
    const selection = CellSelection.create(doc, 2, 14)
    const view = mockView(
      selection,
      `<div data-role="col-drag-handle" data-show="false" class="handle cell-handle">
         <div class="button-group" data-show="false"></div>
       </div>`
    )
    expect(syncTableHandlePopup(view)).toBe(true)
    const handle = view.dom.querySelector('[data-role="col-drag-handle"]')
    expect(handle.classList.contains(TABLE_OPS_OPEN_CLASS)).toBe(true)
    expect(handle.dataset.show).toBe('true')
    expect(view.dom.querySelector('.button-group').dataset.show).toBe('true')
    view.destroy()
  })

  it('is idempotent: second sync does not thrash classList when already open', () => {
    const doc = buildTableDoc()
    const selection = CellSelection.create(doc, 2, 14)
    const view = mockView(
      selection,
      `<div data-role="col-drag-handle" data-show="false" class="handle cell-handle">
         <div class="button-group" data-show="false"></div>
       </div>`
    )
    syncTableHandlePopup(view)
    const handle = view.dom.querySelector('[data-role="col-drag-handle"]')
    let classChanges = 0
    const observer = new MutationObserver((mutations) => {
      classChanges += mutations.filter((m) => m.attributeName === 'class').length
    })
    observer.observe(handle, { attributes: true, attributeFilter: ['class', 'data-show'] })
    syncTableHandlePopup(view)
    syncTableHandlePopup(view)
    observer.disconnect()
    expect(classChanges).toBe(0)
    expect(handle.classList.contains(TABLE_OPS_OPEN_CLASS)).toBe(true)
    view.destroy()
  })

  it('clears is-ops-open when selection is not col/row', () => {
    const doc = buildTableDoc()
    const state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.create(doc, 4)
    })
    const view = mockView(
      state.selection,
      `<div data-role="col-drag-handle" data-show="true" class="handle cell-handle ${TABLE_OPS_OPEN_CLASS}">
         <div class="button-group" data-show="true"></div>
       </div>`
    )
    // Override with text selection state (mockView rebuilds from selection.$anchor.doc)
    view.state = state
    expect(syncTableHandlePopup(view)).toBe(false)
    expect(
      view.dom.querySelector('[data-role="col-drag-handle"]').classList.contains(TABLE_OPS_OPEN_CLASS)
    ).toBe(false)
    view.destroy()
  })

  it('keeps column handle popup open without injecting merge buttons', () => {
    const doc = buildTableDoc()
    const selection = CellSelection.create(doc, 2, 14)
    expect(selection.isColSelection()).toBe(true)
    const view = mockView(
      selection,
      `<div data-role="col-drag-handle" data-show="false" class="handle cell-handle">
         <div class="button-group" data-show="false"></div>
       </div>`
    )
    expect(syncTableHandlePopup(view)).toBe(true)
    expect(view.dom.querySelector('[data-cat2bug-merge-action]')).toBeNull()
    expect(
      view.dom.querySelector('[data-role="col-drag-handle"]').classList.contains(TABLE_OPS_OPEN_CLASS)
    ).toBe(true)
    view.destroy()
  })
})
