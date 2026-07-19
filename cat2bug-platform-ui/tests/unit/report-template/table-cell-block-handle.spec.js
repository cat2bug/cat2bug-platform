/** @jest-environment jsdom */
import { Schema } from 'prosemirror-model'
import {
  tableAwareFilterNodes,
  resolvePosNearBlockHandle,
  ensureParagraphAfterTable,
  isBlockishDslContent,
  insertBlockFragmentAfterTable
} from '@/components/ReportTemplateEditor/milkdown/table-cell-block-handle'
import { installReportBlockAddOverlay } from '@/components/ReportTemplateEditor/milkdown/report-block-add-overlay'

const schema = new Schema({
  nodes: {
    doc: { content: 'block+' },
    text: { group: 'inline' },
    paragraph: { group: 'block', content: 'inline*', toDOM: () => ['p', 0] },
    blockquote: { group: 'block', content: 'block+', toDOM: () => ['blockquote', 0] },
    table: { group: 'block', content: 'table_row+', tableRole: 'table', isolating: true },
    table_row: { content: '(table_cell | table_header)+', tableRole: 'row' },
    table_cell: { content: 'block+', tableRole: 'cell', isolating: true },
    table_header: { content: 'block+', tableRole: 'header_cell', isolating: true }
  }
})

function buildTableDoc() {
  const paragraph = schema.nodes.paragraph.create(null, schema.text('cell'))
  const cell = schema.nodes.table_cell.create(null, paragraph)
  const row = schema.nodes.table_row.create(null, cell)
  const table = schema.nodes.table.create(null, row)
  return schema.nodes.doc.create(null, [
    schema.nodes.paragraph.create(null, schema.text('outside')),
    table
  ])
}

describe('tableAwareFilterNodes', () => {
  it('does not attach doc block handles to cell paragraphs (walk up to table)', () => {
    const doc = buildTableDoc()
    let paraPos = null
    doc.descendants((node, pos) => {
      if (node.type.name === 'paragraph' && node.textContent === 'cell') {
        paraPos = pos
        return false
      }
      return undefined
    })
    expect(paraPos).not.toBeNull()
    const $para = doc.resolve(paraPos + 1)
    expect($para.parent.type.name).toBe('paragraph')
    expect(tableAwareFilterNodes($para, $para.parent)).toBe(false)
  })

  it('allows the whole table block handle (plus / drag before table)', () => {
    const doc = buildTableDoc()
    let tablePos = null
    doc.descendants((node, pos) => {
      if (node.type.name === 'table') {
        tablePos = pos
        return false
      }
      return undefined
    })
    const $table = doc.resolve(tablePos)
    const tableNode = $table.nodeAfter
    expect(tableNode.type.name).toBe('table')
    expect(tableAwareFilterNodes($table, tableNode)).toBe(true)
  })

  it('rejects table_row / table_cell (row/col use Milkdown table handles)', () => {
    const doc = buildTableDoc()
    let rowPos = null
    let cellPos = null
    doc.descendants((node, pos) => {
      if (node.type.name === 'table_row' && rowPos == null) {
        rowPos = pos
      }
      if (node.type.name === 'table_cell' && cellPos == null) {
        cellPos = pos
      }
    })
    const $row = doc.resolve(rowPos)
    expect(tableAwareFilterNodes($row, $row.nodeAfter)).toBe(false)
    const $cell = doc.resolve(cellPos)
    expect(tableAwareFilterNodes($cell, $cell.nodeAfter)).toBe(false)
  })

  it('still allows normal paragraphs outside tables', () => {
    const doc = buildTableDoc()
    const $outside = doc.resolve(1)
    expect($outside.parent.type.name).toBe('paragraph')
    expect(tableAwareFilterNodes($outside, $outside.parent)).toBe(true)
  })

  it('rejects nodes inside blockquote (Crepe behavior)', () => {
    const quotePara = schema.nodes.paragraph.create(null, schema.text('q'))
    const quote = schema.nodes.blockquote.create(null, quotePara)
    const doc = schema.nodes.doc.create(null, quote)
    const $pos = doc.resolve(2)
    expect(tableAwareFilterNodes($pos, $pos.parent)).toBe(false)
  })
})

