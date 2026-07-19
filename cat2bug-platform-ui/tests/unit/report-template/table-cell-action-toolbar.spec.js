/** @jest-environment jsdom */

import { Schema } from 'prosemirror-model'
import { EditorState, TextSelection } from 'prosemirror-state'
import { EditorView } from 'prosemirror-view'
import { tableNodes, CellSelection } from 'prosemirror-tables'
import {
  CELL_ACTION_TOOLBAR_CLASS,
  installTableCellActionToolbar,
  isNonEmptyInlineTextSelection,
  resolveTopCellDom,
  resolveTopCellPos
} from '@/components/ReportTemplateEditor/milkdown/table-cell-action-toolbar'

const tableNodeSpecs = tableNodes({
  tableGroup: 'block',
  cellContent: 'paragraph+',
  cellAttributes: {}
})

const schema = new Schema({
  nodes: {
    doc: { content: 'block+' },
    text: { group: 'inline' },
    paragraph: {
      group: 'block',
      content: 'inline*',
      toDOM: () => ['p', 0],
      parseDOM: [{ tag: 'p' }]
    },
    ...tableNodeSpecs
  }
})

function buildTwoByTwo() {
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

describe('table-cell-action-toolbar', () => {
  let root
  let view
  let cleanup = () => {}

  beforeEach(() => {
    root = document.createElement('div')
    document.body.appendChild(root)
  })

  afterEach(() => {
    cleanup()
    view?.destroy()
    root?.remove()
    document.querySelectorAll(`.${CELL_ACTION_TOOLBAR_CLASS}`).forEach((n) => n.remove())
  })

  it('resolveTopCellPos picks the earliest cell in a CellSelection', () => {
    const doc = buildTwoByTwo()
    const selection = CellSelection.create(doc, 2, 14)
    const state = EditorState.create({ doc, schema, selection })
    expect(resolveTopCellPos(state)).toBe(2)
  })

  it('resolveTopCellDom returns the top-left selected cell element', () => {
    const doc = buildTwoByTwo()
    const selection = CellSelection.create(doc, 2, 14)
    const state = EditorState.create({ doc, schema, selection })
    view = new EditorView(root, { state })
    const top = resolveTopCellDom(view)
    expect(top).toBeTruthy()
    expect(top.tagName).toMatch(/TH|TD/)
  })

  it('stays hidden when caret is in a table cell but the user has not interacted', async () => {
    const doc = buildTwoByTwo()
    let textPos = null
    doc.descendants((node, pos) => {
      if (textPos != null) {
        return false
      }
      if (node.isText && node.text === 'c') {
        textPos = pos
        return false
      }
      return true
    })
    const state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.create(doc, textPos)
    })
    view = new EditorView(root, { state })
    // Drawer autofocus without a real click/type should not reveal the bar.
    view.focus()

    cleanup = installTableCellActionToolbar({
      getView: () => view,
      onAdd: () => {}
    })

    await new Promise((resolve) => requestAnimationFrame(resolve))
    await new Promise((resolve) => setTimeout(resolve, 250))

    const bar = document.querySelector(`.${CELL_ACTION_TOOLBAR_CLASS}`)
    expect(bar).toBeTruthy()
    expect(bar.style.display).toBe('none')
    expect(bar.getAttribute('data-show')).toBe('false')
  })

  it('shows icon buttons for 添加/合并/拆分 when caret is inside a table cell', async () => {
    const doc = buildTwoByTwo()
    let textPos = null
    doc.descendants((node, pos) => {
      if (textPos != null) {
        return false
      }
      if (node.isText && node.text === 'c') {
        textPos = pos
        return false
      }
      return true
    })
    expect(textPos).not.toBeNull()
    const state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.create(doc, textPos)
    })
    view = new EditorView(root, { state })
    view.focus()

    let opened = null
    cleanup = installTableCellActionToolbar({
      getView: () => view,
      onAdd: (rect) => {
        opened = rect
      }
    })

    view.dom.dispatchEvent(new KeyboardEvent('keydown', { bubbles: true, key: 'ArrowRight' }))
    await new Promise((resolve) => requestAnimationFrame(resolve))
    await new Promise((resolve) => setTimeout(resolve, 250))

    const bar = document.querySelector(`.${CELL_ACTION_TOOLBAR_CLASS}`)
    expect(bar).toBeTruthy()
    expect(bar.style.display).toBe('flex')
    const addBtn = bar.querySelector('button[aria-label="添加"]')
    const mergeBtn = bar.querySelector('button[aria-label="合并"]')
    const splitBtn = bar.querySelector('button[aria-label="拆分"]')
    expect(addBtn).toBeTruthy()
    expect(mergeBtn).toBeTruthy()
    expect(splitBtn).toBeTruthy()
    expect(addBtn.querySelector('svg')).toBeTruthy()
    expect(mergeBtn.querySelector('svg')).toBeTruthy()
    expect(splitBtn.querySelector('svg')).toBeTruthy()

    addBtn.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }))
    expect(opened).toBeTruthy()
  })

  it('hides the toolbar when selection leaves the table', async () => {
    const outside = schema.nodes.paragraph.create(null, schema.text('out'))
    const doc = schema.nodes.doc.create(null, [outside, buildTwoByTwo().firstChild])
    const state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.create(doc, 1)
    })
    view = new EditorView(root, { state })

    cleanup = installTableCellActionToolbar({
      getView: () => view,
      onAdd: () => {}
    })
    await new Promise((resolve) => setTimeout(resolve, 250))

    const bar = document.querySelector(`.${CELL_ACTION_TOOLBAR_CLASS}`)
    expect(bar.style.display).toBe('none')
  })

  it('isNonEmptyInlineTextSelection is true for ranged text, false for caret/cells', () => {
    const doc = buildTwoByTwo()
    let from = null
    let to = null
    doc.descendants((node, pos) => {
      if (from != null) {
        return false
      }
      if (node.isText && node.text === 'c') {
        from = pos
        to = pos + node.nodeSize
        return false
      }
      return true
    })
    const ranged = EditorState.create({
      doc,
      schema,
      selection: TextSelection.create(doc, from, to)
    })
    expect(isNonEmptyInlineTextSelection(ranged)).toBe(true)

    const caret = EditorState.create({
      doc,
      schema,
      selection: TextSelection.create(doc, from)
    })
    expect(isNonEmptyInlineTextSelection(caret)).toBe(false)

    const cells = EditorState.create({
      doc,
      schema,
      selection: CellSelection.create(doc, 2, 14)
    })
    expect(isNonEmptyInlineTextSelection(cells)).toBe(false)
  })

  it('hides when a non-empty text string is selected (Crepe format toolbar owns that)', async () => {
    const doc = buildTwoByTwo()
    let from = null
    let to = null
    doc.descendants((node, pos) => {
      if (from != null) {
        return false
      }
      if (node.isText && node.text === 'c') {
        from = pos
        to = pos + node.nodeSize
        return false
      }
      return true
    })
    const state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.create(doc, from, to)
    })
    view = new EditorView(root, { state })
    view.focus()

    cleanup = installTableCellActionToolbar({
      getView: () => view,
      onAdd: () => {}
    })
    await new Promise((resolve) => setTimeout(resolve, 250))

    const bar = document.querySelector(`.${CELL_ACTION_TOOLBAR_CLASS}`)
    expect(bar.style.display).toBe('none')
  })
})
