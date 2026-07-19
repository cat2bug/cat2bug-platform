import { Schema } from 'prosemirror-model'
import { EditorState, TextSelection } from 'prosemirror-state'
import { tableNodes } from 'prosemirror-tables'
import {
  insertTableCellLineBreak,
  moveToNextTableCell
} from '@/components/ReportTemplateEditor/milkdown/table-cell-enter'
import { buildConvertTableHtmlBreakTransaction } from '@/components/ReportTemplateEditor/milkdown/table-cell-html-break'
import { getTableCellPlainText } from '@/components/ReportTemplateEditor/dsl/serialize-multimd-table'
import { createTableCellInlineContent } from '@/components/ReportTemplateEditor/dsl/parse-multimd-table'

const tableNodeSpecs = tableNodes({
  tableGroup: 'block',
  cellContent: 'paragraph',
  cellAttributes: {}
})

const schema = new Schema({
  nodes: {
    doc: { content: 'block+' },
    text: { group: 'inline' },
    paragraph: { group: 'block', content: 'inline*', toDOM: () => ['p', 0] },
    hardbreak: {
      inline: true,
      group: 'inline',
      selectable: false,
      leafText: () => '\n',
      toDOM: () => ['br']
    },
    html: {
      inline: true,
      group: 'inline',
      atom: true,
      attrs: { value: { default: '' } },
      toDOM: (node) => ['span', { 'data-type': 'html' }, node.attrs.value]
    },
    table: tableNodeSpecs.table,
    table_row: tableNodeSpecs.table_row,
    table_cell: tableNodeSpecs.table_cell,
    table_header: tableNodeSpecs.table_header
  }
})

function buildTwoCellDoc() {
  const p1 = schema.nodes.paragraph.create(null, schema.text('a'))
  const p2 = schema.nodes.paragraph.create(null, schema.text('b'))
  const c1 = schema.nodes.table_cell.create(null, p1)
  const c2 = schema.nodes.table_cell.create(null, p2)
  const row = schema.nodes.table_row.create(null, [c1, c2])
  const table = schema.nodes.table.create(null, row)
  const after = schema.nodes.paragraph.create(null, schema.text('tail'))
  return schema.nodes.doc.create(null, [table, after])
}

describe('table-cell-enter-keymap', () => {
  it('moves caret to the next cell on Enter and does not leave the table', () => {
    const doc = buildTwoCellDoc()
    // table@0 > row@1 > cell@2 > paragraph@3 > text "a"
    const state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.create(doc, 4)
    })
    let nextState = state
    const ok = moveToNextTableCell(state, (tr) => {
      nextState = state.apply(tr)
    })
    expect(ok).toBe(true)
    const $head = nextState.selection.$head
    let inSecondCell = false
    for (let d = $head.depth; d > 0; d -= 1) {
      if ($head.node(d).type.name === 'table_cell' && $head.node(d).textContent === 'b') {
        inSecondCell = true
      }
    }
    expect(inSecondCell).toBe(true)
    expect(nextState.doc.lastChild.textContent).toBe('tail')
  })

  it('consumes Enter in the last cell without exiting the table', () => {
    const doc = buildTwoCellDoc()
    // second cell text starts after first cell
    const state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.create(doc, 9)
    })
    let dispatched = false
    const ok = moveToNextTableCell(state, () => {
      dispatched = true
    })
    expect(ok).toBe(true)
    expect(dispatched).toBe(false)
  })

  it('inserts a hardbreak in the cell on Mod-Enter', () => {
    const doc = buildTwoCellDoc()
    const state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.create(doc, 5) // after "a"
    })
    let nextState = state
    const ok = insertTableCellLineBreak(state, (tr) => {
      nextState = state.apply(tr)
    })
    expect(ok).toBe(true)
    const cell = nextState.doc.nodeAt(2)
    expect(getTableCellPlainText(cell)).toBe('a<br>')
  })

  it('ignores Enter outside tables', () => {
    const para = schema.nodes.paragraph.create(null, schema.text('x'))
    const doc = schema.nodes.doc.create(null, para)
    const state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.create(doc, 1)
    })
    expect(moveToNextTableCell(state)).toBe(false)
    expect(insertTableCellLineBreak(state)).toBe(false)
  })
})

describe('table-cell hardbreak serialize/parse', () => {
  it('serializes hardbreak as <br> and rebuilds it from cell text', () => {
    const inline = createTableCellInlineContent(schema, 'hello<br>world')
    expect(inline).toBeTruthy()
    const paragraph = schema.nodes.paragraph.create(null, inline)
    const cell = schema.nodes.table_cell.create(null, paragraph)
    expect(getTableCellPlainText(cell)).toBe('hello<br>world')
    expect(inline.child(1).type.name).toBe('hardbreak')
  })

  it('converts html <br> atoms inside table cells to hardbreak', () => {
    const htmlBr = schema.nodes.html.create({ value: '<br>' })
    const paragraph = schema.nodes.paragraph.create(null, [
      schema.text('a'),
      htmlBr,
      schema.text('b')
    ])
    const cell = schema.nodes.table_cell.create(null, paragraph)
    const row = schema.nodes.table_row.create(null, cell)
    const table = schema.nodes.table.create(null, row)
    const doc = schema.nodes.doc.create(null, table)
    const state = EditorState.create({ doc, schema })
    const tr = buildConvertTableHtmlBreakTransaction(state)
    expect(tr).toBeTruthy()
    const next = state.apply(tr)
    expect(getTableCellPlainText(next.doc.nodeAt(0).child(0).child(0))).toBe('a<br>b')
  })
})
