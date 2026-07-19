import { Schema } from 'prosemirror-model'
import { EditorState, TextSelection } from 'prosemirror-state'
import { resolveBusinessInsertMode } from '@/components/ReportTemplateEditor/milkdown/business-insert-mode'

const schema = new Schema({
  nodes: {
    doc: { content: 'block+' },
    text: { group: 'inline' },
    paragraph: { group: 'block', content: 'inline*', toDOM: () => ['p', 0] },
    table: { group: 'block', content: 'table_row+', tableRole: 'table', isolating: true },
    table_row: { content: '(table_cell | table_header)+', tableRole: 'row' },
    table_cell: { content: 'block+', tableRole: 'cell', isolating: true },
    table_header: { content: 'block+', tableRole: 'header_cell', isolating: true }
  }
})

describe('resolveBusinessInsertMode', () => {
  it('uses inline-text inside table cells to avoid splitting the table', () => {
    const paragraph = schema.nodes.paragraph.create(null, schema.text('a'))
    const cell = schema.nodes.table_cell.create(null, paragraph)
    const row = schema.nodes.table_row.create(null, cell)
    const table = schema.nodes.table.create(null, row)
    const doc = schema.nodes.doc.create(null, table)
    const state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.create(doc, 4)
    })
    expect(resolveBusinessInsertMode(state.selection)).toBe('inline-text')
  })

  it('uses block insert on an empty top-level paragraph', () => {
    const paragraph = schema.nodes.paragraph.create()
    const doc = schema.nodes.doc.create(null, paragraph)
    const state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.create(doc, 1)
    })
    expect(resolveBusinessInsertMode(state.selection)).toBe('block')
  })

  it('uses block-newline when the caret is mid-paragraph outside tables', () => {
    const paragraph = schema.nodes.paragraph.create(null, schema.text('hello'))
    const doc = schema.nodes.doc.create(null, paragraph)
    const state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.create(doc, 3)
    })
    expect(resolveBusinessInsertMode(state.selection)).toBe('block-newline')
  })

  it('uses inline-text for $image even mid-paragraph so it does not replace the line', () => {
    const paragraph = schema.nodes.paragraph.create(null, schema.text('hello world'))
    const doc = schema.nodes.doc.create(null, paragraph)
    const state = EditorState.create({
      doc,
      schema,
      selection: TextSelection.create(doc, 6)
    })
    expect(
      resolveBusinessInsertMode(
        state.selection,
        '$image["style":"width:100px;height:100px;"]{api.project.projectIcon}'
      )
    ).toBe('inline-text')
  })
})
