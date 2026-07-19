import { Schema, Fragment } from 'prosemirror-model'
import { EditorState } from 'prosemirror-state'
import { tableNodes, CellSelection, TableMap } from 'prosemirror-tables'
import {
  canMergeCells,
  runMergeCells,
  runSplitCell,
  getMergeToolbarState,
  isMergeToolbarSelection
} from '@/components/ReportTemplateEditor/milkdown/table-cell-merge'

const tableNodeSpecs = tableNodes({
  tableGroup: 'block',
  cellContent: 'paragraph',
  cellAttributes: {
    alignment: {
      default: 'left',
      getFromDOM: () => 'left',
      setDOMAttr: () => {}
    }
  }
})

/** Milkdown-like schema: single paragraph per cell + dedicated header row type */
const schema = new Schema({
  nodes: {
    doc: { content: 'block+' },
    paragraph: {
      group: 'block',
      content: 'inline*',
      toDOM: () => ['p', 0],
      parseDOM: [{ tag: 'p' }]
    },
    text: { group: 'inline' },
    table: {
      ...tableNodeSpecs.table,
      content: 'table_header_row table_row+',
      tableRole: 'table'
    },
    table_header_row: {
      ...tableNodeSpecs.table_row,
      content: '(table_header)*',
      tableRole: 'row'
    },
    table_row: tableNodeSpecs.table_row,
    table_cell: tableNodeSpecs.table_cell,
    table_header: tableNodeSpecs.table_header
  }
})

function para(text) {
  return text
    ? schema.nodes.paragraph.create(null, schema.text(text))
    : schema.nodes.paragraph.createAndFill()
}

function th(text, attrs = {}) {
  return schema.nodes.table_header.create(
    { colspan: 1, rowspan: 1, colwidth: null, alignment: 'left', ...attrs },
    Fragment.from(para(text))
  )
}

function td(text, attrs = {}) {
  return schema.nodes.table_cell.create(
    { colspan: 1, rowspan: 1, colwidth: null, alignment: 'left', ...attrs },
    Fragment.from(para(text))
  )
}

function makeTable() {
  return schema.nodes.table.create(
    null,
    Fragment.from([
      schema.nodes.table_header_row.create(
        null,
        Fragment.from([th('column1'), th('column2'), th('column3')])
      ),
      schema.nodes.table_row.create(
        null,
        Fragment.from([td('content1'), td('content2'), td('content3')])
      )
    ])
  )
}

function stateWithSelection(table, rect) {
  const doc = schema.nodes.doc.create(null, Fragment.from(table))
  let state = EditorState.create({ schema, doc })
  const map = TableMap.get(table)
  const anchor = 1 + map.positionAt(rect.top, rect.left, table)
  const head = 1 + map.positionAt(rect.bottom - 1, rect.right - 1, table)
  state = state.apply(
    state.tr.setSelection(CellSelection.create(state.doc, anchor, head))
  )
  return state
}

describe('table-cell-merge', () => {
  it('canMerge is true for a rectangular multi-cell selection', () => {
    const state = stateWithSelection(makeTable(), {
      top: 0,
      left: 0,
      bottom: 1,
      right: 2
    })
    expect(isMergeToolbarSelection(state.selection)).toBe(true)
    expect(canMergeCells(state)).toBe(true)
    expect(getMergeToolbarState(state).canMerge).toBe(true)
  })

  it('merges header col1+col2 without lifting text out of the table', () => {
    let state = stateWithSelection(makeTable(), {
      top: 0,
      left: 0,
      bottom: 1,
      right: 2
    })
    expect(
      runMergeCells(state, (tr) => {
        state = state.apply(tr)
      })
    ).toBe(true)

    // Must remain a single table — stock mergeCells splits when cellContent is `paragraph`
    expect(state.doc.childCount).toBe(1)
    expect(state.doc.firstChild.type.name).toBe('table')

    const table = state.doc.firstChild
    const map = TableMap.get(table)
    expect(map.width).toBe(3)
    expect(map.height).toBe(2)
    expect(map.problems).toBeNull()

    const header = table.child(0)
    expect(header.childCount).toBe(2)
    expect(header.child(0).attrs.colspan).toBe(2)
    expect(header.child(0).textContent).toBe('column1 column2')
    expect(header.child(1).textContent).toBe('column3')

    const body = table.child(1)
    expect(body.childCount).toBe(3)
    expect(body.child(0).textContent).toBe('content1')
    expect(body.child(1).textContent).toBe('content2')
    expect(body.child(2).textContent).toBe('content3')
  })

  it('splitCell restores colspan to 1', () => {
    let state = stateWithSelection(makeTable(), {
      top: 0,
      left: 0,
      bottom: 1,
      right: 2
    })
    runMergeCells(state, (tr) => {
      state = state.apply(tr)
    })
    expect(
      runSplitCell(state, (tr) => {
        state = state.apply(tr)
      })
    ).toBe(true)
    expect(state.doc.childCount).toBe(1)
    const firstCell = state.doc.firstChild.child(0).child(0)
    expect(firstCell.attrs.colspan).toBe(1)
  })

  it('shows toolbar and merges two body cells vertically (incl. full-column select)', () => {
    const table = schema.nodes.table.create(
      null,
      Fragment.from([
        schema.nodes.table_header_row.create(
          null,
          Fragment.from([th('h1'), th('h2')])
        ),
        schema.nodes.table_row.create(
          null,
          Fragment.from([td('a'), td('b')])
        ),
        schema.nodes.table_row.create(
          null,
          Fragment.from([td('c'), td('d')])
        )
      ])
    )
    // Two body rows in col0 — not a full column (header excluded)
    let state = stateWithSelection(table, { top: 1, left: 0, bottom: 3, right: 1 })
    expect(isMergeToolbarSelection(state.selection)).toBe(true)
    expect(canMergeCells(state)).toBe(true)
    expect(getMergeToolbarState(state).canMerge).toBe(true)

    expect(
      runMergeCells(state, (tr) => {
        state = state.apply(tr)
      })
    ).toBe(true)
    expect(state.doc.childCount).toBe(1)
    const body1 = state.doc.firstChild.child(1)
    expect(body1.child(0).attrs.rowspan).toBe(2)
    expect(body1.child(0).textContent).toBe('a c')
    expect(state.doc.firstChild.child(2).childCount).toBe(1)
  })

  it('shows merge toolbar for full-column selection (2-row table vertical)', () => {
    const table = schema.nodes.table.create(
      null,
      Fragment.from([
        schema.nodes.table_header_row.create(
          null,
          Fragment.from([th('h1'), th('h2')])
        ),
        schema.nodes.table_row.create(
          null,
          Fragment.from([td('a'), td('b')])
        )
      ])
    )
    const state = stateWithSelection(table, { top: 0, left: 0, bottom: 2, right: 1 })
    expect(state.selection.isColSelection()).toBe(true)
    expect(isMergeToolbarSelection(state.selection)).toBe(true)
    expect(getMergeToolbarState(state).canMerge).toBe(true)
  })
})