describe('report-block-add-overlay table cell path', () => {
  let cleanup = () => {}

  beforeEach(() => {
    document.body.innerHTML = `
      <div class="report-template-editor">
        <div class="report-milkdown-editor milkdown"></div>
        <div class="milkdown-block-handle" style="position:absolute;left:10px;top:10px;width:40px;height:40px">
          <div class="operation-item">+</div>
          <div class="operation-item">::</div>
        </div>
      </div>
    `
  })

  afterEach(() => {
    cleanup()
    document.body.innerHTML = ''
  })

  it('opens menu immediately and skips Crepe onAdd when prepareTableCellInsert returns true', () => {
    let prepareCalls = 0
    let crepeOnAdd = 0
    cleanup = installReportBlockAddOverlay({
      getMenuGroups: () => [
        {
          key: 'plan',
          label: '计划',
          items: [{ key: 'var', label: '变量', onSelect: () => {} }]
        }
      ],
      runEditorAction: (fn) => fn({}),
      prepareTableCellInsert: () => {
        prepareCalls += 1
        return true
      }
    }).destroy

    const addBtn = document.querySelector('.milkdown-block-handle .operation-item')
    addBtn.addEventListener('pointerup', () => {
      crepeOnAdd += 1
    })
    addBtn.dispatchEvent(new MouseEvent('pointerup', { bubbles: true, cancelable: true }))

    expect(prepareCalls).toBe(1)
    expect(crepeOnAdd).toBe(0)
    const host = document.querySelector('.report-block-add-menu-host')
    expect(host).toBeTruthy()
    expect(host.style.display).toBe('block')
  })

  it('still opens custom menu and skips Crepe when prepare returns false', () => {
    let crepeOnAdd = 0
    cleanup = installReportBlockAddOverlay({
      getMenuGroups: () => [
        {
          key: 'text',
          label: '文本',
          items: [{ key: 'p', label: '正文', onSelect: () => {} }]
        }
      ],
      runEditorAction: (fn) => fn({}),
      prepareTableCellInsert: () => false
    }).destroy

    const addBtn = document.querySelector('.milkdown-block-handle .operation-item')
    addBtn.addEventListener('pointerup', () => {
      crepeOnAdd += 1
    })
    addBtn.dispatchEvent(new MouseEvent('pointerup', { bubbles: true, cancelable: true }))

    expect(crepeOnAdd).toBe(0)
    expect(document.querySelector('.report-block-add-menu-host')?.style.display).toBe('block')
  })
})

describe('resolvePosNearBlockHandle', () => {
  it('returns null without a view or handle', () => {
    expect(resolvePosNearBlockHandle(null, null)).toBeNull()
  })

  it('treats a handle below the table bottom as after-table, not last cell', () => {
    const doc = buildTableDoc()
    const root = document.createElement('div')
    document.body.appendChild(root)
    const tableEl = document.createElement('table')
    tableEl.getBoundingClientRect = () => ({
      top: 100,
      bottom: 200,
      left: 80,
      right: 400,
      width: 320,
      height: 100,
      x: 80,
      y: 100,
      toJSON() {
        return {}
      }
    })
    root.appendChild(tableEl)

    let cellPos = null
    doc.descendants((node, pos) => {
      if (node.type.name === 'paragraph' && node.textContent === 'cell') {
        cellPos = pos + 1
        return false
      }
      return undefined
    })

    const handle = document.createElement('div')
    handle.getBoundingClientRect = () => ({
      top: 202,
      bottom: 230,
      left: 10,
      right: 50,
      width: 40,
      height: 28,
      x: 10,
      y: 202,
      toJSON() {
        return {}
      }
    })

    const view = {
      dom: root,
      state: {
        doc,
        selection: { from: cellPos }
      },
      posAtCoords: () => ({ pos: cellPos, inside: cellPos }),
      nodeDOM: () => tableEl,
      dispatch() {},
      focus() {}
    }

    const resolved = resolvePosNearBlockHandle(view, handle)
    expect(resolved).toBeTruthy()
    expect(resolved.inTableCell).toBe(false)
    expect(resolved.placeAfterTable).toBe(true)

    root.remove()
  })
})

