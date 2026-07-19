import { Schema } from 'prosemirror-model'
import { EditorState, NodeSelection, TextSelection } from 'prosemirror-state'
import {
  isSelectionInsideTableCell,
  resolveTableCellTextSelection
} from '@/components/ReportTemplateEditor/milkdown/table-cell-caret'

const schema = new Schema({
  nodes: {
    doc: { content: 'block+' },
    text: { group: 'inline' },
    paragraph: { group: 'block', content: 'inline*', toDOM: () => ['p', 0] },
    table: { group: 'block', content: 'table_row+', tableRole: 'table', isolating: true },
    table_row: { content: '(table_cell | table_header)+', tableRole: 'row' },
    table_cell: { content: 'block+', tableRole: 'cell', isolating: true },
    table_header: { content: 'block+', tableRole: 'header_cell', isolating: true },
    cat2bug_variable: {
      group: 'inline',
      inline: true,
      atom: true,
      attrs: { path: { default: '' }, raw: { default: '' } }
    }
  }
})

function buildTableDoc(cellContent) {
  const paragraph = schema.nodes.paragraph.create(null, cellContent)
  const cell = schema.nodes.table_cell.create(null, paragraph)
  const row = schema.nodes.table_row.create(null, cell)
  const table = schema.nodes.table.create(null, row)
  return schema.nodes.doc.create(null, table)
}

describe('table-cell-caret', () => {
  it('detects selections inside table cells', () => {
    const doc = buildTableDoc(schema.text('hi'))
    // table@0 > row@1 > cell@2 > paragraph@3
    const state = EditorState.create({
      doc,
      schema,
      selection: NodeSelection.create(doc, 3)
    })
    expect(isSelectionInsideTableCell(state.selection)).toBe(true)
  })

  it('converts cell paragraph NodeSelection to TextSelection (Milkdown first-click)', () => {
    const doc = buildTableDoc(schema.text('hi'))
    const state = EditorState.create({
      doc,
      schema,
      selection: NodeSelection.create(doc, 3)
    })
    expect(state.selection instanceof NodeSelection).toBe(true)
    expect(state.selection.node.type.name).toBe('paragraph')

    const next = resolveTableCellTextSelection(state)
    expect(next).toBeTruthy()
    expect(next instanceof TextSelection).toBe(true)
    expect(next.empty).toBe(true)
  })

  it('places caret after an in-cell variable atom NodeSelection', () => {
    const variable = schema.nodes.cat2bug_variable.create({
      path: 'api.plan.name',
      raw: '${api.plan.name[1]}'
    })
    const doc = buildTableDoc(variable)
    // paragraph@3 > variable@4
    const varPos = 4
    const state = EditorState.create({
      doc,
      schema,
      selection: NodeSelection.create(doc, varPos)
    })
    expect(state.selection.node.type.name).toBe('cat2bug_variable')

    const next = resolveTableCellTextSelection(state)
    expect(next).toBeTruthy()
    expect(next instanceof TextSelection).toBe(true)
    expect(next.from).toBeGreaterThanOrEqual(varPos + variable.nodeSize)
  })

  it('ignores NodeSelection outside tables', () => {
    const para = schema.nodes.paragraph.create(null, schema.text('outside'))
    const doc = schema.nodes.doc.create(null, para)
    const state = EditorState.create({
      doc,
      schema,
      selection: NodeSelection.create(doc, 0)
    })
    expect(resolveTableCellTextSelection(state)).toBeNull()
  })
})