describe('ensureParagraphAfterTable', () => {
  it('inserts a paragraph after the table when nothing follows it', () => {
    const paragraph = schema.nodes.paragraph.create(null, schema.text('cell'))
    const cell = schema.nodes.table_cell.create(null, paragraph)
    const row = schema.nodes.table_row.create(null, cell)
    const table = schema.nodes.table.create(null, row)
    const doc = schema.nodes.doc.create(null, [table])

    const { EditorState, TextSelection } = require('prosemirror-state')
    let state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.near(doc.resolve(1), 1)
    })

    const view = {
      state,
      dispatch(tr) {
        state = state.apply(tr)
        view.state = state
      },
      focus() {}
    }

    expect(ensureParagraphAfterTable(view, 0)).toBe(true)
    expect(view.state.doc.childCount).toBe(2)
    expect(view.state.doc.lastChild.type.name).toBe('paragraph')

    let inTable = false
    for (let d = view.state.selection.$from.depth; d > 0; d -= 1) {
      if (view.state.selection.$from.node(d).type.name === 'table') {
        inTable = true
      }
    }
    expect(inTable).toBe(false)
  })
})

describe('insertBlockFragmentAfterTable', () => {
  it('places block content after the table, not in the last cell', () => {
    const paragraph = schema.nodes.paragraph.create(null, schema.text('cell'))
    const cell = schema.nodes.table_cell.create(null, paragraph)
    const row = schema.nodes.table_row.create(null, cell)
    const table = schema.nodes.table.create(null, row)
    const doc = schema.nodes.doc.create(null, [table])

    const { EditorState, TextSelection } = require('prosemirror-state')
    const { Fragment } = require('prosemirror-model')
    let state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.near(doc.resolve(1), 1)
    })
    const view = {
      state,
      dispatch(tr) {
        state = state.apply(tr)
        view.state = state
      },
      focus() {}
    }

    const imagePara = schema.nodes.paragraph.create(
      null,
      schema.text('$image{api.project.projectIcon}')
    )
    expect(insertBlockFragmentAfterTable(view, 0, Fragment.from(imagePara))).toBe(true)
    expect(view.state.doc.childCount).toBe(2)
    expect(view.state.doc.firstChild.type.name).toBe('table')
    expect(view.state.doc.lastChild.textContent).toContain('$image{api.project.projectIcon}')
    expect(view.state.doc.firstChild.textContent).not.toContain('$image')
  })

  it('replaces an empty placeholder paragraph after the table', () => {
    const paragraph = schema.nodes.paragraph.create(null, schema.text('cell'))
    const cell = schema.nodes.table_cell.create(null, paragraph)
    const row = schema.nodes.table_row.create(null, cell)
    const table = schema.nodes.table.create(null, row)
    const empty = schema.nodes.paragraph.create()
    const doc = schema.nodes.doc.create(null, [table, empty])

    const { EditorState, TextSelection } = require('prosemirror-state')
    const { Fragment } = require('prosemirror-model')
    let state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.near(doc.resolve(1), 1)
    })
    const view = {
      state,
      dispatch(tr) {
        state = state.apply(tr)
        view.state = state
      },
      focus() {}
    }

    const imagePara = schema.nodes.paragraph.create(
      null,
      schema.text('$image{api.project.projectIcon}')
    )
    expect(insertBlockFragmentAfterTable(view, 0, Fragment.from(imagePara))).toBe(true)
    expect(view.state.doc.childCount).toBe(2)
    expect(view.state.doc.lastChild.textContent).toContain('$image')
  })
})

describe('isBlockishDslContent', () => {
  it('detects only $table / $card (image is inline at caret)', () => {
    expect(isBlockishDslContent('$table{api.defect.list}')).toBe(true)
    expect(isBlockishDslContent('$card{api.case.list}')).toBe(true)
    expect(isBlockishDslContent('$image{api.project.projectIcon}')).toBe(false)
    expect(isBlockishDslContent('$image["style":"w:1"]{api.project.projectIcon}')).toBe(false)
    expect(isBlockishDslContent('${api.project.projectName}')).toBe(false)
  })
})
